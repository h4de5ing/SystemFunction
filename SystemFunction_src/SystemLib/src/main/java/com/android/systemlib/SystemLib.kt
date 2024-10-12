package com.android.systemlib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.IActivityManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.IPackageDataObserver
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.IWifiManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ServiceManager
import android.os.SystemProperties
import android.os.storage.StorageManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.TimeZone


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
    return SystemProperties.get(key, "")
}

/**
 * 设置prop里面的值，需要有权限才可以调用成果
 */
fun setSystemPropertyString(key: String, value: String) {
    SystemProperties.set(key, value)
}

/**
 * 查看系统SN号
 */
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

/**
 * 查看系统版本号，主要用于检查OTA升级前后的版本变化
 */
fun getSystemVersion(): String {
    var version = ""
    try {
        version = SystemProperties.get("ro.product.version")
        if (TextUtils.isEmpty(version)) version = SystemProperties.get("ro.build.display.id")
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return version
}


@SuppressLint("MissingPermission", "NewApi", "HardwareIds")
fun getImeis(context: Context): Pair<String, String> {
    var imei1 = ""
    var imei2 = ""
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    try {
        imei1 = telephonyManager.getDeviceId(0)?:""
    } catch (_: Exception) {
    }
    try {
        imei2 = telephonyManager.getDeviceId(1)?:""
    } catch (_: Exception) {
    }
    return Pair(imei1, imei2)
}

@SuppressLint("MissingPermission", "HardwareIds")
fun getSubscriberId(context: Context): String {
    var subscriberId = ""
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    try {
        subscriberId = telephonyManager.subscriberId ?: ""
    } catch (_: Exception) {
    }
    return subscriberId
}

@SuppressLint("HardwareIds")
fun getWifiMac(context: Context): String {
    var mac = ""
    try {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mac = wifiManager.connectionInfo.macAddress
        if ("02:00:00:00:00:00" == mac) {
            (IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE)) as IWifiManager)
                .factoryMacAddresses[0]
        }
    } catch (e: Exception) {
        //e.printStackTrace()
    }
    return mac
}

@SuppressLint("HardwareIds")
fun getBTMac(context: Context): String {
    var mac = ""
    try {
        val bluetoothManager =
            context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mac = bluetoothManager.adapter.address ?: ""
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
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sm.storageVolumes.forEach {
                try {
                    val file = File("${it.getInternalPath()}")
                    if (file.absolutePath.contains("emulated")) {// -> /storage/emulated/0
                        val totalSize = file.totalSpace
                        val availableSize = file.usableSpace
                        val usedSize =
                            file.totalSpace - file.usableSpace
                        pair = Triple(usedSize, availableSize, totalSize)
                    }
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

/**
 * 获取内存是已使用，剩余，总共的容量
 */
fun getRomMemorySize(context: Context): Triple<Long, Long, Long> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val outInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(outInfo)
    val total = outInfo.totalMem
    val avail = outInfo.availMem
    val used = outInfo.totalMem - outInfo.availMem
    return Triple(used, avail, total)
}

fun getProperty(key: String, defaultValue: String): String {
    return try {
        try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod(
                "get",
                String::class.java,
                String::class.java
            )
            get.invoke(c, key, defaultValue) as String
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    } catch (th: Throwable) {
        defaultValue
    }
}

fun setProperty(key: String, value: String?) {
    try {
        val c = Class.forName("android.os.SystemProperties")
        val set = c.getMethod("set", String::class.java, String::class.java)
        set.invoke(c, key, value)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 获取总内存
 */
fun getTotalRam(context: Context): Long {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.getTotalRam() / 1024L / 1024L / 1024L
}

/**
 * 清除单个应用的全部数据
 */
fun clearApplicationUserData(packageName: String, onChange: ((String, Boolean) -> Unit)) {
    val iam = IActivityManager.Stub.asInterface(
        ServiceManager.getService(Context.ACTIVITY_SERVICE)
    )
    iam.clearApplicationUserData(packageName, false, object : IPackageDataObserver.Stub() {
        override fun onRemoveCompleted(p0: String, p1: Boolean) {
            println("把${p0}的数据清除,${if (p1) "成功" else "失败"}")
            onChange(p0, p1)
        }
    }, 0)
}

@SuppressLint("MissingPermission")
fun isNetAvailable(context: Context): Boolean {
    var networkCapabilities: NetworkCapabilities? = null
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT < 23 || manager.getNetworkCapabilities(manager.activeNetwork)
            ?.also { networkCapabilities = it } == null
    ) {
        false
    } else networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
}

fun ping(): Int {
    var result: Int
    var sb: StringBuilder
    try {
        val p: java.lang.Process = Runtime.getRuntime().exec("ping -c 3 -w 100 www.baidu.com")
        val input: InputStream = p.inputStream
        val br = BufferedReader(InputStreamReader(input))
        val strBuffer = StringBuffer()
        while (true) {
            val content = br.readLine() ?: break
            strBuffer.append(content)
        }
        Log.d("---ping", "result data = $strBuffer")
        val status: Int = p.waitFor()
        result = if (status == 0) 200 else 404
        sb = StringBuilder()
    } catch (e: Exception) {
        result = 404
        sb = StringBuilder()
    } catch (th: Throwable) {
        Log.d("---ping", "result code = 0")
        throw th
    }
    Log.d("---ping", sb.append("result code = ").append(result).toString())
    return result
}

fun setHomeActivity(className: ComponentName) {
    IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        .setHomeActivity(className, 0)
}

fun isRoot(): Boolean {
    return try {
        Runtime.getRuntime().exec("su") != null
    } catch (e: IOException) {
        false
    }
}

fun getBatteryCapacity(context: Context): Int {
    var batteryCapacity = 0
    val mPowerProfile: Any
    try {
        val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"
        mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context::class.java)
            .newInstance(context)
        batteryCapacity = (Class.forName(POWER_PROFILE_CLASS).getMethod("getBatteryCapacity")
            .invoke(mPowerProfile) as Double).toInt()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return batteryCapacity
}

/**
 * @description 获取ntp时间
 * @param url ntp服务器地址
 * 中国计量科学研究院NIM授时服务
 * ntp1.nim.ac.cn
 * 教育网
 * edu.ntp.org.cn
 * 阿里云
 * ntp.aliyun.com
 * @param timeout 超时时间,默认3秒
 * @param time 结果回调
 */
@SuppressLint("SimpleDateFormat")
fun getNtpTime(
    url: String = "ntp.aliyun.com",
    timeout: Int = NtpClient.NTP_TIME_OUT_MILLISECOND,
    time: (Long) -> Unit
) {
    MainScope().launch(Dispatchers.IO) {
        val newTime = NtpClient().requestTime(url, timeout)
        withContext(Dispatchers.Main) { time(newTime) }
    }
}

/**
 * 设置日期时间
 * 需要设置权限
 * <uses-permission android:name="android.permission.SET_TIME"/>
 */
@SuppressLint("MissingPermission")
fun setTime(context: Context, time: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
    alarmManager?.setTime(time)
}

/**
 * @param context 上下文
 * @param zone 时区id 例如：Asia/Shanghai
 * 需要权限<uses-permission android:name="android.permission.SET_TIME"/>
 */
@SuppressLint("MissingPermission")
fun setTimeZone(context: Context, zone: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
    alarmManager?.setTimeZone(zone)
}

/**
 * 获取系统支持的所有时区
 */
fun getAllSystemZone(): Array<String>? {
    return TimeZone.getAvailableIDs()
}