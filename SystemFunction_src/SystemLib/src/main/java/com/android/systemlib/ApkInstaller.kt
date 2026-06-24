package com.android.systemlib

import android.content.Context
import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import android.os.ServiceManager
import android.os.Bundle
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * ApkInstaller —— system 权限静默安装器（不依赖 root / Shizuku 等第三方特权方式）。
 *
 * 设计来源：分析 InstallerX-Revived 与 universal-installer 两个开源安装器后提炼，
 * 详见 `app/docs/ApkInstaller-设计文档.md`。
 *
 * 静默性来源：本应用以 system 签名 / system UID 运行，`PackageInstaller` 调用本身即特权身份，
 * 系统不会注入 [PackageInstaller.STATUS_PENDING_USER_ACTION]；提交用进程内 [IIntentSender]
 * 同步回传结果，不拉 Activity、不发广播；并以隐藏 [IPackageInstaller.setPermissionsResult]
 * 作为程序化批准兜底。全程不调用 `su` / `pm` / Shizuku / `app_process`。
 *
 * 支持格式：`.apk` / `.apks` / `.xapk` / `.apkm` / 多 APK ZIP。
 */
object ApkInstaller {

    /** 安装结果。 */
    sealed class Result {
        data class Success(val packageName: String, val message: String = "install success") : Result()
        data class Failure(
            val status: Int,
            val legacyStatus: Int = -1,
            val message: String = "",
            val packageName: String = ""
        ) : Result()

        /** 自动批准失败后，仍需用户确认的兜底（system 身份下几乎不会出现）。 */
        data class PendingUserAction(val confirmIntent: Intent) : Result()
    }

    /** split 类型。 */
    private enum class SplitType { BASE, ARCHITECTURE, DENSITY, LANGUAGE, FEATURE, OTHER, DM }

    /** 一个待写入会话的 split 条目。 */
    private class SplitEntry(
        val name: String,            // 写入会话用的文件名（如 base.apk / split_config.arm64_v8a.apk）
        val type: SplitType,
        val abi: String? = null,
        val density: Int? = null,
        val localeTag: String? = null,
        val isBase: Boolean = false,
        val size: Long = -1L,
        val openStream: () -> InputStream
    )

    /** 已知 ABI -> 优先级（数值越小越优先，用于在设备 SUPPORTED_ABIS 里匹配）。 */
    private val ABI_TOKENS = listOf(
        "arm64-v8a", "armeabi-v7a", "armeabi", "x86_64", "x86", "mips64", "mips"
    )

    /** 已知 density token -> dpi。 */
    private val DENSITY_TOKENS = mapOf(
        "ldpi" to 120, "mdpi" to 160, "hdpi" to 240, "xhdpi" to 320,
        "xxhdpi" to 480, "xxxhdpi" to 640, "tvdpi" to 213, "nodpi" to 0, "anydpi" to 0
    )

    private val VERSION_CODE_HIGHEST = -1L

    /** 隐藏常量 PackageInstaller.EXTRA_LEGACY_STATUS（精确 legacy 错误码），反射读取。 */
    private val EXTRA_LEGACY_STATUS: Int by lazy {
        try {
            PackageInstaller::class.java.getDeclaredField("EXTRA_LEGACY_STATUS").getInt(null)
        } catch (_: Throwable) {
            -1
        }
    }

    /** 隐藏常量 PackageManager.INSTALL_REPLACE_EXISTING。 */
    private val INSTALL_REPLACE_EXISTING: Int by lazy {
        try {
            PackageManager::class.java.getDeclaredField("INSTALL_REPLACE_EXISTING").getInt(null)
        } catch (_: Throwable) {
            0x00000002
        }
    }

    /**
     * 安装任意支持的格式文件。system 静默优先。
     *
     * @param context  上下文
     * @param filePath 待安装文件路径（apk/apks/xapk/apkm/多 APK zip）
     * @param userId   目标用户 id（system 身份可跨用户），默认当前用户
     * @param installerPackageName 安装来源归属包名（Settings > Apps 里显示的“安装来源”），null=本应用包名
     * @param onProgress 进度回调（0..100）
     */
    fun install(
        context: Context,
        filePath: String,
        userId: Int = 0,
        installerPackageName: String? = null,
        onProgress: ((percent: Int, message: String) -> Unit)? = null
    ): Result {
        val file = File(filePath)
        if (!file.exists()) {
            return Result.Failure(status = -999, message = "file not found: $filePath")
        }
        return try {
            val format = detectFormat(file)
            onProgress?.invoke(2, "detected format: $format")
            // 多包 ZIP：每个独立 APK 各自建会话。
            if (format == Format.MULTI_APK_ZIP) {
                return installMultiApkZip(context, file, userId, installerPackageName, onProgress)
            }
            val splits = parseSplits(file, format)
            if (splits.isEmpty()) {
                return Result.Failure(status = -5, message = "no apk entries found in $format")
            }
            val selected = selectOptimal(context, splits)
            onProgress?.invoke(5, "splits selected: ${selected.size}/${splits.size}")
            installSplits(context, selected, userId, installerPackageName, onProgress)
        } catch (e: Throwable) {
            Result.Failure(status = -1, message = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** 卸载（system 静默）。 */
    fun uninstall(
        context: Context,
        packageName: String,
        userId: Int = 0
    ): Result {
        return try {
            val iPackageInstaller = getIPackageInstaller() ?: return Result.Failure(
                status = -7, message = "cannot reach IPackageInstaller"
            )
            val receiver = LocalIntentSender()
            iPackageInstaller.uninstall(
                VersionedPackage(packageName, VERSION_CODE_HIGHEST),
                installerPackageName(context),
                0,
                receiver.intentSender(),
                userId
            )
            receiver.toResult(packageName)
        } catch (e: Throwable) {
            Result.Failure(status = -1, message = "uninstall error: ${e.message}", packageName = packageName)
        }
    }

    // ------------------------------------------------------------------
    // 格式检测与解析
    // ------------------------------------------------------------------

    private enum class Format { APK, APKS, XAPK, APKM, MULTI_APK_ZIP }

    private fun detectFormat(file: File): Format {
        val name = file.name.lowercase(Locale.ROOT)
        if (name.endsWith(".apk")) return Format.APK
        // 其余视为 ZIP，按内容判定
        return try {
            ZipFile(file).use { zip ->
                val entries = Collections.list(zip.entries()).map { it.name.lowercase() }
                val has = { sub: String -> entries.any { it.contains(sub) } }
                when {
                    has("manifest.json") && (has("split_apks") || has("expansions")) -> Format.XAPK
                    has("info.json") && has("info.json") -> Format.APKM
                    has("androidmanifest.xml") -> Format.APK
                    has("toc.pb") || has("base.apk") || entries.any { it.startsWith("base-master") } -> Format.APKS
                    else -> Format.MULTI_APK_ZIP
                }
            }
        } catch (_: Throwable) {
            // 非 zip 或损坏：当单 APK 兜底
            Format.APK
        }
    }

    private fun parseSplits(file: File, format: Format): List<SplitEntry> {
        return when (format) {
            Format.APK -> listOf(
                SplitEntry(
                    name = "base.apk",
                    type = SplitType.BASE,
                    isBase = true,
                    size = file.length(),
                    openStream = { file.inputStream() }
                )
            )
            Format.APKS, Format.MULTI_APK_ZIP -> parseZipApks(file, null)
            Format.XAPK -> {
                val manifest = readZipEntryText(file, "manifest.json")
                val json = manifest?.let { runCatching { JSONObject(it) }.getOrNull() }
                val splitIds = mutableListOf<String>()
                if (json != null && json.has("split_apks")) {
                    val arr = json.getJSONArray("split_apks")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        splitIds.add(o.optString("file", o.optString("id")))
                    }
                }
                parseZipApks(file, splitIds.ifEmpty { null })
            }
            Format.APKM -> {
                val info = readZipEntryText(file, "info.json")
                val json = info?.let { runCatching { JSONObject(it) }.getOrNull() }
                val names = mutableListOf<String>()
                if (json != null && json.has("split_apks")) {
                    val arr = json.getJSONArray("split_apks")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        names.add(o.optString("file", o.optString("name")))
                    }
                }
                parseZipApks(file, names.ifEmpty { null })
            }
        }
    }

    /** 从 ZIP 里取所有 .apk（[filter] 非空时只取指定文件名，按 manifest 顺序）。 */
    private fun parseZipApks(file: File, filter: List<String>?): List<SplitEntry> {
        val result = mutableListOf<SplitEntry>()
        ZipFile(file).use { zip ->
            val entries = if (filter != null) {
                // 按 manifest 给定顺序解析；同时附带同名 .dm
                filter.flatMap { listOf(it, it.replace(".apk", ".dm")) }
                    .mapNotNull { name -> zip.getEntry(name) }
            } else {
                Collections.list(zip.entries()).filter { !it.isDirectory && it.name.lowercase().endsWith(".apk") } +
                    Collections.list(zip.entries()).filter { !it.isDirectory && it.name.lowercase().endsWith(".dm") }
            }
            for (entry in entries) {
                val lower = entry.name.lowercase(Locale.ROOT)
                val isDm = lower.endsWith(".dm")
                val type = if (isDm) SplitType.DM else classifySplit(entry.name)
                val writeName = if (isDm) File(entry.name).name else File(entry.name).name
                result.add(
                    SplitEntry(
                        name = writeName,
                        type = type,
                        abi = if (type == SplitType.ARCHITECTURE) extractAbi(entry.name) else null,
                        density = if (type == SplitType.DENSITY) extractDensity(entry.name) else null,
                        localeTag = if (type == SplitType.LANGUAGE) extractLocale(entry.name) else null,
                        isBase = type == SplitType.BASE,
                        size = entry.size,
                        openStream = { zip.getInputStream(entry) }
                    )
                )
            }
        }
        return result
    }

    private fun classifySplit(name: String): SplitType {
        val n = File(name).nameWithoutExtension.lowercase(Locale.ROOT)
        if (n == "base" || n == "base-master" || n.startsWith("base-master")) return SplitType.BASE
        val stripped = n
            .removePrefix("split_config.")
            .removePrefix("config.")
            .removePrefix("split-")
            .removePrefix("base-")
        return when {
            ABI_TOKENS.any { stripped.replace("_", "-") == it || stripped == it } -> SplitType.ARCHITECTURE
            DENSITY_TOKENS.keys.any { stripped == it } || Regex("\\d+dpi").matches(stripped) -> SplitType.DENSITY
            isLocaleTag(stripped) -> SplitType.LANGUAGE
            stripped.startsWith("feature") -> SplitType.FEATURE
            else -> SplitType.OTHER
        }
    }

    private fun extractAbi(name: String): String =
        File(name).nameWithoutExtension.lowercase(Locale.ROOT)
            .removePrefix("split_config.").removePrefix("config.").removePrefix("split-")
            .let { if (it.startsWith("arm64_v8a")) "arm64-v8a" else it.replace("_", "-") }

    private fun extractDensity(name: String): Int {
        val t = File(name).nameWithoutExtension.lowercase(Locale.ROOT)
            .removePrefix("split_config.").removePrefix("config.").removePrefix("split-")
        DENSITY_TOKENS[t]?.let { return it }
        Regex("(\\d+)dpi").find(t)?.let { return it.groupValues[1].toInt() }
        return 0
    }

    private fun extractLocale(name: String): String =
        File(name).nameWithoutExtension.lowercase(Locale.ROOT)
            .removePrefix("split_config.").removePrefix("config.").removePrefix("split-")

    private fun isLocaleTag(s: String): Boolean =
        s.length in 2..8 && !s.contains('.') && !s.contains('_') &&
            runCatching { Locale.forLanguageTag(s); true }.getOrDefault(false)

    private fun readZipEntryText(file: File, entryName: String): String? = try {
        ZipFile(file).use { zip ->
            zip.getEntry(entryName)?.let { zip.getInputStream(it).bufferedReader().use { it.readText() } }
        }
    } catch (_: Throwable) {
        null
    }

    // ------------------------------------------------------------------
    // 智能选择 split
    // ------------------------------------------------------------------

    /** 按设备 ABI/density/locale 选最优 split；base/feature/other/dm 始终保留。 */
    private fun selectOptimal(context: Context, splits: List<SplitEntry>): List<SplitEntry> {
        if (splits.none { it.type == SplitType.BASE } && splits.size == 1) return splits
        val keep = mutableListOf<SplitEntry>()
        keep += splits.filter { it.type in listOf(SplitType.BASE, SplitType.FEATURE, SplitType.OTHER, SplitType.DM) }

        // ABI：取设备 SUPPORTED_ABIS 里第一个命中的
        val deviceAbis = Build.SUPPORTED_ABIS.toList()
        val archSplits = splits.filter { it.type == SplitType.ARCHITECTURE }
        if (archSplits.isNotEmpty()) {
            for (devAbi in deviceAbis) {
                val hit = archSplits.firstOrNull { it.abi.equals(devAbi, ignoreCase = true) }
                if (hit != null) { keep += hit; break }
            }
            // 没命中则全留（交给系统判断 / 触发二进制翻译）
            if (keep.none { it.type == SplitType.ARCHITECTURE }) keep += archSplits
        }

        // density：取与设备最接近的
        val dpiSplits = splits.filter { it.type == SplitType.DENSITY && it.density != null && it.density > 0 }
        if (dpiSplits.isNotEmpty()) {
            val devDpi = context.resources.displayMetrics.densityDpi
            keep += dpiSplits.minByOrNull { kotlin.math.abs((it.density ?: 0) - devDpi) }!!
        } else {
            keep += splits.filter { it.type == SplitType.DENSITY && it.density == 0 } // nodpi/anydpi
        }

        // locale：取设备默认语言匹配，始终保留 en
        val devLocales = androidx.core.os.LocaleListCompat.getDefault()
        val localeSplits = splits.filter { it.type == SplitType.LANGUAGE }
        if (localeSplits.isNotEmpty()) {
            var picked = false
            for (i in 0 until devLocales.size()) {
                val tag = devLocales[i]?.toLanguageTag()?.lowercase(Locale.ROOT)
                val hit = localeSplits.firstOrNull { it.localeTag?.equals(tag, ignoreCase = true) == true }
                if (hit != null) { keep += hit; picked = true; break }
            }
            if (!picked) {
                keep += localeSplits.firstOrNull { it.localeTag?.startsWith("en") == true } ?: localeSplits.first()
            }
        }
        // 去重（按 name）
        return keep.distinctBy { it.name }
    }

    // ------------------------------------------------------------------
    // 会话构建与写入
    // ------------------------------------------------------------------

    private fun installSplits(
        context: Context,
        splits: List<SplitEntry>,
        userId: Int,
        installerPackageName: String?,
        onProgress: ((Int, String) -> Unit)?
    ): Result {
        val hasBase = splits.any { it.isBase }
        val params = SessionParams(
            if (hasBase) SessionParams.MODE_FULL_INSTALL else SessionParams.MODE_INHERIT_EXISTING
        )
        // 隐藏字段（反射）：组合 installFlags（强制替换已存在）。
        runCatching {
            val f = SessionParams::class.java.getDeclaredField("installFlags")
            f.isAccessible = true
            f.setInt(params, f.getInt(params) or INSTALL_REPLACE_EXISTING)
        }
        // 隐藏字段（反射）：abiOverride 触发二进制翻译（Houdini）。
        runCatching {
            val abi = splits.firstOrNull { it.type == SplitType.ARCHITECTURE }?.abi
            if (abi != null) {
                val f = SessionParams::class.java.getDeclaredField("abiOverride")
                f.isAccessible = true
                f.set(params, abi)
            }
        }
        params.setSize(splits.sumOf { if (it.size > 0) it.size else 0L })
        runCatching { params.setInstallReason(PackageManager.INSTALL_REASON_USER) }
        installerPackageName?.let { runCatching { params.setInstallerPackageName(it) } }

        val packageInstaller = getPrivilegedPackageInstaller(context, userId, installerPackageName)
            ?: context.packageManager.packageInstaller
        val sessionId = packageInstaller.createSession(params)
        onProgress?.invoke(10, "session created: $sessionId")

        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            val totalBytes = splits.sumOf { maxOf(it.size, 0L) }.coerceAtLeast(1L)
            var written = 0L
            for ((idx, split) in splits.withIndex()) {
                session.openWrite(split.name, 0, split.size).use { out ->
                    split.openStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            written += n
                            onProgress?.invoke(
                                (10 + 85 * written / totalBytes).toInt().coerceIn(10, 95),
                                "writing ${split.name}"
                            )
                        }
                    }
                    session.fsync(out)
                }
                onProgress?.invoke(95, "wrote ${idx + 1}/${splits.size}")
            }
            val result = commitAndAwait(context, packageInstaller, sessionId, onProgress)
            return result
        } catch (e: Throwable) {
            runCatching { session?.abandon() }
            return Result.Failure(status = -3, message = "session error: ${e.message}")
        } finally {
            closeQuietly(session)
        }
    }

    private fun installMultiApkZip(
        context: Context,
        file: File,
        userId: Int,
        installerPackageName: String?,
        onProgress: ((Int, String) -> Unit)?
    ): Result {
        // 把每个内嵌 APK 视为独立单包安装，依次进行；任一失败即返回。
        ZipFile(file).use { zip ->
            val apkEntries = Collections.list(zip.entries())
                .filter { !it.isDirectory && it.name.lowercase().endsWith(".apk") }
            if (apkEntries.isEmpty()) return Result.Failure(status = -5, message = "no apk in zip")
            // 解压到缓存逐个安装
            val cacheDir = File(context.cacheDir, "apkinstaller_multi").apply { mkdirs() }
            for ((i, entry) in apkEntries.withIndex()) {
                val tmp = File(cacheDir, "${System.currentTimeMillis()}_${i}_${File(entry.name).name}")
                zip.getInputStream(entry).use { it.copyTo(FileOutputStream(tmp)) }
                val r = install(context, tmp.absolutePath, userId, installerPackageName) { p, m ->
                    onProgress?.invoke((p * (i + 1) / apkEntries.size).toInt(), m)
                }
                tmp.delete()
                if (r is Result.Failure) return r
            }
            cacheDir.delete()
            return Result.Success("", "multi apk installed: ${apkEntries.size}")
        }
    }

    // ------------------------------------------------------------------
    // 提交与结果（核心：本地 IntentSender 同步回传 + 自动批准兜底）
    // ------------------------------------------------------------------

    private fun commitAndAwait(
        context: Context,
        packageInstaller: PackageInstaller,
        sessionId: Int,
        onProgress: ((Int, String) -> Unit)?
    ): Result {
        // 主路径：进程内 IIntentSender 同步回传（吸收 InstallerX LocalIntentReceiver）。
        var session: PackageInstaller.Session? = null
        try {
            session = packageInstaller.openSession(sessionId)
            val receiver = LocalIntentSender()
            session.commit(receiver.intentSender())
            onProgress?.invoke(96, "committing…")
            val intent = receiver.await(AWAIT_TIMEOUT_MS)
            val result = interpretResult(intent)
            // 兜底：若系统仍要求用户确认，尝试程序化批准（system 身份下几乎不会走到）。
            if (result is Result.PendingUserAction) {
                val approved = runCatching { autoApprove(sessionId, true) }.getOrDefault(false)
                if (approved) {
                    val again = receiver.await(AWAIT_TIMEOUT_MS)
                    return interpretResult(again)
                }
            }
            return result
        } catch (e: Throwable) {
            // 本地接收器在极端版本上可能不兼容（IIntentSender.send 签名演进），
            // 回退到 PendingIntent 广播路径（即现有 installAPK 的成熟实现）。
            return commitViaPendingIntent(context, packageInstaller, sessionId, onProgress)
                ?: Result.Failure(status = -3, message = "commit failed: ${e.message}")
        } finally {
            closeQuietly(session)
        }
    }

    /** 回退提交路径：PendingIntent 广播（兼容性最好）。 */
    private fun commitViaPendingIntent(
        context: Context,
        packageInstaller: PackageInstaller,
        sessionId: Int,
        onProgress: ((Int, String) -> Unit)?
    ): Result? {
        var session: PackageInstaller.Session? = null
        return try {
            session = packageInstaller.openSession(sessionId)
            val appContext = context.applicationContext
            val action = "com.android.systemlib.INSTALL_COMPLETE.$sessionId"
            val latch = CountDownLatch(1)
            var resultIntent: Intent? = null
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    resultIntent = intent
                    latch.countDown()
                }
            }
            val filter = IntentFilter(action)
            appContext.registerReceiver(
                receiver, filter,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Context.RECEIVER_NOT_EXPORTED else 0
            )
            val intent = Intent(action).setPackage(appContext.packageName)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getBroadcast(appContext, sessionId, intent, flags)
            session.commit(pi.intentSender)
            latch.await(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            appContext.unregisterReceiver(receiver)
            interpretResult(resultIntent)
        } catch (e: Throwable) {
            null
        } finally {
            closeQuietly(session)
        }
    }

    /** 解析安装结果 Intent。 */
    private fun interpretResult(intent: Intent?): Result {
        if (intent == null) return Result.Failure(status = -8, message = "install timeout, no result intent")
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
        val pkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) ?: ""
        val legacy = if (EXTRA_LEGACY_STATUS != -1)
            intent.getIntExtra(EXTRA_LEGACY_STATUS.toString(), -1) else -1
        return when (status) {
            PackageInstaller.STATUS_SUCCESS -> Result.Success(pkg, "install success: $pkg")
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    Result.PendingUserAction(confirm)
                } else {
                    Result.Failure(status, legacy, "pending user action without intent", pkg)
                }
            }
            else -> Result.Failure(status, legacy, "install failed status=$status, msg=$msg, pkg=$pkg", pkg)
        }
    }

    /** 调用隐藏 IPackageInstaller.setPermissionsResult 程序化批准/拒绝会话。返回是否调用成功。 */
    private fun autoApprove(sessionId: Int, granted: Boolean): Boolean {
        val i = getIPackageInstaller() ?: return false
        return try {
            i.setPermissionsResult(sessionId, granted); true
        } catch (_: Throwable) {
            false
        }
    }

    // ------------------------------------------------------------------
    // 特权 PackageInstaller 获取
    // ------------------------------------------------------------------

    /** 直接经 AIDL 取系统 IPackageInstaller（system 身份下即特权）。 */
    private fun getIPackageInstaller(): IPackageInstaller? = try {
        val iPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        iPm.packageInstaller
    } catch (_: Throwable) {
        null
    }

    /**
     * 通过隐藏构造器构造绑定到指定 [userId] 与 [callerPackageName] 的特权 [PackageInstaller]。
     * 失败返回 null，调用方回退到公开 [PackageManager.getPackageInstaller]。
     */
    private fun getPrivilegedPackageInstaller(
        context: Context,
        userId: Int,
        callerPackageName: String?
    ): PackageInstaller? {
        val iInstaller = getIPackageInstaller() ?: return null
        val caller = callerPackageName ?: installerPackageName(context)
        return try {
            // API 31+ 真实签名为 4 参 (IPackageInstaller, String, String, int)，
            // 旧版为 3 参 (IPackageInstaller, String, int)。反射逐个尝试。
            runCatching {
                PackageInstaller::class.java
                    .getDeclaredConstructor(
                        IPackageInstaller::class.java, String::class.java,
                        String::class.java, Int::class.javaPrimitiveType
                    )
                    .apply { isAccessible = true }
                    .newInstance(iInstaller, caller, null as String?, userId)
            }.getOrElse {
                PackageInstaller::class.java
                    .getDeclaredConstructor(
                        IPackageInstaller::class.java, String::class.java, Int::class.javaPrimitiveType
                    )
                    .apply { isAccessible = true }
                    .newInstance(iInstaller, caller, userId)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun installerPackageName(context: Context): String = context.applicationContext.packageName

    // ------------------------------------------------------------------
    // 进程内 IntentSender（吸收 InstallerX LocalIntentReceiver）
    // ------------------------------------------------------------------

    /**
     * 直接实现隐藏 [IIntentSender.Stub]，用隐藏构造器 [IntentSender](`(IIntentSender)`) 包装，
     * `session.commit(intentSender)` 通过 CountDownLatch 在进程内同步回传结果，
     * 取代 PendingIntent 广播。注：`send` 签名随版本演进，若不兼容会在 [commitAndAwait] 里
     * 被 Throwable 捕获并回退到广播路径。
     */
    private class LocalIntentSender : IIntentSender.Stub() {
        private val latch = CountDownLatch(1)
        @Volatile
        private var result: Intent? = null

        override fun send(
            code: Int,
            intent: Intent?,
            resolvedType: String?,
            resultTo: IBinder?,
            resultWho: IIntentReceiver?,
            requiredPermission: String?,
            options: Bundle?
        ) {
            result = intent
            latch.countDown()
        }

        fun intentSender(): IntentSender = newIntentSender(this)

        private fun newIntentSender(iSender: IIntentSender): IntentSender = try {
            // 隐藏构造器 IntentSender(IIntentSender) 在 SDK 中为 package-private，反射构造。
            IntentSender::class.java
                .getDeclaredConstructor(IIntentSender::class.java)
                .apply { isAccessible = true }
                .newInstance(iSender)
        } catch (_: Throwable) {
            // 回退到 2 参构造器 (IIntentSender, IBinder)。
            IntentSender::class.java
                .getDeclaredConstructor(IIntentSender::class.java, IBinder::class.java)
                .apply { isAccessible = true }
                .newInstance(iSender, null)
        }

        fun await(timeoutMs: Long): Intent? =
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) result else null

        @Throws(RemoteException::class)
        fun toResult(packageName: String): Result {
            val intent = await(AWAIT_TIMEOUT_MS)
            return interpretResultForUninstall(intent, packageName)
        }
    }

    private fun interpretResultForUninstall(intent: Intent?, packageName: String): Result {
        if (intent == null) return Result.Failure(status = -8, message = "uninstall timeout", packageName = packageName)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
        return if (status == PackageInstaller.STATUS_SUCCESS)
            Result.Success(packageName, "uninstall success")
        else Result.Failure(status, message = "uninstall failed status=$status, msg=$msg", packageName = packageName)
    }

    // ------------------------------------------------------------------
    private val AWAIT_TIMEOUT_MS = 5 * 60 * 1000L

    private fun closeQuietly(c: Closeable?) {
        if (c != null) runCatching { c.close() }
    }
}
