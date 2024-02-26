package com.android.systemlib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.storage.IStorageManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.View
import java.net.Inet4Address
import java.net.NetworkInterface

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
    val pm = context.packageManager
    val resolveInfo = pm.resolveActivity(HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.loadLabel(pm)?.toString()
}

/**
 * 获取系统里具有系统权限的Launcher
 * TODO 优化 参考 getSystemDefaultLauncher2 返回ComponentName
 */
fun getSystemDefaultLauncher(context: Context): String? {
    return context.packageManager.queryIntentActivities(
        HOME_INTENT,
        PackageManager.MATCH_DEFAULT_ONLY
    )
        .firstOrNull { it != null && isSystemResolveInfo(it) }?.activityInfo?.packageName
}

/**
 * 判断他是否具有系统属性
 */
@SuppressLint("DiscouragedPrivateApi")
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

const val DISABLE2_NONE = 0x00000000
const val DISABLE2_QUICK_SETTINGS = 1
const val DISABLE2_SYSTEM_ICONS = 1 shl 1
const val DISABLE2_NOTIFICATION_SHADE = 1 shl 2
const val DISABLE2_GLOBAL_ACTIONS = 1 shl 3
const val DISABLE2_ROTATE_SUGGESTIONS = 1 shl 4
const val DISABLE2_MASK: Int =
    (DISABLE2_QUICK_SETTINGS or DISABLE2_SYSTEM_ICONS
            or DISABLE2_NOTIFICATION_SHADE or DISABLE2_GLOBAL_ACTIONS or DISABLE2_ROTATE_SUGGESTIONS)

@SuppressLint("WrongConstant")
fun setStatusBar2(context: Context, status: Int) {
    val service = context.getSystemService("statusbar")
    try {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            (service as StatusBarManager).disable2(status)
//        }
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand = statusBarManager.getMethod("disable2", Int::class.java)
        expand.invoke(service, status)
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

@SuppressLint("MissingPermission")
fun getSN(): String {
    val sn: String
    val meigSerial = SystemProperties.get("persist.radio.sn")
    sn = if (!TextUtils.isEmpty(meigSerial) && meigSerial.length >= 8) {
        meigSerial
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Build.getSerial() else SystemProperties.get(
            "ro.serialno"
        )
    }
    return sn
}

fun getSystemVersion(): String {
    var version = ""
    try {
        version = SystemProperties.get("ro.product.version")
        if (TextUtils.isEmpty(version)) version = SystemProperties.get("ro.build.display.id")
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return version
}

@SuppressLint("MissingPermission")
fun getImei(context: Context): String? {
    return (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
}

fun getWifiMac(context: Context): String {
    var mac = ""
    try {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mac = wifiManager.connectionInfo.macAddress
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return mac
}

fun getBTMac(context: Context): String {
    var mac = ""
    try {
        val bluetoothManager =
            context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mac = bluetoothManager.adapter.address
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return mac
}

fun getIp(): String {
    var ip = ""
    try {
        for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
            if (networkInterface.isUp) {
                networkInterface.interfaceAddresses.forEach {
                    when (it.address) {
                        is Inet4Address -> {
                            it.address.hostAddress?.apply { ip = this }
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {
    }
    return ip
}

fun getSDCard(context: Context): Triple<Long, Long, Long> {
    var pair: Triple<Long, Long, Long> = Triple(0, 0, 1)
    try {
        val iStorageManager =
            IStorageManager.Stub.asInterface(ServiceManager.getService(Context.STORAGE_SERVICE))
        iStorageManager.getVolumes(0)?.forEach {
            if (it.path.contains("emulated")) {
                try {
                    val file = it.getPath()
                    val totalSize = file.totalSpace
                    val availableSize = file.usableSpace
                    val usedSize =
                        file.totalSpace - file.usableSpace
                    pair = Triple(usedSize, availableSize, totalSize)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return pair
}


fun getRomMemorySize(context: Context): Triple<Long, Long, Long> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val outInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(outInfo)
    val total = outInfo.totalMem
    val avail = outInfo.availMem
    val used = outInfo.totalMem - outInfo.availMem
    return Triple(used, avail, total)
}