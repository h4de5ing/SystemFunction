package com.android.systemlib

import android.app.timezone.Callback
import android.app.timezone.RulesManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

/**
 * 时区数据更新包（distro.zip）安装器，仅支持 Android 8.1 / 9 / 10（API 27 / 28 / 29）。
 *
 * - RulesManagerService 在运行时（需要 framework 配置 config_enableUpdateableTimeZoneRules=true）：
 *   走 RulesManager.requestInstall()，需要 UPDATE_TIME_ZONE_RULES 权限。
 * - 仅 Android 8.1：服务未开启时退回 UPDATE_TZDATA 广播 -> TzDataInstallReceiver，
 *   通过 [TzDistroProvider] 把文件交给 system_server 读取，需要 UPDATE_CONFIG 权限。
 *   Android 9 起系统已删除该接收器，只能走 RulesManager。
 *
 * 两种方式都只是写入 /data/misc/zoneinfo/staged，重启后由 tzdatacheck 移到 current 生效。
 * 不同系统版本要求的包格式不同（8.1 = 1.x，9 = 2.x，10 = 3.x），不能混用。
 */
class TzDataInstaller(context: Context) {
    private val context = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val isRunning = AtomicBoolean(false)

    data class Callbacks(
        val onStatus: (String) -> Unit = {},
        val onError: (String) -> Unit = {},
        val onSuccess: (String) -> Unit = {},
    )

    /** distro_version / tz_version 内容，形如 "003.001|2026e|001" */
    data class DistroVersion(
        val raw: String,
        val formatMajor: Int,
        val formatMinor: Int,
        val rulesYear: Int,
        val rulesLetter: Char,
        val revision: Int,
    ) {
        /** IANA 规则版本，如 2026e */
        val rules: String get() = "$rulesYear$rulesLetter"

        /** Android 8.1 广播方式使用的 VERSION：2026e r1 -> 202605001，单调递增 */
        val configUpdateVersion: Int
            get() = (rulesYear * 100 + (rulesLetter - 'a' + 1)) * 1000 + revision

        /** 先比较 IANA 规则版本，再比较修订号 */
        fun compareRules(other: DistroVersion): Int = compareValuesBy(
            this, other, { it.rulesYear }, { it.rulesLetter }, { it.revision }
        )

        override fun toString() = raw

        companion object {
            private val PATTERN =
                Regex("""(\d{3})\.(\d{3})\|(\d{4})([a-z])\|(\d{3}).*""", RegexOption.DOT_MATCHES_ALL)

            fun parse(text: String?): DistroVersion? {
                val s = text?.trim() ?: return null
                val m = PATTERN.matchEntire(s) ?: return null
                val g = m.groupValues
                return DistroVersion(
                    s, g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4][0], g[5].toInt()
                )
            }
        }
    }

    data class Status(
        val sdkInt: Int,
        val supported: Boolean,
        /** 本机接受的包格式 (major, minor)，不支持的系统为 null */
        val requiredFormat: Pair<Int, Int>?,
        /** 系统内置的规则版本，如 2020a；读取失败为 null */
        val baseRules: String?,
        /** /data/misc/zoneinfo/current 中已生效的包 */
        val installed: DistroVersion?,
        /** /data/misc/zoneinfo/staged 中待重启生效的包 */
        val staged: DistroVersion?,
        val icuTzVersion: String?,
        val rulesManagerAvailable: Boolean,
        val rulesState: String?,
    ) {
        override fun toString() = buildString {
            append("Android ").append(Build.VERSION.RELEASE).append(" (API ").append(sdkInt).append(")\n")
            append("Supported: ").append(supported).append('\n')
            append("Accepted distro format: ")
                .append(requiredFormat?.let { "%03d.%03d".format(Locale.ROOT, it.first, it.second) } ?: "-")
                .append('\n')
            append("Base (system) rules: ").append(baseRules ?: "?").append('\n')
            append("Installed: ").append(installed ?: "none").append('\n')
            append("Staged (applies after reboot): ").append(staged ?: "none").append('\n')
            append("ICU tz version in use: ").append(icuTzVersion ?: "?").append('\n')
            append("RulesManagerService: ").append(if (rulesManagerAvailable) "available" else "NOT running").append('\n')
            if (rulesState != null) append("RulesState: ").append(rulesState).append('\n')
        }
    }

    fun isSupported(): Boolean = Build.VERSION.SDK_INT in SUPPORTED_SDKS

    /** 本机接受的包格式，major 必须完全一致，minor 不能低于要求 */
    fun requiredFormat(): Pair<Int, Int>? = when (Build.VERSION.SDK_INT) {
        27 -> 1 to 1
        28 -> 2 to 1
        29 -> 3 to 1
        else -> null
    }

    /** APK 内置包的文件名前缀：assets/<prefix>.zip 或 assets/<prefix>_*.zip */
    fun assetPrefix(): String? = when (Build.VERSION.SDK_INT) {
        27 -> "distro_android8"
        28 -> "distro_android9"
        29 -> "distro_android10"
        else -> null
    }

    fun installedVersion(): DistroVersion? =
        DistroVersion.parse(readSmallFile("$ZONEINFO_DIR/current/distro_version"))

    fun stagedVersion(): DistroVersion? =
        DistroVersion.parse(readSmallFile("$ZONEINFO_DIR/staged/distro_version"))

    /** 系统内置（只读分区 / APEX）数据的规则版本，如 2020a */
    fun baseRulesVersion(): String? {
        if (Build.VERSION.SDK_INT == 29) {
            DistroVersion.parse(readSmallFile("$TZDATA_APEX_DIR/tz_version"))?.let { return it.rules }
            return tzdataHeaderVersion("$TZDATA_APEX_DIR/tzdata")
        }
        return tzdataHeaderVersion("/system/usr/share/zoneinfo/tzdata")
    }

    fun isRulesManagerAvailable(): Boolean =
        runCatching { ServiceManager.getService(TIME_ZONE_RULES_MANAGER_SERVICE) != null }.getOrDefault(false)

    fun getStatus(): Status {
        val rm = isSupported() && isRulesManagerAvailable()
        return Status(
            sdkInt = Build.VERSION.SDK_INT,
            supported = isSupported(),
            requiredFormat = requiredFormat(),
            baseRules = baseRulesVersion(),
            installed = installedVersion(),
            staged = stagedVersion(),
            icuTzVersion = runCatching { android.icu.util.TimeZone.getTZDataVersion() }.getOrNull(),
            rulesManagerAvailable = rm,
            rulesState = if (rm) runCatching { rulesManager().rulesState.toString() }.getOrNull() else null,
        )
    }

    fun readDistroVersion(zip: File): DistroVersion? =
        FileInputStream(zip).use { readDistroVersion(it) }

    /** 检查包是否适合本机，返回 null 表示通过，否则返回原因 */
    fun precheck(dv: DistroVersion?, force: Boolean = false): String? {
        if (dv == null) return "distro_version missing/invalid"
        val fmt = requiredFormat()
            ?: return "this Android version is not supported (API ${Build.VERSION.SDK_INT}), only 8.1 / 9 / 10"
        if (dv.formatMajor != fmt.first || dv.formatMinor < fmt.second) {
            return "distro format %03d.%03d does not fit this device (needs %03d.%03d) - wrong package for this Android version"
                .format(Locale.ROOT, dv.formatMajor, dv.formatMinor, fmt.first, fmt.second)
        }
        val base = baseRulesVersion()
        if (base != null && base.length == 5 && dv.rules < base) {
            return "rules ${dv.rules} are older than the system's $base"
        }
        if (!force) {
            val newest = listOfNotNull(installedVersion(), stagedVersion()).maxWithOrNull { a, b -> a.compareRules(b) }
            if (newest != null && dv.compareRules(newest) <= 0) {
                return "already installed/staged: $newest (use force to reinstall)"
            }
        }
        return null
    }

    /**
     * 异步安装 distro.zip，结果通过 [callbacks] 回调（在后台线程中调用）。
     *
     * @param force 重新安装相同或更旧的版本（仍然不能低于系统内置版本）
     * @param configUpdateVersion 仅 8.1 广播方式：指定 VERSION，默认由版本号推出
     */
    fun install(
        distroFile: File,
        force: Boolean = false,
        configUpdateVersion: Int? = null,
        callbacks: Callbacks,
    ) {
        if (!isRunning.compareAndSet(false, true)) {
            callbacks.onError("Time zone operation is already running")
            return
        }
        executor.execute {
            try {
                if (!isSupported()) {
                    callbacks.onError("This Android version is not supported (API ${Build.VERSION.SDK_INT}), only 8.1 / 9 / 10")
                    return@execute
                }
                val dv = try {
                    readDistroVersion(distroFile)
                } catch (e: IOException) {
                    callbacks.onError("Cannot read $distroFile: $e")
                    return@execute
                }
                callbacks.onStatus("Package: ${distroFile.name}  distro_version=$dv")
                precheck(dv, force)?.let {
                    callbacks.onError(it)
                    return@execute
                }
                when {
                    isRulesManagerAvailable() -> installWithRulesManager(distroFile, callbacks)
                    Build.VERSION.SDK_INT == 27 ->
                        installWithConfigUpdate(distroFile, dv!!, configUpdateVersion, callbacks)

                    else -> callbacks.onError(
                        "RulesManagerService is not running on this device. It is only started when " +
                                "framework config_enableUpdateableTimeZoneRules is true."
                    )
                }
            } catch (e: SecurityException) {
                callbacks.onError(
                    "SecurityException: ${e.message}\nThe app must hold android.permission.UPDATE_TIME_ZONE_RULES " +
                            "(and UPDATE_CONFIG on 8.1), i.e. be signed with the platform key."
                )
            } catch (e: Exception) {
                callbacks.onError(e.message ?: e.toString())
            } finally {
                isRunning.set(false)
            }
        }
    }

    /**
     * 卸载已安装/待生效的包，重启后恢复系统内置数据。只能通过 RulesManager 完成
     * （8.1 广播方式没有卸载接口）。
     */
    fun uninstall(callbacks: Callbacks) {
        if (!isRunning.compareAndSet(false, true)) {
            callbacks.onError("Time zone operation is already running")
            return
        }
        executor.execute {
            try {
                if (!isSupported() || !isRulesManagerAvailable()) {
                    callbacks.onError("Uninstall needs RulesManagerService (config_enableUpdateableTimeZoneRules=true)")
                    return@execute
                }
                callbacks.onStatus("Using RulesManager.requestUninstall()")
                val result = awaitRulesManager { rulesManager().requestUninstall(null, it) }
                if (result == Callback.SUCCESS) {
                    callbacks.onSuccess("Uninstall staged. Reboot to restore the system time zone data.")
                } else {
                    callbacks.onError("Uninstall failed: ${callbackStatus(result)}")
                }
            } catch (e: Exception) {
                callbacks.onError(e.message ?: e.toString())
            } finally {
                isRunning.set(false)
            }
        }
    }

    /**
     * 把 APK 内置的、适合本机的最新包（assets/<prefix>.zip 或 <prefix>_*.zip）拷贝到私有目录并返回，
     * 没有则返回 null。有多个时按 zip 内的 distro_version 取最新的一个。
     */
    fun extractBundledDistro(): File? {
        val prefix = assetPrefix() ?: return null
        val names = runCatching { context.assets.list("") }.getOrNull() ?: return null
        val best = names
            .filter { it.endsWith(".zip") && (it == "$prefix.zip" || it.startsWith("${prefix}_")) }
            .mapNotNull { name ->
                runCatching { context.assets.open(name).use { readDistroVersion(it) } }.getOrNull()
                    ?.let { name to it }
            }
            .maxWithOrNull { a, b -> a.second.compareRules(b.second) }
            ?: return null
        val out = File(workDir(), best.first)
        context.assets.open(best.first).use { copyStream(it, out) }
        return out
    }

    private fun installWithRulesManager(distroFile: File, callbacks: Callbacks) {
        callbacks.onStatus("Using RulesManager.requestInstall()")
        val result = ParcelFileDescriptor.open(distroFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            // RulesManagerService 在 binder 调用期间会 dup 这个描述符，调用返回后即可关闭
            awaitRulesManager { rulesManager().requestInstall(pfd, null, it) }
        }
        if (result == Callback.SUCCESS) {
            callbacks.onSuccess("Staged OK. Reboot to activate.")
        } else {
            callbacks.onError("Install failed: ${callbackStatus(result)}")
        }
    }

    /** 调用 RulesManager 并等待 Callback 结果；返回 Callback 状态码 */
    private fun awaitRulesManager(request: (Callback) -> Int): Int {
        val done = CountDownLatch(1)
        var status = Callback.ERROR_UNKNOWN_FAILURE
        val rc = request(object : Callback() {
            override fun onFinished(s: Int) {
                status = s
                done.countDown()
            }
        })
        if (rc != RulesManager.SUCCESS) {
            throw IOException(
                if (rc == RulesManager.ERROR_OPERATION_IN_PROGRESS) "Another time zone operation is in progress"
                else "RulesManager request failed: $rc"
            )
        }
        // 回调投递到主线程，这里在后台线程等待，不会死锁
        if (!done.await(RULES_MANAGER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw IOException("No result from RulesManagerService after $RULES_MANAGER_TIMEOUT_SECONDS s")
        }
        return status
    }

    private fun installWithConfigUpdate(
        distroFile: File,
        dv: DistroVersion,
        versionOverride: Int?,
        callbacks: Callbacks,
    ) {
        callbacks.onStatus("Using UPDATE_TZDATA broadcast -> TzDataInstallReceiver")
        copyStream(FileInputStream(distroFile), TzDistroProvider.file(context))
        val uri: Uri = TzDistroProvider.uri(context)
        context.grantUriPermission("android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val version = versionOverride ?: dv.configUpdateVersion
        val intent = Intent(ACTION_UPDATE_TZDATA).apply {
            component = TZDATA_RECEIVER
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(EXTRA_VERSION_NUMBER, version.toString())
            putExtra(EXTRA_REQUIRED_HASH, "NONE")
        }
        context.sendBroadcast(intent)
        callbacks.onStatus(
            "Broadcast sent (VERSION=$version). system_server installs it in the background; " +
                    "the receiver ignores a VERSION <= the last one it installed."
        )
        // 接收器没有返回结果，轮询 staged 目录
        repeat(CONFIG_UPDATE_POLL_SECONDS) {
            Thread.sleep(1000)
            val staged = stagedVersion()
            if (staged != null && staged.compareRules(dv) == 0) {
                callbacks.onSuccess("Staged OK ($staged). Reboot to activate.")
                return
            }
        }
        callbacks.onError(
            "No staged distro after $CONFIG_UPDATE_POLL_SECONDS s. Check `adb logcat -s TZDataInstallReceiver " +
                    "ConfigUpdateInstallReceiver`. If it says 'new version is <= current version', pass a larger VERSION."
        )
    }

    private fun rulesManager(): RulesManager =
        context.getSystemService(TIME_ZONE_RULES_MANAGER_SERVICE) as RulesManager

    private fun workDir(): File = File(context.filesDir, TzDistroProvider.DIR).apply { mkdirs() }

    private fun readDistroVersion(input: InputStream): DistroVersion? {
        ZipInputStream(input).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: return null
                if (entry.name == "distro_version") {
                    return DistroVersion.parse(String(readAll(zis, 1024), Charsets.US_ASCII))
                }
            }
        }
    }

    private fun tzdataHeaderVersion(path: String): String? {
        // tzdata 文件头："tzdata2026e\0"
        val hdr = readSmallFile(path, 12) ?: return null
        return if (hdr.startsWith("tzdata") && hdr.length >= 11) hdr.substring(6, 11) else null
    }

    private fun readSmallFile(path: String, max: Int = 256): String? {
        val f = File(path)
        if (!f.canRead()) return null
        return runCatching {
            FileInputStream(f).use { input ->
                val buf = ByteArray(max)
                val n = input.read(buf)
                if (n <= 0) null else String(buf, 0, n, Charsets.US_ASCII).trim('\u0000', ' ', '\n')
            }
        }.getOrNull()
    }

    private fun readAll(input: InputStream, max: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            bos.write(buf, 0, n)
            if (bos.size() > max) throw IOException("distro_version too large")
        }
        return bos.toByteArray()
    }

    private fun copyStream(input: InputStream, dst: File) {
        dst.parentFile?.mkdirs()
        val tmp = File(dst.path + ".tmp")
        input.use { src -> tmp.outputStream().use { src.copyTo(it) } }
        if (!tmp.renameTo(dst)) throw IOException("rename $tmp -> $dst")
    }

    private fun callbackStatus(s: Int): String = when (s) {
        Callback.SUCCESS -> "SUCCESS"
        Callback.ERROR_UNKNOWN_FAILURE -> "ERROR_UNKNOWN_FAILURE (see logcat timezone.RulesManagerService)"
        Callback.ERROR_INSTALL_BAD_DISTRO_STRUCTURE -> "ERROR_INSTALL_BAD_DISTRO_STRUCTURE"
        Callback.ERROR_INSTALL_BAD_DISTRO_FORMAT_VERSION -> "ERROR_INSTALL_BAD_DISTRO_FORMAT_VERSION (wrong Android target)"
        Callback.ERROR_INSTALL_RULES_TOO_OLD -> "ERROR_INSTALL_RULES_TOO_OLD (older than system)"
        Callback.ERROR_INSTALL_VALIDATION_ERROR -> "ERROR_INSTALL_VALIDATION_ERROR (tzdata/tzlookup rejected)"
        else -> "status $s"
    }

    companion object {
        /** 8.1 / 9 / 10 */
        val SUPPORTED_SDKS = setOf(27, 28, 29)

        const val ZONEINFO_DIR = "/data/misc/zoneinfo"
        private const val TZDATA_APEX_DIR = "/apex/com.android.tzdata/etc/tz"

        /** Context.TIME_ZONE_RULES_MANAGER_SERVICE（@hide） */
        private const val TIME_ZONE_RULES_MANAGER_SERVICE = "timezone"

        // Android 8.1 的 TzDataInstallReceiver（Android 9 起已删除）
        private const val ACTION_UPDATE_TZDATA = "android.intent.action.UPDATE_TZDATA"
        private val TZDATA_RECEIVER =
            ComponentName("android", "com.android.server.updates.TzDataInstallReceiver")
        private const val EXTRA_VERSION_NUMBER = "VERSION"
        private const val EXTRA_REQUIRED_HASH = "REQUIRED_HASH"

        private const val RULES_MANAGER_TIMEOUT_SECONDS = 120L
        private const val CONFIG_UPDATE_POLL_SECONDS = 30
    }
}
