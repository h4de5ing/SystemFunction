package com.android.systemlib

import android.annotation.SuppressLint
import android.app.*
import android.app.admin.DevicePolicyManager
import android.app.backup.IBackupManager
import android.content.*
import android.content.pm.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityEventCompat
import com.android.android12.disableCamera12
import com.android.internal.app.IAppOpsService
import java.io.*

/**
 * 移除WIFI配置
 */
@SuppressLint("MissingPermission")
fun removeWifiConfig(context: Context, config: WifiConfiguration): Boolean {
    var removeResult = false
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wifiManager.configuredNetworks?.forEach {
        if (config == it) removeResult = wifiManager.removeNetwork(it.networkId)
    }
    return removeResult
}

/**
 * 移除WIFI配置
 */
@SuppressLint("MissingPermission")
fun removeWifiConfig(context: Context, ssid: String): Boolean {
    var removeResult = false
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    wifiManager.configuredNetworks?.forEach {
        if ("\"${ssid}\"" == it.SSID) {
            removeResult = wifiManager.removeNetwork(it.networkId)
        }
    }
    return removeResult
}

fun addWifiConfig(context: Context, config: WifiConfiguration): Int {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.addNetwork(config)
}

/**
 * 创建wifi连接，但是wifi只在app中生效
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
fun addWifi(context: Context, ssid: String, pass: String) {
    val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val specifier: NetworkSpecifier = WifiNetworkSpecifier.Builder()
        .setSsidPattern(PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
        .setWpa2Passphrase(pass)
        .build()
    //创建一个请求
    val request: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI) //创建的是WIFI网络。
        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) //网络不受限
        .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED) //信任网络，增加这个连个参数让设备连接wifi之后还联网。
        .setNetworkSpecifier(specifier)
        .build()
    connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
        }

        override fun onUnavailable() {
            super.onUnavailable()
        }
    })
}

/**
 * 获取所有已连接的wifi信息
 */
fun getPrivilegedConfiguredNetworks(context: Context): List<WifiConfiguration> {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.getPrivilegedConfiguredNetworks()
}

/**
 * 静默设置默认桌面
 */
fun setDefaultLauncher(context: Context, packageName: String) {
    try {
        val pm = context.packageManager
        val mIPackageManager =
            IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
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
            mIPackageManager.replacePreferredActivity(
                filter,
                AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START,
                cnHomeSets,
                cnAppLock,
                0
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 静默移除默认桌面
 * 默认设备管理可以参考IRoleManager
 */
@SuppressLint("QueryPermissionsNeeded")
fun clearDefaultLauncher(context: Context, packageName: String) {
    val pm = context.packageManager
    pm.queryIntentActivities(HOME_INTENT, 0).forEach { resolveInfo ->
        if (resolveInfo != null) {
            if (packageName == resolveInfo.activityInfo.packageName)
                pm.clearPackagePreferredActivities(resolveInfo.activityInfo.packageName)
        }
    }
    //IRoleManager.Stub.asInterface(ServiceManager.getService(Context.ROLE_SERVICE))
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
    write2File("${getSystemPropertyString(USB_SWITCH_PATH)}", if (isDisable) "1" else "0", false)
}

private const val USB_STATE_PATH = "ro.vendor.usb.hdmi_state.property"
private const val USB_SWITCH_PATH = "ro.vendor.usb.switch.property"

/**
 * 查询是否禁用USB数据传输
 */
fun isUSBDataDisabled(context: Context): Boolean {
    return Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
}

/**
 * 关机
 * 关机的那些方式
 * 方式一:IPowerManager
 * 方式二:IDevicePolicyManager
 */
fun shutdown() = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    .shutdown(false, "shutdown", false)

/**
 * 关机
 */
fun shutdown(context: Context) {
    (context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .shutdown(false, "shutdown", false)
//    val intent = Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN")
//    intent.putExtra("com.android.internal.intent.action.REQUEST_SHUTDOWN", false)
//    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//    context.startActivity(intent)
}

/**
 * 重启
 */
fun reboot() = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    .reboot(false, "reboot", false)

/**
 * 恢复出厂设置
 * Can't perform master clear/factory reset
 */
fun reset(context: Context) {
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

/**
 * 静默安装apk
 */
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
FLAG_ALLOW_BACKUP 是否允许备份
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
@SuppressLint("MissingPermission")
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

/**
 * 禁止卸载应用
 */
@Deprecated("调用下面不需要dpm权限的disUninstallAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun disUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setUninstallBlocked(componentName, packageName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否是禁止卸载
 */
@Deprecated("调用下面不需要dpm权限的isDisUninstallAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun isDisUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isUninstallBlocked(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 禁止卸载应用
 */
fun disUninstallAPP(packageName: String, isDisable: Boolean) {
    val mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    mIPackageManager.setBlockUninstallForUser(packageName, isDisable, 0)
}

/**
 * 是否是禁止卸载
 */
fun isDisUninstallAPP(packageName: String): Boolean {
    val mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    return mIPackageManager.getBlockUninstallForUser(packageName, 0)
}

/**
 * 隐藏app
 */
fun hiddenAPP(packageName: String, isHidden: Boolean) {
    IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        .setApplicationHiddenSettingAsUser(packageName, isHidden, 0)
}

/**
 * 是否是隐藏app
 */
fun isHiddenAPP(packageName: String): Boolean {
    return IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        .getApplicationHiddenSettingAsUser(packageName, 0)
}


/**
 * 暂停应用
 */
fun suspendedAPP(packageName: String, isHidden: Boolean) {
    val mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    mIPackageManager.setPackagesSuspendedAsUser(
        arrayOf(packageName),
        isHidden,
        null,
        null,
        null,
        "android",
        0
    )
}

/**
 * 应用是否被暂停
 */
fun isSuspendedAPP(packageName: String): Boolean {
    return IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        .isPackageSuspendedForUser(packageName, 0)
}

/**
 * PackageManager.COMPONENT_ENABLED_STATE_DEFAULT=0
 * PackageManager.COMPONENT_ENABLED_STATE_ENABLED=1
 * PackageManager.COMPONENT_ENABLED_STATE_DISABLED=2
 * PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER=3
 * PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED=4
 */
fun setDisableAPP(context: Context, packageName: ComponentName, isDisable: Boolean) {
    val pm = context.applicationContext.packageManager
    pm.setComponentEnabledSetting(
        packageName,
        if (isDisable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        0
    )
}

fun isDisableAPP(context: Context, packageName: ComponentName): Boolean {
    val pm = context.applicationContext.packageManager
    return pm.getComponentEnabledSetting(packageName) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
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

/**
 * 添加应用白名单
 */
@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("WrongConstant", "SoonBlockedPrivateApi")
fun addPowerSaveWhitelistApp(context: Context, packageName: String) {
    try {
        val deviceIdleManager = context.getSystemService("deviceidle")
        val manager = Class.forName("android.os.DeviceIdleManager")
        val mServiceField = manager.getDeclaredField("mService")
        mServiceField.isAccessible = true
        val mService = mServiceField.get(deviceIdleManager)
        val iDeviceIdleController = mService as IDeviceIdleController
        iDeviceIdleController.addPowerSaveWhitelistApp(packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 移除应用白名单
 */
@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("WrongConstant", "SoonBlockedPrivateApi")
fun removePowerSaveWhitelistApp(context: Context, packageName: String) {
    try {
        val deviceIdleManager =
            context.applicationContext.getSystemService(DeviceIdleManager::class.java)
        val manager = Class.forName("android.os.DeviceIdleManager")
        val mServiceField = manager.getDeclaredField("mService")
        mServiceField.isAccessible = true
        val mService = mServiceField.get(deviceIdleManager)
        val iDeviceIdleController = mService as IDeviceIdleController
        iDeviceIdleController.removePowerSaveWhitelistApp(packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 判断应用是否在白名单里面
 */
@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("WrongConstant", "SoonBlockedPrivateApi")
fun isPowerSaveWhitelistApp(context: Context, packageName: String): Boolean {
    return try {
        val deviceIdleManager =
            context.applicationContext.getSystemService(DeviceIdleManager::class.java)
        val manager = Class.forName("android.os.DeviceIdleManager")
        val mServiceField = manager.getDeclaredField("mService")
        mServiceField.isAccessible = true
        val mService = mServiceField.get(deviceIdleManager)
        val iDeviceIdleController = mService as IDeviceIdleController
        iDeviceIdleController.isPowerSaveWhitelistApp(packageName)
    } catch (e: Exception) {
        false
    }
}

/**
 * 获取应用白名单
 * 系统预设白名单
 * frameworks\base\data\etc\platform.xml
 * vendor\mediatek\proprietary\frameworks\base\data\etc\platform.xml
 * vendor\partner_gms\etc\sysconfig\google.xml
 */
@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("WrongConstant", "SoonBlockedPrivateApi")
fun getPowerSaveWhitelistApp(context: Context): List<String> {
    return try {
        val deviceIdleManager =
            context.applicationContext.getSystemService(DeviceIdleManager::class.java)
        val manager = Class.forName("android.os.DeviceIdleManager")
        val mServiceField = manager.getDeclaredField("mService")
        mServiceField.isAccessible = true
        val mService = mServiceField.get(deviceIdleManager)
        val iDeviceIdleController = mService as IDeviceIdleController
        iDeviceIdleController.userPowerWhitelist.toList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 应用数据目录具备可读权限
 */
fun canBackup(context: Context, packageName: String): Boolean {
    val flags = (context.applicationContext.packageManager.getApplicationInfo(
        packageName,
        0
    )).flags
    return (flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == ApplicationInfo.FLAG_ALLOW_BACKUP
}

fun backup2() {
    val bm: IBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"))
    bm.isBackupEnabled = true
    println("是否允许备份:${bm.isBackupEnabled} ${bm.isBackupEnabledForUser(0)}")
//    bm.backupNow()
    bm.fullTransportBackupForUser(0, arrayOf("com.android.settings", "com.zzzmode.appopsx"))
}

/**
 * 获取应用的数据目录
 */
fun getPackageDataDir(packageName: String): String {
    val mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    val applicationInfo = mIPackageManager.getApplicationInfo(packageName, 0, 0)
    return applicationInfo.dataDir
}


/**
 * 当第一次刷机或者恢复出厂设置以后这个返回值为true
 */
fun isFirstRun(): Boolean {
    val mIPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    return mIPackageManager.isFirstBoot
}

private fun drawable2Bitmap(icon: Drawable): Bitmap {
    val bitmap =
        Bitmap.createBitmap(
            icon.intrinsicWidth,
            icon.intrinsicHeight,
            if (icon.opacity == PixelFormat.OPAQUE) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    icon.draw(canvas)
    return bitmap
}

//序列化 Drawable->Bitmap->ByteArray
fun drawable2ByteArray(icon: Drawable): ByteArray {
    return bitmap2ByteArray(drawable2Bitmap(icon))
}

private fun bitmap2ByteArray(bitmap: Bitmap): ByteArray {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
}

//反序列化 ByteArray->Bitmap->Drawable
fun byteArray2Drawable(byteArray: ByteArray): Drawable? {
    val bitmap = byteArray2Bitmap(byteArray)
    return if (bitmap == null) null else BitmapDrawable(bitmap)
}

private fun byteArray2Bitmap(byteArray: ByteArray): Bitmap? {
    return if (byteArray.isNotEmpty()) BitmapFactory.decodeByteArray(
        byteArray,
        0,
        byteArray.size
    ) else null
}

/**
 * 结束任务
 */
fun removeTask(taskId: Int): Boolean =
    IActivityManager.Stub.asInterface(ServiceManager.getService(Context.ACTIVITY_SERVICE))
        .removeTask(taskId)

/**
 * 结束任务
 */
fun amTask(taskId: Int) {
    IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"))
        .removeTask(taskId)
}

/**
 * 获取所有应用包名
 */
fun getAllPackages(): List<String> {
    return IPackageManager.Stub.asInterface(ServiceManager.getService("package")).allPackages
}

fun getPermissions(context: Context, packageName: String): List<String> {
//    val pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    val pm = context.applicationContext.packageManager
    //所有申请的权限
    val arrays = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
    arrays.forEach {
        try {
            println(it)
        } catch (e: Exception) {
        }
    }
    return arrays.toList()
}

/**
 * 授权
 * @param code 代表具体的操作权限
 * @param uid user id
 * @param packageName 应用包名
 * @param mode 代币要更改的类型 允许/禁止/提示
 * AppOpsManager.MODE_ALLOWED
 * AppOpsManager.MODE_IGNORED
 * AppOpsManager.MODE_ERRORED
 * AppOpsManager.MODE_DEFAULT
 * AppOpsManager.MODE_FOREGROUND
 */
fun setMode(context: Context, code: Int, packageName: String, mode: Int) {
    val uid = UserHandle.getCallingUserId()
    println("uid:${uid}")
    val opsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
//    opsManager.unsafeCheckOp(op,uid,packageName)//检测是否就有操作权限
//    opsManager.unsafeCheckOpNoThrow(op, uid, packageName)//不抛出异常
//    opsManager.noteOp(op,uid,packageName,"","")//检测权限，会做记录
//    opsManager.noteOpNoThrow()
    val iAppOpsManager =
        IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE))
//    iAppOpsManager.checkPackage()//检测权限有没有被绕过
//    iAppOpsManager.setMode(code, uid, packageName, mode)
//    val list = iAppOpsManager.getOpsForPackage(uid, packageName, null)
//    list?.apply {
//        this.forEach {
//            it.ops?.onEach {
//                println("${it.op} ${it.mode} ${it.opStr}")
//            }
//        }
//    }
    iAppOpsManager.resetAllModes(0, packageName)
}

/**
 * 禁用Sensor
 * 支持：
 * 0 未知
 * 1 麦克风
 * 2 相机
 * 3.传感器
 */
fun disableSensor(isDisable: Boolean, sensor: Int) {
    if (Build.VERSION.SDK_INT > 31) {
        disableCamera12(isDisable, 2)
    } else {

    }
}