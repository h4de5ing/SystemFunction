package com.android.systemlib

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import android.text.TextUtils
import android.view.View

val HOME_INTENT = Intent("android.intent.action.MAIN")
    .addCategory("android.intent.category.HOME")
    .addCategory("android.intent.category.DEFAULT")

/**
 * 获取默认Launcher的包名
 */
fun getDefaultLauncher(context: Context): String? {
    return context.packageManager.resolveActivity(
        HOME_INTENT,
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName
}

/**
 * 判断传入上下文的应用是否是默认Launcher
 */
fun isDefaultLauncher(context: Context): Boolean {
    val info =
        context.packageManager.resolveActivity(HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY)
    return !(info?.activityInfo == null || !context.packageName.equals(info.activityInfo?.packageName))
}

/**
 * 获取默认Launcher的应用名
 */
fun getDefaultLauncherName(context: Context): String? {
    var name = ""
    val pm = context.packageManager
    val resolveInfo = pm.resolveActivity(HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY)
    if (resolveInfo != null) {
        val appName = resolveInfo.activityInfo.loadLabel(pm).toString()
        if (!TextUtils.isEmpty(appName)) {
            name = resolveInfo.activityInfo.loadLabel(pm).toString()
        }
    }
    return name
}

/**
 * 获取系统里具有系统权限的Launcher
 * TODO 优化 参考 getSystemDefaultLauncher2 返回ComponentName
 */
fun getSystemDefaultLauncher(context: Context): String? {
    var packageName = ""
    val pm = context.packageManager
    pm.queryIntentActivities(HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY)
        .forEach { resolveInfo ->
            if (resolveInfo != null) {
                val appName = resolveInfo.activityInfo.loadLabel(pm).toString()
                if (!TextUtils.isEmpty(appName) && isSystemResolveInfo(resolveInfo)) {
                    packageName = resolveInfo.activityInfo.packageName
                }
            }
        }
    return packageName
}

/**
 * 判断他是否具有系统属性
 */
fun isSystemResolveInfo(info: ResolveInfo): Boolean {
    val field = info.javaClass.getDeclaredField("system")
    field.isAccessible = true
    return field.getBoolean(info)
}

/**
 * 获取系统里面的所有Launcher
 */
@SuppressLint("QueryPermissionsNeeded")
fun getAllLaunchers(context: Context): MutableList<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    val pm = context.packageManager
    pm.queryIntentActivities(HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY)
        .forEach { resolveInfo ->
            if (resolveInfo != null) {
                val appName = resolveInfo.activityInfo.loadLabel(pm).toString()
                if (!TextUtils.isEmpty(appName)) {
                    println(resolveInfo.activityInfo.packageName)
                    list.add(
                        Pair(
                            resolveInfo.activityInfo.loadLabel(pm).toString(),
                            resolveInfo.activityInfo.packageName
                        )
                    )
                }
            }
        }
    return list
}

/**
 * 设置szPkg为默认应用
 * szPkg 设置的应用的包名
 */
//fun setDefaultLauncher(context: Context, szPkg: String) {
//    try {
//        val pm = context.packageManager
//        val replacePreferredActivity = pm.javaClass.getMethod(
//            "replacePreferredActivity",
//            IntentFilter::class.java, Integer.TYPE,
//            Array<ComponentName>::class.java,
//            ComponentName::class.java
//        )
//        val filter = IntentFilter(Intent.ACTION_MAIN)
//        filter.addCategory(Intent.CATEGORY_HOME)
//        filter.addCategory(Intent.CATEGORY_DEFAULT)
//        val homeActivities = pm.queryIntentActivities(
//            Intent(Intent.ACTION_MAIN).addCategory(
//                Intent.CATEGORY_HOME
//            ), 0
//        )
//        val cnHomeSets = arrayOfNulls<ComponentName>(homeActivities.size)
//        var cnAppLock: ComponentName? = null
//        for (i in homeActivities.indices) {
//            val info = homeActivities[i].activityInfo
//            cnHomeSets[i] = ComponentName(info.packageName, info.name)
//            if (szPkg == info.packageName) {
//                cnAppLock = cnHomeSets[i]
//            }
//        }
//        if (cnAppLock != null) {
//            replacePreferredActivity.invoke(
//                pm, filter, Integer.valueOf(
//                    AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START
//                ), cnHomeSets, cnAppLock
//            )
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//}

/**
 * 清空默认Launcher
 */
@SuppressLint("QueryPermissionsNeeded")
fun cleanDefaultLauncher(context: Context) {
    try {
        val pm = context.packageManager
        pm.queryIntentActivities(HOME_INTENT, 0).forEach { resolveInfo ->
            if (resolveInfo != null) pm.clearPackagePreferredActivities(resolveInfo.activityInfo.packageName)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

//状态栏相关
const val DISABLE_NONE = 0x00000000 //不禁用任何东西
const val DISABLE_EXPAND = 0x00010000//禁用展开状态栏
const val DISABLE_NOTIFICATION_ICONS = 0x00020000 //禁用状态栏的通知图标
const val DISABLE_NOTIFICATION_ALERTS = 0x00040000 //禁用通知提示
const val DISABLE_NOTIFICATION_TICKER = 0x00080000
const val DISABLE_SYSTEM_INFO = 0x00100000 //禁用系统信息，包含状态栏右侧的wifi 电池等图标
const val DISABLE_HOME = 0x00200000 //禁用Home按键，会隐藏按键
const val DISABLE_RECENT = 0x01000000 //禁用Recent按键，会隐藏按键
const val DISABLE_BACK = 0x00400000  //禁用Back按键，会隐藏按键
const val DISABLE_CLOCK = 0x00800000  //禁用状态栏的时间
const val DISABLE_SEARCH = 0x02000000  //禁用搜索
const val DISABLE_ONGOING_CALL_CHIP = 0x04000000
const val STATUS_DISABLE_NAVIGATION = DISABLE_BACK or DISABLE_HOME or DISABLE_RECENT //禁用导航栏

//隐藏导航栏
const val HIDE_NAVIGATION =
    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN

@SuppressLint("WrongConstant")
fun setStatusBarInt(context: Context, status: Int) {
    val service = context.getSystemService("statusbar")
    try {
//        (service as StatusBarManager).disable(status)
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand = statusBarManager.getMethod("disable", Int::class.java)
        expand.invoke(service, status)
        //如下代码不生效
//        val iStatusBarManager =
//            IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))
//        iStatusBarManager.disable(status, Binder(), context.packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun set(context: Context, enable: Boolean) {
    //自动旋转屏幕
    Settings.System.ACCELEROMETER_ROTATION
    //自动屏幕亮度
    Settings.System.SCREEN_BRIGHTNESS_MODE

//    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//    wifiManager.isWifiEnabled = enable
    //弹出网络面板
//    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    //NFC
//    context.startActivity(Intent(Settings.Panel.ACTION_NFC))
    //媒体音量    //通话音量    //铃声音量    //闹钟音量
//    context.startActivity(Intent(Settings.Panel.ACTION_VOLUME))
//    context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
}


/**
 * 开启本应用的辅助权限
 */
fun enabledAccessibilityServices(context: Context, enable: Boolean) {
    Settings.Secure.putString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        context.packageName
    )
    Settings.Secure.putInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        if (enable) 1 else 0
    )
}

/**
 * 获取getprop里面字段值
 */
fun getSystemPropertyString(key: String): String? {
    return android.os.SystemProperties.get(key, "")
}

fun setSystemPropertyString(key: String, value: String) {
    android.os.SystemProperties.set(key, value)
}