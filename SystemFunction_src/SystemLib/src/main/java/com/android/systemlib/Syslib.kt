package com.android.systemlib

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IPowerManager
import android.os.ServiceManager
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityEventCompat
import java.io.*


/**
 * 移除WIFI配置
 */
fun removeWifiConfig(context: Context, config: WifiConfiguration): Boolean {
    var removeResult = false
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wifiManager.configuredNetworks.forEach {
        if (config == it) removeResult = wifiManager.removeNetwork(it.networkId)
    }
    return removeResult
}

/**
 * 移除WIFI配置
 */
fun removeWifiConfig(context: Context, ssid: String): Boolean {
    var removeResult = false
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wifiManager.configuredNetworks.forEach {
        if ("\"${ssid}\"" == it.SSID) {
            removeResult = wifiManager.removeNetwork(it.networkId)
        }
    }
    return removeResult
}

/**
 * 静默激活设备管理器
 */
fun activeDeviceManager(context: Context, packageName: String, className: String) {
    (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .setActiveAdmin(ComponentName(packageName, className), true)
}

/**
 * 静默取消激活设备管理
 */
fun removeActiveDeviceAdmin(context: Context, packageName: String, className: String) {
    (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .removeActiveAdmin(ComponentName(packageName, className))
}

fun isActiveDeviceManager(context: Context, componentName: ComponentName): Boolean {
    return (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .isAdminActive(componentName)
}

/**
 * 判断是否激活设备管理器
 */
fun isActiveDeviceManager(context: Context, packageName: String, className: String): Boolean {
    return (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .isAdminActive(ComponentName(packageName, className))
}

/**
 * 静默设置默认桌面
 */
fun setDefaultLauncher(context: Context, packageName: String) {
    try {
        val pm = context.packageManager
        val filter = IntentFilter(Intent.ACTION_MAIN)
        filter.addCategory(Intent.CATEGORY_HOME)
        filter.addCategory(Intent.CATEGORY_DEFAULT)

        val homeActivities = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(
                Intent.CATEGORY_HOME
            ), 0
        )
        val cnHomeSets = arrayOfNulls<ComponentName>(homeActivities.size)
        var cnAppLock: ComponentName? = null
        for (i in homeActivities.indices) {
            val info = homeActivities[i].activityInfo
            cnHomeSets[i] = ComponentName(info.packageName, info.name)
            if (packageName == info.packageName) {
                cnAppLock = cnHomeSets[i]
            }
        }
        if (cnAppLock != null) {
            pm.replacePreferredActivity(
                filter,
                AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START,
                cnHomeSets,
                cnAppLock
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 静默移除默认桌面
 */
fun clearDefaultLauncher(context: Context, packageName: String) {
    val pm = context.packageManager
    pm.queryIntentActivities(HOME_INTENT, 0).forEach { resolveInfo ->
        if (resolveInfo != null) {
            if (packageName == resolveInfo.activityInfo.packageName)
                pm.clearPackagePreferredActivities(resolveInfo.activityInfo.packageName)
        }
    }
}

fun setMDM(context: Context, componentName: ComponentName): Boolean {
    val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    //setProfileOwner
    //setActiveProfileOwner
    return dm.setActiveProfileOwner(componentName, "mdm")
}

fun removeMDM(context: Context, componentName: ComponentName) {
    val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    dm.clearProfileOwner(componentName)
}

fun disableMDM(
    context: Context,
    componentName: ComponentName,
    key: String,
    isDisable: Boolean
): Boolean {
    var setResult = false
    try {
        val dm =
            context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (isDisable)
            dm.addUserRestriction(componentName, key)
        else
            dm.clearUserRestriction(componentName, key)
        setResult = true
    } catch (e: Exception) {
        setResult = false
        e.printStackTrace()
    }
    return setResult
}

fun isDisableDMD(context: Context, key: String): Boolean {
    return (context.applicationContext.getSystemService(Context.USER_SERVICE) as UserManager)
        .userRestrictions.getBoolean(key)
}

/**
 * 禁用USB数据传输
 */
fun setUSBDataDisabled(context: Context, isDisable: Boolean) {
    Settings.Global.putInt(
        context.contentResolver,
        Settings.Global.ADB_ENABLED,
        if (isDisable) 0 else 1
    )
    setSwitch(if (isDisable) 1 else 0)
}

private const val USB_STATE_PATH = "ro.vendor.usb.hdmi_state.property"
private const val USB_SWITCH_PATH = "ro.vendor.usb.switch.property"
private fun setSwitch(i: Int) {
    try {
        val path = getSystemPropertyString(USB_SWITCH_PATH)
        if (File(path).exists()) {
            val bufferedWriter = BufferedWriter(FileWriter(path))
            bufferedWriter.write(i.toString())
            bufferedWriter.close()
        } else {
            println("$USB_SWITCH_PATH is not exists")
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

/**
 * 查询是否禁用USB数据传输
 */
fun isUSBDataDisabled(context: Context): Boolean {
    return Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
}

/**
 * 关机设备
 */
fun shutdown() {
    val pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    pm.shutdown(false, "shutdown", false)
//    val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//    dm.reboot(packageName,className)
}

/**
 * 重启设备
 */
fun rebootDevice() {
    val pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    pm.reboot(false, "shutdown", false)
}

/**
 * 恢复出厂设置
 * Can't perform master clear/factory reset
 */
fun resetDevices(context: Context) {
//    val intent = Intent("android.intent.action.FACTORY_RESET")
//    intent.setPackage("android")
//    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
//    intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
//    intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false /*mEraseSdCard*/)
//    intent.putExtra("com.android.internal.intent.extra.WIPE_ESIMS", false /*mEraseEsims*/)
//    context.sendBroadcast(intent)

    val intent = Intent(Intent.ACTION_FACTORY_RESET)
    intent.setPackage("android")
    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm")
    intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, false/*mEraseSdCard*/)
    intent.putExtra(Intent.EXTRA_WIPE_ESIMS, true/*mEraseEsims*/)
    context.sendBroadcast(intent)
}

/**
static final int WIPE_EUICC = 4;
public static final int WIPE_EXTERNAL_STORAGE = 1;
public static final int WIPE_RESET_PROTECTION_DATA = 2;
public static final int WIPE_SILENTLY = 8;
 */
fun wipeDate(context: Context) {
    val dm =
        context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    dm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
}

/**
 * 禁用设备个人热点
 */
fun setHotSpotDisabled(context: Context, isDisable: Boolean) {
    if (isDisable) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.stopSoftAp()
    }
}

/**
 * 是否禁用设备个人热点
 */
fun isHotSpotDisabled(context: Context): Boolean {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.isWifiApEnabled()
}

//启用 禁用数据流量  //TODO 没有测试通过
fun mobile_data(context: Context, isDisable: Boolean) {
    val tm =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    if (isDisable) tm.enableDataConnectivity() else tm.disableDataConnectivity()
}

fun installAPK(context: Context, apkFilePath: String) {
    try {
        val apkFile = File(apkFilePath);
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        sessionParams.setSize(apkFile.length())
        val sessionId = packageInstaller.createSession(sessionParams)
        if (sessionId != -1) {
            val copySuccess = copyInstallFile(packageInstaller, sessionId, apkFilePath)
            if (copySuccess) {
                execInstallCommand(context, packageInstaller, sessionId)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private fun copyInstallFile(
    packageInstaller: PackageInstaller,
    sessionId: Int, apkFilePath: String
): Boolean {
    var `in`: InputStream? = null
    var out: OutputStream? = null
    var session: PackageInstaller.Session? = null
    var success = false
    try {
        val apkFile = File(apkFilePath)
        session = packageInstaller.openSession(sessionId)
        out = session.openWrite("base.apk", 0, apkFile.length())
        `in` = FileInputStream(apkFile)
        var total = 0
        var c: Int
        val buffer = ByteArray(65536)
        while (`in`.read(buffer).also { c = it } != -1) {
            total += c
            out.write(buffer, 0, c)
        }
        session.fsync(out)
        success = true
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        closeQuietly(out)
        closeQuietly(`in`)
        closeQuietly(session)
    }
    return success
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private fun execInstallCommand(
    context: Context,
    packageInstaller: PackageInstaller,
    sessionId: Int
) {
    var session: PackageInstaller.Session? = null
    try {
        session = packageInstaller.openSession(sessionId)
        val intent = Intent(context, InstallResultReceiver::class.java)
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        session.commit(pendingIntent.intentSender)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        closeQuietly(session)
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println(
            "${
                intent?.getIntExtra(
                    PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE
                )
            }"
        )
    }
}

private fun closeQuietly(c: Closeable?) {
    if (c != null) {
        try {
            c.close()
        } catch (ignored: IOException) {
            ignored.printStackTrace()
        }
    }
}

/**
 * 判断是否具有system权限
FLAG_ALLOW_BACKUP
FLAG_ALLOW_CLEAR_USER_DATA
FLAG_ALLOW_TASK_REPARENTING
FLAG_DEBUGGABLE 是否允许被调试
FLAG_EXTERNAL_STORAGE
FLAG_EXTRACT_NATIVE_LIBS
FLAG_FACTORY_TEST  设备在工厂测试模式下运行
FLAG_FULL_BACKUP_ONLY
FLAG_HARDWARE_ACCELERATED
FLAG_HAS_CODE 具有与其关联的代码
FLAG_INSTALLED
FLAG_IS_DATA_ONLY
FLAG_IS_GAME 失效

FLAG_KILL_AFTER_RESTORE
FLAG_LARGE_HEAP
FLAG_MULTIARCH
FLAG_PERSISTENT 持久的,是否表示白名单
FLAG_RESIZEABLE_FOR_SCREENS
FLAG_RESTORE_ANY_VERSION
FLAG_STOPPED 停止状态
FLAG_SUPPORTS_LARGE_SCREENS
FLAG_SUPPORTS_NORMAL_SCREENS
FLAG_SUPPORTS_RTL
FLAG_SUPPORTS_SCREEN_DENSITIES 失效，弃用
FLAG_SUPPORTS_SMALL_SCREENS 适配小屏幕
FLAG_SUPPORTS_XLARGE_SCREENS 适配大屏幕
FLAG_SUSPENDED  是否被挂起，图标可见，应用不可用
FLAG_SYSTEM 系统级别应用
FLAG_TEST_ONLY 仅用于测试
FLAG_UPDATED_SYSTEM_APP 内置系统应用程序的更新
FLAG_USES_CLEARTEXT_TRAFFIC
FLAG_VM_SAFE_MODE 安全模式运行
 */
fun isSystemAPP(context: Context, packageName: String): Boolean {
    return try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, 0)
        val flags: Int = packageInfo.applicationInfo.flags
//    flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        (flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM
    } catch (e: Exception) {
        false
    }
}

/**
 * 判断是否为挂起状态
 */
fun isSuspended(context: Context, packageName: String): Boolean {
    return try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, 0)
        val flags: Int = packageInfo.applicationInfo.flags
        (flags and ApplicationInfo.FLAG_SUSPENDED) == ApplicationInfo.FLAG_SUSPENDED
    } catch (e: Exception) {
        false
    }

}

/**
 * 判断APP是否可以卸载
 */
fun canUninstall(context: Context, packageName: String): Boolean {
    return try {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(packageName, 0)
        val flags: Int = packageInfo.applicationInfo.flags
        (flags and ApplicationInfo.FLAG_INSTALLED) == ApplicationInfo.FLAG_INSTALLED
    } catch (e: Exception) {
        false
    }
}

/**
 * 静默卸载
 */
fun uninstall(context: Context, packageName: String) {
    val intent = Intent()
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    val sender = PendingIntent.getActivity(context, 0, intent, 0)
    val packageInstaller: PackageInstaller = context.packageManager.packageInstaller
    packageInstaller.uninstall(packageName, sender.intentSender)
}

/**
 * 设置使用是否可用
 * 会显示在Launcher中
 * pm list packages 可以显示包名
 *
 */
fun disableApp(context: Context, componentName: ComponentName, isDisable: Boolean) {
    context.packageManager.setComponentEnabledSetting(
        componentName,
        if (isDisable) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )
}

//fun getStatusBarHeight(context: Context): Int {
//    return if (Build.VERSION.SDK_INT >= 30) {
//        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val windowMetrics = wm.currentWindowMetrics
//        val windowInsets = windowMetrics.windowInsets
//        val insets =
//            windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
//        insets.top
//    } else {//TODO 以前怎么获取的高度
//        0
//    }
//}


fun getPkgList(): MutableList<String> {
    val packages: MutableList<String> = ArrayList()
    try {
        val p = Runtime.getRuntime().exec("pm list packages")
        val isr = InputStreamReader(p.inputStream, "utf-8")
        val br = BufferedReader(isr)
        var line = br.readLine()
        while (line != null) {
            line = line.trim { it <= ' ' }
            if (line.length > 8) {
                val prefix = line.substring(0, 8)
                if (prefix.equals("package:", ignoreCase = true)) {
                    line = line.substring(8).trim { it <= ' ' }
                    if (!TextUtils.isEmpty(line)) {
                        packages.add(line)
                    }
                }
            }
            line = br.readLine()
        }
        br.close()
        p.destroy()
    } catch (t: Exception) {
        t.printStackTrace()
    }
    return packages
}
