package com.android.systemlib

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.IPowerManager
import android.os.ServiceManager
import androidx.core.view.accessibility.AccessibilityEventCompat


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
fun activateDeviceManager(context: Context, packageName: String, className: String) {
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

/**
 * 禁用设备蓝牙
 */
fun setBluetoothDisable(context: Context, isDisable: Boolean): Boolean {
    return false
}

/**
 * 查询是否禁用蓝牙设备
 */
fun isBluetoothDisabled(context: Context): Boolean {
    return false
}

/**
 * 禁用USB数据传输
 */
fun setUSBDataDisabled(context: Context, isDisable: Boolean) {

}

/**
 * 查询是否禁用USB数据传输
 */
fun isUSBDataDisabled(): Boolean {
    return false
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
 * 禁用设备截屏
 */
fun setScreenShotDisabled(context: Context, isDisable: Boolean) {
//    setScreenCaptureDisabled
}

/**
 * 查询是否禁用设备截屏
 */
fun isScreenShotDisabled(context: Context): Boolean {
    return false
}

/**
 * 禁用设备TF卡存储
 */
fun setTFCardDisabled(context: Context, isDisable: Boolean) {}

/**
 * 查询是否禁用TF卡存储
 */
fun isTFCardDisabled(context: Context): Boolean {
    return false
}

/**
 * 禁用拨打电话
 */
fun setCallPhoneDisabled(context: Context, isDisable: Boolean) {

}

/**
 * 是否禁用拨打电话
 */
fun isCallPhoneDisabled(context: Context): Boolean {
    return false
}

/**
 * 关机设备
 */
fun shutdown(context: Context) {
    val pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    pm.shutdown(true, "shutdown", false)
}

/**
 * 重启设备
 */
fun rebootDevice(context: Context) {
    val pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE))
    pm.reboot(true, "shutdown", false)
}

/**
 * 恢复出厂设置
 */
fun resetDevices(context: Context) {
    val intent = Intent("android.intent.action.FACTORY_RESET")
    intent.setPackage("android")
    intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm")
    intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", false /*mEraseSdCard*/)
    intent.putExtra("com.android.internal.intent.extra.WIPE_ESIMS", true /*mEraseEsims*/)
    context.sendBroadcast(intent)
}

/**
 * 禁用设备个人热点
 */
fun setHotSpotDisabled(context: Context, isDisable: Boolean) {
    if (isDisable) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        wifiManager.cancelLocalOnlyHotspotRequest()
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
 * 禁用短信
 */
fun disableSms(context: Context, isDisable: Boolean) {

}

/**
 * 查询是否禁用短信
 */
fun isSmsDisable(context: Context): Boolean {
    return false
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
 * 禁用录音功能
 */
fun setMicrophoneDisable(context: Context, isDisable: Boolean) {

}

/**
 * 查询录音功能禁用状态
 */
fun isMicrophoneDisable(context: Context) {

}

/**
 * 禁止使用WIFI
 */
fun setWifiDisabled(context: Context, isDisable: Boolean) {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    if (isDisable)
        if (wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = false
        else if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true
}

/**
 * 查询是否禁止使用WiFi
 */
fun isWifiDisabled(context: Context): Boolean {
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

/**
 * 禁用或允许设备录屏
 */
fun setScreenCaptureDisabled(context: Context, isDisable: Boolean) {}

/**
 * 查询是否禁用设备录屏
 */
fun isScreenCaptureDisabled(context: Context, isDisable: Boolean): Boolean {
    return false
}

/**
 * 禁止设备GPS功能
 */
fun setGPSDisabled(context: Context, isDisable: Boolean) {}

/**
 * 查询是否禁用设备GPS
 */
fun isGPSDisable(context: Context): Boolean {
    return false
}

/**
 * 禁止设备恢复出厂设备
 */
fun setRestoreFactoryDisabled(context: Context) {

}

/**
 * 查询是否禁用设备恢复出厂设备
 */
fun isRestoreFactoryDisable(context: Context): Boolean {
    return false
}

/**
 * 禁止设备移动数据网络
 */
fun setDataConnectivityDisabled(context: Context, isDisable: Boolean) {

}

/**
 * 查询是否禁用移动数据网络
 */
fun isDataConnectivityDisabled(context: Context): Boolean {
    return false
}