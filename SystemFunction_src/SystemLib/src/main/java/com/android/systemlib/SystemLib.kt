package com.android.systemlib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.IActivityManager
import android.app.usage.IStorageStatsManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.IPackageDataObserver
import android.content.pm.IPackageManager
import android.content.pm.IPackageStatsObserver
import android.content.pm.PackageManager
import android.content.pm.PackageStats
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.IWifiManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.IPowerManager
import android.os.ServiceManager
import android.os.SystemProperties
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.android.internal.app.LocalePicker
import com.android.internal.util.MemInfoReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.TimeZone
import java.util.UUID


val HOME_INTENT = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    .addCategory(Intent.CATEGORY_DEFAULT)

val HOME_INTENT_FILTER = IntentFilter(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_HOME)
    addCategory(Intent.CATEGORY_DEFAULT)
}

/**
 * 获取默认Launcher的包名
 */
fun getDefaultLauncher(context: Context): String? {
    return context.packageManager.resolveActivity(
        HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY
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
        HOME_INTENT, PackageManager.MATCH_DEFAULT_ONLY
    ).firstOrNull { it != null && isSystemResolveInfo(it) }?.activityInfo?.packageName
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
    (DISABLE2_QUICK_SETTINGS or DISABLE2_SYSTEM_ICONS or DISABLE2_NOTIFICATION_SHADE or DISABLE2_GLOBAL_ACTIONS or DISABLE2_ROTATE_SUGGESTIONS)

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
 * 获取getprop里面字段值
 */
fun getSystemPropertyString(key: String): String? = SystemProperties.get(key, "")

/**
 * 设置prop里面的值，需要有权限才可以调用成果
 */
fun setSystemPropertyString(key: String, value: String) {
    SystemProperties.set(key, value)
}

/**
 * 获取系统SN号
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
        imei1 = telephonyManager.getDeviceId(0) ?: ""
    } catch (_: Exception) {
    }
    try {
        imei2 = telephonyManager.getDeviceId(1) ?: ""
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
            (IWifiManager.Stub.asInterface(ServiceManager.getService(Context.WIFI_SERVICE)) as IWifiManager).factoryMacAddresses[0]
        }
    } catch (_: Exception) {
    }
    return mac
}

@SuppressLint("HardwareIds", "MissingPermission")
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

/**
 * 获取SD卡已使用，剩余，总共的容量
 */
fun getSDCard(): Triple<Long, Long, Long> {
    var pair: Triple<Long, Long, Long> = Triple(0, 0, 1)
    try {
        val file = Environment.getExternalStorageDirectory()
        val totalSize = file.totalSpace
        val availableSize = file.usableSpace
        val usedSize = file.totalSpace - file.usableSpace
        pair = Triple(usedSize, availableSize, totalSize)
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

/**
 * 获取总内存
 */
fun getTotalRam(): Long = MemInfoReader().let {
    it.readMemInfo()
    it.totalSize / 1024L / 1024L / 1024L
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
    } else networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}

fun ping(): Int {
    var result: Int
    var sb: StringBuilder
    try {
        val p: Process = Runtime.getRuntime().exec("ping -c 3 -w 100 www.baidu.com")
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
    } catch (_: Exception) {
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
    } catch (_: IOException) {
        false
    }
}

fun getBatteryCapacity(context: Context): Int {
    var batteryCapacity = 0
    val mPowerProfile: Any
    try {
        val powerProFileClass = "com.android.internal.os.PowerProfile"
        mPowerProfile = Class.forName(powerProFileClass).getConstructor(Context::class.java)
            .newInstance(context)
        batteryCapacity = (Class.forName(powerProFileClass).getMethod("getBatteryCapacity")
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
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setTime(time)
}

/**
 * @param context 上下文
 * @param zone 时区id 例如：Asia/Shanghai
 * 需要权限<uses-permission android:name="android.permission.SET_TIME"/>
 */
@SuppressLint("MissingPermission")
fun setTimeZone(context: Context, zone: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setTimeZone(zone)
}

/**
 * 获取系统支持的所有时区
 */
fun getAllSystemZone(): Array<String>? = TimeZone.getAvailableIDs()

/**
 * 是否锁屏
 */
fun isScreenOn(): Boolean =
    IPowerManager.Stub.asInterface(ServiceManager.getService("power")).isInteractive


/**
 * 设置系统语言
 */
fun setConfiguration(language: String): Boolean {
    try {
        var locale: Locale? = null
        if (language.contains("-")) {
            val splits = language.split("-")
            if (splits.size == 2) locale = Locale(splits[0], splits[1])
            else if (splits.size >= 3) locale = Locale(splits[0], splits[splits.size - 1])
        } else locale = Locale(language)
        LocalePicker.updateLocale(locale)
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

/**
 * 分屏显示，第一个显示在上方
 * component1
 * component2
 * 如果是竖屏 1在上面 2在下面
 * 如果是横屏 1在左边 2在右边
 *
 * https://source.android.google.cn/docs/core/display/multi-window?hl=zh-cn
 *   //未定义
 *     public static final int WINDOWING_MODE_UNDEFINED = 0;
 *     //普通全屏窗口
 *     public static final int WINDOWING_MODE_FULLSCREEN = 1;
 *     //画中画
 *     public static final int WINDOWING_MODE_PINNED = 2;
 *     //分屏主窗口
 *     public static final int WINDOWING_MODE_SPLIT_SCREEN_PRIMARY = 3;
 *     //分屏副窗口
 *     public static final int WINDOWING_MODE_SPLIT_SCREEN_SECONDARY = 4;
 *     //自由窗口 自由窗口模式里面，窗口支持放大缩小以及移动位置，原理是不断的更改Task的边界(用Rect表示)，然后根据Task的边界来重新缩放Task，从而达到窗口缩放和拖动的作用。
 *     public static final int WINDOWING_MODE_FREEFORM = 5;
 */
fun enterSplitScreen(context: Context, component1: ComponentName, component2: ComponentName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent1 = Intent()
        intent1.component = component1
        intent1.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val options1 = ActivityOptions.makeBasic()
        context.startActivity(intent1, options1.toBundle())

        val intent2 = Intent()
        intent2.component = component2
        intent2.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent2)
    }
}

/**
 * 多屏显示
 */
fun enterMultiScreen(context: Context, component1: ComponentName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //display0表达第一块屏幕 display1表达第二块屏幕
            options.launchDisplayId = 0
        }
        val secondIntent = Intent()
        secondIntent.component = component1
        secondIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        context.startActivity(secondIntent, options.toBundle())
    }
}

private const val contextFlagsIncludeCode = 0x00000001
private const val contextFlagsIgnoreSecurity = 0x00000002
private const val contextFlagsRestricted = 0x00000004
private const val contextFlagsDeviceProtectedStorage = 0x00000008
private const val contextFlagsCredentialProtectedStorage = 0x00000010
private const val contextFlagsRegisterPackage = 0x40000000

/**
 * 获取了对应app的上下文，就可以获取目标应用的一些数据
 * 动态加载其他应用程序的代码：通过使用 CONTEXT_INCLUDE_CODE 和 CONTEXT_IGNORE_SECURITY，你可以加载其他应用程序的类并实例化对象，调用方法。这常用于插件化框架。
 *
 * 访问其他应用程序的资源：即使没有包含代码，你也可以访问其他应用程序的资源（如字符串、图片等）。例如，你可以使用 getResources() 方法获取目标应用程序的资源。
 *
 * 访问特定存储区域的数据：通过 CONTEXT_DEVICE_PROTECTED_STORAGE 和 CONTEXT_CREDENTIAL_PROTECTED_STORAGE，你可以访问目标应用程序在不同安全级别的存储区域的数据。
 *
 * 绕过安全限制：使用 CONTEXT_IGNORE_SECURITY 可以绕过一些安全限制，但要注意这可能会带来安全风险，并且只有在调用者具有足够权限（如系统应用）时才可能成功。
 *
 * 创建受限的上下文：使用 CONTEXT_RESTRICTED 可以创建一个受限制的上下文，用于安全沙盒环境。
 */
fun getPackageContext(context: Context, packageName: String) {
    try {
        val targetContext =
            context.createPackageContext(packageName, 0)
        val appInfo = targetContext.packageManager.getApplicationInfo(packageName, 0)
        val assets = targetContext.resources.assets.list("")
        assets?.forEach {
            println("assets=$it")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private val UUID_PRIVATE_INTERNAL: String? = null
private const val UUID_PRIMARY_PHYSICAL = "primary_physical"
private const val UUID_SYSTEM = "system"
private const val FAT_UUID_PREFIX = "fafafafa-fafa-5afa-8afa-fafa"
private val UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69")
private val UUID_PRIMARY_PHYSICAL_ = UUID.fromString("0f95a519-dae7-5abf-9519-fbd6209e05fd")
private val UUID_SYSTEM_ = UUID.fromString("5d258386-e60d-59e3-826d-0089cdd42cc0")
fun convert(storageUuid: UUID): String? {
    if (UUID_DEFAULT == storageUuid) {
        return UUID_PRIVATE_INTERNAL
    } else if (UUID_PRIMARY_PHYSICAL_ == storageUuid) {
        return UUID_PRIMARY_PHYSICAL
    } else if (UUID_SYSTEM_ == storageUuid) {
        return UUID_SYSTEM
    } else {
        val uuidString = storageUuid.toString()
        // This prefix match will exclude fsUuids from private volumes because
        // (a) linux fsUuids are generally Version 4 (random) UUIDs so the prefix
        // will contain 4xxx instead of 5xxx and (b) we've already matched against
        // known namespace (Version 5) UUIDs above.
        if (uuidString.startsWith(FAT_UUID_PREFIX)) {
            val fatStr = uuidString.substring(FAT_UUID_PREFIX.length).uppercase()
            return fatStr.substring(0, 4) + "-" + fatStr.substring(4)
        }
        return storageUuid.toString()
    }
}

fun getStorageStats(context: Context, storageUuid: UUID, packageName: String): LongArray {
    val pm = context.packageManager
    var result = longArrayOf(0L, 0L, 0L)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val iStorage =
                IStorageStatsManager.Stub.asInterface(ServiceManager.getService(Context.STORAGE_STATS_SERVICE))
            val storageStats = iStorage.queryStatsForPackage(
                convert(storageUuid), packageName, 0, context.packageName
            )
            result = longArrayOf(
                storageStats.cacheBytes, storageStats.appBytes, storageStats.dataBytes
            )
        } else {
            try {
                val clazz = Class.forName("android.content.pm.PackageManager")
                val method = clazz.getMethod(
                    "getPackageSizeInfo",
                    String::class.java,
                    Class.forName("android.content.pm.IPackageStatsObserver")
                )
                val observer = object : IPackageStatsObserver.Stub() {
                    override fun onGetStatsCompleted(stats: PackageStats?, succeeded: Boolean) {
                        if (succeeded) {
                            println("cacheBytes=${stats?.cacheSize}")
                            println("appBytes=${stats?.codeSize}")
                            println("dataBytes=${stats?.dataSize}")
                            result = longArrayOf(
                                stats?.cacheSize ?: 0L, stats?.codeSize ?: 0L, stats?.dataSize ?: 0L
                            )
                        }
                    }
                }
                method.invoke(pm, packageName, observer)
                // 注意：此方法在 Android 5.0+ 已被废弃，可能无效，实际开发中建议使用 StorageStatsManager（API 26+）
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}