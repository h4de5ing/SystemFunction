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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toDrawable
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
 * Launcher3控制
 *  <uses-permission android:name="com.android.launcher3.permission.READ_SETTINGS" />
 *  <uses-permission android:name="com.android.launcher3.permission.WRITE_SETTINGS" />
 */
class Launcher3(private val context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val packageManager: PackageManager = context.packageManager

    // 所有桌面 App 图标（itemType=0，全部 screen）
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

    // hotSeat（dock）图标：container = -101，screen = 槽位编号 0~4
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
        contentResolver.update(launcherFavoritesUri, values, "_id = $id", null)
    }

    fun deleteIcon(id: Long) {
        contentResolver.delete(launcherFavoritesUri, "_id = $id", null)
    }

    /**
     * 如果没有配置默认Launcher就使用Launcher3作为默认Launcher
     */
    fun resolveDefaultLauncher(defaultLauncher: String): String =
        if (defaultLauncher == "android") "com.android.launcher3" else defaultLauncher

    fun cleanDefaultLauncher() {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
    }

    //处理桌面创建快捷方式
    /**
     * 处理从系统桌面接收到的快捷方式固定请求，提取其中的 ShortcutInfo 并保存到数据库中。
     * ACTION_MAIN + DEEP_SHORTCUT + setPackage + setFlags(NEW_TASK|RESET_TASK) + putExtra(shortcut_id) + setComponent(shortcutInfo.activity)
     *  #Intent;action=android.intent.action.MAIN;category=com.android.launcher3.DEEP_SHORTCUT;launchFlags=0x10200000;package=xxx;component=xxx/.Activity;S.shortcut_id=xxx;end
     */
    fun pinShortCut(
        intent: Intent,
        launcherApps: LauncherApps?
    ): LauncherFavorites? {
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
                val label = shortcutInfo.shortLabel
                val id = shortcutInfo.id
                val packageIcon: Drawable = packageManager.getApplicationIcon(packageName)

                val icon: Drawable? = launcherApps?.getShortcutIconDrawable(shortcutInfo, 0)
                // Mirror ShortcutKey.makeIntent(ShortcutInfo si) from Launcher3
                val intentUri = Intent(Intent.ACTION_MAIN)
                    .addCategory("com.android.launcher3.DEEP_SHORTCUT")
                    .setPackage(packageName)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    .putExtra("shortcut_id", id)
                    .setComponent(shortcutInfo.activity)
                    .toUri(0)
                launcherFavorites = LauncherFavorites(
                    "$label",
                    packageName,
                    null,
                    intentUri,
                )
                request.accept()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return launcherFavorites
    }

    fun getDeepShortcut(): List<LauncherFavorites> {
        val list = mutableListOf<LauncherFavorites>()
        try {
            val cursor =
                contentResolver.query(
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
                    val iconBytes = it.getBlob(iconIdx)
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

    fun createDeepShortCut(
        title: String,
        url: String,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            val edgePackage = "com.microsoft.emmx"
            val chromePackage = "com.android.chrome"
            val browser = "com.android.browser"
            var icon: Icon? = null
            val activityInfo = getDefaultBrowser()
            if (activityInfo != null && "android" != activityInfo.packageName) {
                icon =
                    Icon.createWithBitmap(drawable2Bitmap(activityInfo.loadIcon(packageManager)))
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
            val shortcut =
                ShortcutInfo.Builder(context, "${UUID.randomUUID()}").setShortLabel(title)
                    .setLongLabel(title).setIcon(icon).setIntent(intent).build()
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isExistPackageName(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getDefaultBrowser(): ActivityInfo? {
        val intent = Intent(Intent.ACTION_VIEW, "http://".toUri())
        val defaultBrowser =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return defaultBrowser?.activityInfo
    }


    //监听桌面变化
    private class FavoritesObserver(
        handler: Handler,
        private val onChanged: () -> Unit
    ) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = onChanged()
    }

    private var launcherObserver: FavoritesObserver? = null

    /**
     * 监听桌面变化
     */
    fun registerLauncherObserver(onChanged: () -> Unit) {
        launcherObserver = FavoritesObserver(Handler(), onChanged)
        contentResolver.registerContentObserver(
            launcherFavoritesUri,
            true,
            launcherObserver!!
        )
    }

    /**
     * 取消桌面变化监听
     */
    fun unregisterLauncherObserver() {
        launcherObserver?.let { contentResolver.unregisterContentObserver(it) }
        launcherObserver = null
    }
}