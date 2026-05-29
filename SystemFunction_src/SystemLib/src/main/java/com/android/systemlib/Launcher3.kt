package com.android.systemlib

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import androidx.core.content.IntentCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import java.util.UUID

data class LauncherIcon(
    val id: Long,
    val title: String,
    val pkg: String,
    val screen: Int,
    val cellX: Int,
    val cellY: Int
)

/**
 * 注意：ByteArray? 字段导致 data class 自动生成的 equals/hashCode 使用引用比较而非内容比较。
 * 若需要按内容比较两个 LauncherFavorites，请手动调用 icon.contentEquals(other.icon)。
 */
data class LauncherFavorites(
    val title: String,
    val pkg: String,
    val icon: ByteArray?,
    val intent: String,
)

val launcherFavoritesUri = "content://com.android.launcher3.settings/favorites".toUri()
const val SHORTCUT_OVERLAY_DIVISOR = 3
val shortcutOverlayPaint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

/**
 * Launcher3 桌面数据操作封装。
 * 需在 AndroidManifest 声明以下权限：
 *   <uses-permission android:name="com.android.launcher3.permission.READ_SETTINGS" />
 *   <uses-permission android:name="com.android.launcher3.permission.WRITE_SETTINGS" />
 */
class Launcher3(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val packageManager: PackageManager = context.packageManager

    // ─────────────────────────────────────────────
    // 桌面图标查询（favorites 表）
    // ─────────────────────────────────────────────

    /**
     * 查询所有桌面主屏图标（itemType=0，container=-100，不含 HotSeat）。
     * @return 按数据库顺序排列的图标列表
     */
    fun getIcons(): List<LauncherIcon> {
        val icons = mutableListOf<LauncherIcon>()
        val cursor = contentResolver.query(
            launcherFavoritesUri, null, "container = -100 AND itemType = 0", null, null
        ) ?: return icons
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title")) ?: ""
                val intentStr = it.getString(it.getColumnIndexOrThrow("intent")) ?: continue
                val screen = it.getInt(it.getColumnIndexOrThrow("screen"))
                val cellX = it.getInt(it.getColumnIndexOrThrow("cellX"))
                val cellY = it.getInt(it.getColumnIndexOrThrow("cellY"))
                val pkg = try {
                    val intent = Intent.parseUri(intentStr, 0)
                    intent.`package` ?: intent.component?.packageName ?: ""
                } catch (_: Exception) {
                    ""
                }
                icons.add(LauncherIcon(id, title, pkg, screen, cellX, cellY))
            }
        }
        return icons
    }

    /**
     * 查询 HotSeat（Dock 栏）图标，container=-101。
     * screen 字段即槽位编号（0~4），cellY 约定映射为 5 以便与主屏图标统一坐标系。
     */
    private fun getHotSeatIcons(): List<LauncherIcon> {
        val icons = mutableListOf<LauncherIcon>()
        val cursor =
            contentResolver.query(launcherFavoritesUri, null, "container = -101", null, null)
                ?: return icons
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title")) ?: ""
                val intentStr = it.getString(it.getColumnIndexOrThrow("intent")) ?: continue
                val slot = it.getInt(it.getColumnIndexOrThrow("screen"))
                val pkg = try {
                    val intent = Intent.parseUri(intentStr, 0)
                    intent.`package` ?: intent.component?.packageName ?: ""
                } catch (_: Exception) {
                    ""
                }
                icons.add(LauncherIcon(id, title, pkg, screen = 0, cellX = slot, cellY = 5))
            }
        }
        return icons
    }

    // ─────────────────────────────────────────────
    // 桌面图标增删改
    // ─────────────────────────────────────────────

    /**
     * 向桌面或 HotSeat 添加一个 App 图标。
     * cellY == 5 表示目标为 HotSeat，此时 cellX 即槽位编号（0~4）。
     * 若包名对应的应用不存在启动 Intent，操作静默忽略。
     */
    fun addIcon(pkg: String, screen: Int, cellX: Int, cellY: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        val label = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
        val values = ContentValues().apply {
            put("title", label)
            put("intent", launchIntent.toUri(0))
            put("spanX", 1)
            put("spanY", 1)
            put("itemType", 0)
            if (cellY == 5) {
                put("container", -101L)
                put("screen", cellX)
                put("cellX", cellX)
                put("cellY", 0)
            } else {
                put("container", -100L)
                put("screen", screen)
                put("cellX", cellX)
                put("cellY", cellY)
            }
        }
        contentResolver.insert(launcherFavoritesUri, values)
    }

    /**
     * 移动已有图标到新位置。
     * cellY == 5 表示目标为 HotSeat，此时 cellX 即目标槽位编号。
     * @param id 图标的数据库行 ID（来自 getIcons()）
     */
    fun moveIcon(id: Long, screen: Int, cellX: Int, cellY: Int) {
        val values = ContentValues().apply {
            if (cellY == 5) {
                put("container", -101L)
                put("screen", cellX)
                put("cellX", cellX)
                put("cellY", 0)
            } else {
                put("container", -100L)
                put("screen", screen)
                put("cellX", cellX)
                put("cellY", cellY)
            }
        }
        contentResolver.update(launcherFavoritesUri, values, "_id = ?", arrayOf(id.toString()))
    }

    /**
     * 删除桌面图标。
     * @param id 图标的数据库行 ID（来自 getIcons()）
     */
    fun deleteIcon(id: Long) {
        contentResolver.delete(launcherFavoritesUri, "_id = ?", arrayOf(id.toString()))
    }

    // ─────────────────────────────────────────────
    // Launcher 默认值
    // ─────────────────────────────────────────────

    /**
     * 若未配置自定义 Launcher（值为 "android"），回退到系统 Launcher3 包名。
     */
    fun resolveDefaultLauncher(defaultLauncher: String): String =
        if (defaultLauncher == "android") "com.android.launcher3" else defaultLauncher

    /** 预留：清除默认 Launcher 绑定（尚未实现） */
    fun cleanDefaultLauncher() {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
    }

    // ─────────────────────────────────────────────
    // 快捷方式（Deep Shortcut）
    // ─────────────────────────────────────────────

    /**
     * 处理 App 请求"固定快捷方式到桌面"的系统广播（ACTION_CONFIRM_PIN_SHORTCUT）。
     * 解析 PinItemRequest，提取 ShortcutInfo 并合成图标，构造 LauncherFavorites 供调用方写库。
     *
     * Intent 格式：
     *   ACTION_MAIN + category=DEEP_SHORTCUT + package + component + extra(shortcut_id)
     *   #Intent;action=android.intent.action.MAIN;category=com.android.launcher3.DEEP_SHORTCUT;
     *   launchFlags=0x10200000;package=xxx;component=xxx/.Activity;S.shortcut_id=xxx;end
     *
     * @param launcherApps 用于获取快捷方式专属图标，传 null 时降级为 App 图标。
     * @return 解析成功返回含合成图标的 LauncherFavorites，否则返回 null。
     */
    fun pinShortCut(intent: Intent, launcherApps: LauncherApps?): LauncherFavorites? {
        var launcherFavorites: LauncherFavorites? = null
        try {
            val request = IntentCompat.getParcelableExtra(
                intent,
                LauncherApps.EXTRA_PIN_ITEM_REQUEST,
                LauncherApps.PinItemRequest::class.java,
            )
            if (request != null && request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                val shortcutInfo = request.shortcutInfo ?: return null
                val packageName = shortcutInfo.`package`
                val label = shortcutInfo.shortLabel?.toString() ?: packageName
                val id = shortcutInfo.id

                val appIcon: Drawable = packageManager.getApplicationIcon(packageName)
                val shortcutDrawable: Drawable? = launcherApps?.getShortcutIconDrawable(shortcutInfo, 0)
                // 合成图标：快捷方式图标为底，App 图标缩小后叠加在右下角
                val iconBytes = if (shortcutDrawable != null) {
                    shortcutIcon(appIcon, shortcutDrawable)
                } else {
                    drawable2ByteArray(appIcon)
                }

                val intentUri = Intent(Intent.ACTION_MAIN)
                    .addCategory("com.android.launcher3.DEEP_SHORTCUT")
                    .setPackage(packageName)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    .putExtra("shortcut_id", id)
                    .setComponent(shortcutInfo.activity)
                    .toUri(0)
                launcherFavorites = LauncherFavorites(label, packageName, iconBytes, intentUri)
                request.accept()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return launcherFavorites
    }

    /**
     * 从 favorites 表查询所有 Deep Shortcut 条目，并合成组合图标（快捷方式图标+App 角标）。
     * 过滤条件：intent 包含 category=com.android.launcher3.DEEP_SHORTCUT。
     */
    fun getDeepShortcut(): List<LauncherFavorites> {
        val list = mutableListOf<LauncherFavorites>()
        try {
            val cursor = contentResolver.query(
                launcherFavoritesUri,
                arrayOf("title", "intent", "icon"),
                "title IS NOT NULL",
                null,
                null
            )
            cursor?.use {
                val titleIdx = it.getColumnIndex("title")
                val intentIdx = it.getColumnIndex("intent")
                val iconIdx = it.getColumnIndex("icon")
                while (it.moveToNext()) {
                    val title = it.getString(titleIdx) ?: continue
                    val intentStr = it.getString(intentIdx) ?: continue
                    // iconIdx 为 -1 时说明列不存在，getBlob(-1) 会抛异常
                    val iconBytes = if (iconIdx >= 0) it.getBlob(iconIdx) else null
                    try {
                        val parsed = Intent.parseUri(intentStr, 0)
                        if (!parsed.hasCategory("com.android.launcher3.DEEP_SHORTCUT")) continue
                        val pkg = parsed.`package` ?: parsed.component?.packageName ?: continue
                        val icon = shortcutIcon(
                            packageManager.getApplicationIcon(pkg),
                            byteArrayToDrawable(context, iconBytes)
                        )
                        list.add(LauncherFavorites(title, pkg, icon, intentStr))
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * 合成快捷方式组合图标：将 drawableTop（App 图标）缩小后绘制到
     * drawableBottom（快捷方式图标）右下角，叠加尺寸为底图的 1/SHORTCUT_OVERLAY_DIVISOR。
     *
     * @param drawableTop    叠加在右下角的小图（通常为 App 图标）
     * @param drawableBottom 作为底图的大图（通常为快捷方式专属图标）
     * @return 合成后的 PNG ByteArray；失败时返回默认 App 图标的 ByteArray
     */
    fun shortcutIcon(drawableTop: Drawable, drawableBottom: Drawable): ByteArray =
        runCatching {
            val overlayBitmap = drawable2Bitmap(drawableTop)
            val baseBitmap = drawable2Bitmap(drawableBottom).asMutableArgb8888()

            if (baseBitmap.width <= 0 || baseBitmap.height <= 0) {
                return@runCatching drawable2ByteArray(defaultAppDrawable(context))
            }

            val overlayWidth =
                (baseBitmap.width / SHORTCUT_OVERLAY_DIVISOR.toFloat()).toInt().coerceAtLeast(1)
            val overlayHeight =
                (baseBitmap.height / SHORTCUT_OVERLAY_DIVISOR.toFloat()).toInt().coerceAtLeast(1)
            val scaledOverlay =
                if (overlayBitmap.width == overlayWidth && overlayBitmap.height == overlayHeight) {
                    overlayBitmap
                } else {
                    overlayBitmap.scale(overlayWidth, overlayHeight)
                }

            Canvas(baseBitmap).drawBitmap(
                scaledOverlay,
                (baseBitmap.width - overlayWidth).toFloat(),
                (baseBitmap.height - overlayHeight).toFloat(),
                shortcutOverlayPaint
            )
            bitmap2ByteArray(baseBitmap)
        }.getOrElse {
            drawable2ByteArray(defaultAppDrawable(context))
        }

    /**
     * 将 URL 固定为浏览器快捷方式到桌面（通过系统 ShortcutManager requestPinShortcut）。
     * 优先使用系统默认浏览器；若默认浏览器为系统（"android"），
     * 按 Edge → Chrome → AOSP Browser 顺序降级查找。
     *
     * 安全：只允许 http/https scheme，其他 scheme（file://、javascript: 等）直接返回。
     *
     * @param title 快捷方式显示名称
     * @param url   目标 URL，必须以 http:// 或 https:// 开头
     */
    fun createDeepShortCut(title: String, url: String) {
        try {
            val scheme = url.toUri().scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            val edgePackage = "com.microsoft.emmx"
            val chromePackage = "com.android.chrome"
            val browser = "com.android.browser"
            var icon: Icon? = null
            val activityInfo = getDefaultBrowser()
            if (activityInfo != null && "android" != activityInfo.packageName) {
                icon = Icon.createWithBitmap(drawable2Bitmap(activityInfo.loadIcon(packageManager)))
                intent.setPackage(activityInfo.packageName)
            } else {
                val browserPkg = when {
                    isExistPackageName(edgePackage) -> edgePackage
                    isExistPackageName(chromePackage) -> chromePackage
                    isExistPackageName(browser) -> browser
                    else -> null
                }
                if (browserPkg != null) {
                    intent.setPackage(browserPkg)
                    icon = Icon.createWithBitmap(
                        drawable2Bitmap(packageManager.getApplicationIcon(browserPkg))
                    )
                }
            }
            if (icon == null) return
            val shortcut = ShortcutInfo.Builder(context, "${UUID.randomUUID()}")
                .setShortLabel(title)
                .setLongLabel(title)
                .setIcon(icon)
                .setIntent(intent)
                .build()
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─────────────────────────────────────────────
    // 包名 / 浏览器工具
    // ─────────────────────────────────────────────

    /**
     * 检查指定包名的应用是否已安装。
     * 注意：Android 11+ 需在 AndroidManifest 的 <queries> 中声明目标包名，
     * 或持有 QUERY_ALL_PACKAGES 权限，否则此方法始终返回 false。
     */
    fun isExistPackageName(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取系统默认浏览器的 ActivityInfo。
     * 若未设置默认浏览器（系统弹出选择器），resolveActivity 返回 null。
     */
    fun getDefaultBrowser(): ActivityInfo? {
        val intent = Intent(Intent.ACTION_VIEW, "http://".toUri())
        val defaultBrowser =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return defaultBrowser?.activityInfo
    }

    // ─────────────────────────────────────────────
    // 桌面变化监听
    // ─────────────────────────────────────────────

    private class FavoritesObserver(
        handler: Handler,
        private val onChanged: () -> Unit
    ) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = onChanged()
    }

    private var launcherObserver: FavoritesObserver? = null

    /**
     * 注册桌面 favorites 表变化监听器，回调在主线程执行。
     * 若已有监听器，先自动注销再重新注册，避免重复回调。
     */
    fun registerLauncherObserver(onChanged: () -> Unit) {
        launcherObserver?.let { contentResolver.unregisterContentObserver(it) }
        launcherObserver = FavoritesObserver(Handler(Looper.getMainLooper()), onChanged)
        contentResolver.registerContentObserver(launcherFavoritesUri, true, launcherObserver!!)
    }

    /**
     * 注销桌面变化监听器并释放引用，防止内存泄漏。
     */
    fun unregisterLauncherObserver() {
        launcherObserver?.let { contentResolver.unregisterContentObserver(it) }
        launcherObserver = null
    }
}
