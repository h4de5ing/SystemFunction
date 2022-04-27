package com.android.systemlib

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager.EXTRA_REASON
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.IPowerManager
import android.os.ServiceManager
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.view.accessibility.AccessibilityEventCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


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
 * 添加禁止安装应用列表
 */
fun addForbiddenInstallApp(context: Context, packageNameList: List<String>) {

}

/**
 * 获取被禁止安装应用列表接口
 */
fun getForbiddenInstallAppList(context: Context): List<String> {
    return emptyList()
}

/**
 * 移除禁止安装应用列表
 */
fun removeForbiddenInstall(context: Context, packageNameList: List<String>) {

}

/**
 * 添加应用安装白名单
 */
fun addInstallPackageTrustList(context: Context, packageNameList: List<String>) {}

/**
 * 获取应用安装白名单
 */
fun getInstallPackageTrustList(context: Context): List<String> {
    return emptyList()
}

/**
 * 删除应用安装白名单
 */
fun removeInstallPackageTrustList(context: Context, packageNameList: List<String>) {

}

/**
 * 添加禁止卸载应用列表
 */
fun addDisallowedUninstallPackages(context: Context, packageNameList: List<String>) {
//    val dm =
//        context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
//    dm.setUninstallBlocked(componentName2, "com.guoshi.httpcanary", true)
}

/**
 * 获取被禁止卸载应用列表
 */
fun getDisallowedUninstallPackageList(context: Context): List<String> {
    return emptyList()
}

/**
 * 移除禁止卸载应用列表
 */
fun removeDisallowedUninstallPackages(context: Context, packageNameList: List<String>) {}

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


/**
 * 禁用彩信
 */
fun disableMms(context: Context, isDisable: Boolean) {

}

/**
 * 查询是否禁用彩信
 */
fun isMmsDisable(context: Context) {

}

/**
 * 禁用设备系统升级
 */
fun setSystemUpdateDisable(context: Context, isDisable: Boolean) {

}

/**
 * 查询系统升级功能禁用状态
 */
fun isSystemUpdateDisabled(context: Context): Boolean {
    return false
}

/**
 * 添加系统应用保活白名单
 */
fun addPersistentApp(context: Context, packageNameList: List<String>) {}

/**
 * 移除系统应用保活白名单
 */
fun removePersistentApp(context: Context, packageNameList: List<String>) {

}

/**
 * 添加受信任应用白名单
 */
fun setSuperWhiteListForSystem(context: Context, packageNameList: List<String>) {}

/**
 * 移除受信任应用白名单
 */
fun remoteSuperWhiteListForSystem(context: Context, packageNameList: List<String>) {

}

//启用 禁用数据流量  //TODO 没有测试通过
fun mobile_data(context: Context, isDisable: Boolean) {
    val tm =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    if (isDisable) tm.enableDataConnectivity() else tm.disableDataConnectivity()
}

fun getSettings(context: Context) {
    val mSettings: Settings = Settings()
    mSettings
}