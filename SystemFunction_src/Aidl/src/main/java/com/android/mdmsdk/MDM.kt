package com.android.mdmsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.IBinder

/**
 * 绑定设置服务
 */
fun bind(context: Context) {
    val intent = Intent()
    intent.action = "com.android.systemfunction.ForegroundService"
    intent.setPackage("com.android.systemfunction")
    context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
}

private var mService: IRemoteInterface? = null
private val conn: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        try {
            mService = IRemoteInterface.Stub.asInterface(service)
            println("onServiceConnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        println("onServiceDisconnected")
        mService = null
    }
}

/**
 * 取消绑定设置服务
 */
fun unbind(context: Context) {
    context.unbindService(conn)
}

/**
 * 判断服务是否绑定成功
 */
fun isBind(): Boolean {
    return mService != null
}

/**
 * 禁用Home键按键
 */
fun setHomeKeyDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_HOME.name, isDisable)
}

/**
 * 查询是否禁用Home键按键
 */
fun isHomeKeyDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_HOME.name) ?: false
}

/**
 * 禁用最近应用按键
 */
fun setRecentKeyDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_RECENT.name, isDisable)
}

/**
 * 查询是否禁用最近应用键按键
 */
fun isRecentKeyDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_RECENT.name) ?: false
}

/**
 * 禁用返回键
 */
fun setBackKeyDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_BACK.name, isDisable)
}

/**
 * 查询是否禁用返回键
 */
fun isBackKeyDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_BACK.name) ?: false
}

/**
 * 禁用导航栏 上面的三个按键，Back Home Recent
 */
fun setNavigaBarDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_NAVIGATION.name, isDisable)
}

/**
 * 查询是否禁用导航栏
 */
fun isNavigaBarDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_NAVIGATION.name) ?: false
}

/**
 * 禁用状态栏
 */
fun setStatusBarDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_STATUS.name, isDisable)
}

/**
 * 查询是否禁用状态栏
 */
fun isStatusBarDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_STATUS.name) ?: false
}

/**
 * 禁用设备蓝牙
 */
fun setBluetoothDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_BLUETOOTH.name, isDisable)
}

/**
 * 查询是否禁用蓝牙设备
 */
fun isBluetoothDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_BLUETOOTH.name) ?: false
}

/**
 * 禁用设备个人热点
 */
fun setHotSpotDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_HOT_SPOT.name, isDisable)
}

/**
 * 是否禁用设备个人热点
 */
fun isHotSpotDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_HOT_SPOT.name) ?: false
}

/**
 * 禁止使用WIFI
 */
fun setWifiDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_WIFI.name, isDisable)
}

/**
 * 查询是否禁止使用WiFi
 */
fun isWifiDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_WIFI.name) ?: false
}

/**
 * 禁止设备GPS功能
 */
fun setGPSDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_GPS.name, isDisable)
}

/**
 * 查询是否禁用设备GPS
 */
fun isGPSDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_GPS.name) ?: false
}

/**
 * 禁用USB数据传输
 */
fun setUSBDataDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_USB_DATA.name, isDisable)
}

/**
 * 查询是否禁用USB数据传输
 */
fun isUSBDataDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_USB_DATA.name) ?: false
}


/**
 * 禁止设备移动数据网络
 */
fun setDataConnectivityDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, isDisable)
}

/**
 * 查询是否禁用移动数据网络
 */
fun isDataConnectivityDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name) ?: false
}

/**
 * 禁用设备截屏
 */
fun setScreenShotDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_SCREEN_SHOT.name, isDisable)
}

/**
 * 是否禁用设备截屏
 */
fun isScreenShot(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_SCREEN_SHOT.name) ?: false
}

/**
 * 禁用设备录屏
 */
fun setScreenCaptureDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_SCREEN_CAPTURE.name, isDisable)
}

/**
 * 是否禁用设备录屏
 */
fun isScreenCaptureDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_SCREEN_CAPTURE.name) ?: false
}

/**
 * 禁用设备TF卡存储
 */
fun setTFCardDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_TF_CARD.name, isDisable)
}

/**
 * 查询是否禁用TF卡存储
 */
fun isTFCardDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_TF_CARD.name) ?: false
}

/**
 * 禁用拨打电话
 */
fun setCallPhoneDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_PHONE_CALL.name, isDisable)
}

/**
 * 是否禁用拨打电话
 */
fun isCallPhoneDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_PHONE_CALL.name) ?: false
}

/**
 * 禁用短信
 */
fun disableSms(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_SMS.name, isDisable)
}

/**
 * 查询是否禁用短信
 */
fun isSmsDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_SMS.name) ?: false
}

/**
 * 禁用录音功能
 */
fun setMicrophoneDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_MICROPHONE.name, isDisable)
}

/**
 * 查询录音功能禁用状态
 */
fun isMicrophoneDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_MICROPHONE.name) ?: false
}

/**
 * 禁止设备恢复出厂设备
 */
fun setRestoreFactoryDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_RESTORE_FACTORY.name, isDisable)
}

/**
 * 查询是否禁用设备恢复出厂设备
 */
fun isRestoreFactoryDisable(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_RESTORE_FACTORY.name) ?: false
}

/**
 * 禁用设备系统升级
 */
fun setSystemUpdateDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_SYSTEM_UPDATE.name, isDisable)
}

/**
 * 查询系统升级功能禁用状态
 */
fun isSystemUpdateDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_SYSTEM_UPDATE.name) ?: false
}

/**
 * 禁止安装应用
 */
fun setInstallDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_INSTALL_APP.name, isDisable)
}

/**
 * 是否禁止安装应用
 */
fun isInstallDisabled(): Boolean {
    return mService?.isDisable(ConfigEnum.DISABLE_INSTALL_APP.name) ?: false
}

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
fun activeDeviceManager(packageName: String, className: String) {
    mService?.deviceManager(packageName, className, false)
}

/**
 * 静默取消激活设备管理
 */
fun removeActiveDeviceAdmin(packageName: String, className: String) {
    mService?.deviceManager(packageName, className, true)
}

/**
 * 静默设置默认桌面
 */
fun setDefaultLauncher(packageName: String) {
    mService?.defaultLauncher(packageName, false)
}

/**
 * 静默移除默认桌面
 */
fun clearDefaultLauncher(packageName: String) {
    mService?.defaultLauncher(packageName, true)
}

/**
 * 添加禁止安装应用列表
 */
fun addForbiddenInstallApp(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        true,
        PackageTypeEnum.DISABLE_INSTALL.ordinal
    )
}

/**
 * 移除禁止安装应用列表
 */
fun removeForbiddenInstallApp(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        false,
        PackageTypeEnum.DISABLE_INSTALL.ordinal
    )
}

/**
 * 获取被禁止安装应用列表接口
 */
fun getForbiddenInstallAppList(): List<String> {
    return try {
        mService?.getPackages(PackageTypeEnum.DISABLE_INSTALL.ordinal)!!.toMutableList()
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 添加应用安装白名单
 */
fun addInstallPackageTrustList(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        true,
        PackageTypeEnum.INSTALL.ordinal
    )
}

/**
 * 移除应用安装白名单
 */
fun removeInstallPackageTrustList(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        false,
        PackageTypeEnum.INSTALL.ordinal
    )
}

/**
 * 获取应用安装白名单
 */
fun getInstallPackageTrustList(): List<String> {
    return mService?.getPackages(PackageTypeEnum.INSTALL.ordinal)?.toMutableList() ?: emptyList()
}


/**
 * 添加禁止卸载应用列表
 */
fun addDisallowedUninstallPackages(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        true,
        PackageTypeEnum.DISABLE_UNINSTALL.ordinal
    )
}

/**
 * 移除禁止卸载应用列表
 */
fun removeDisallowedUninstallPackages(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        false,
        PackageTypeEnum.DISABLE_UNINSTALL.ordinal
    )
}

/**
 * 获取被禁止卸载应用列表
 */
fun getDisallowedUninstallPackageList(): List<String> {
    return mService?.getPackages(PackageTypeEnum.DISABLE_UNINSTALL.ordinal)?.toMutableList()
        ?: emptyList()
}


/**
 * 添加系统应用保活白名单
 */
fun addPersistentApp(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        true,
        PackageTypeEnum.PERSISTENT.ordinal
    )
}

/**
 * 移除系统应用保活白名单
 */
fun removePersistentApp(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        false,
        PackageTypeEnum.PERSISTENT.ordinal
    )
}

/**
 * 获取系统应用保活白名单
 */
fun getPersistentApp(): List<String> {
    return mService?.getPackages(PackageTypeEnum.PERSISTENT.ordinal)?.toMutableList() ?: emptyList()
}


/**
 * 添加受信任应用白名单
 */
fun setSuperWhiteListForSystem(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        true,
        PackageTypeEnum.SUPER_WHITE.ordinal
    )
}

/**
 * 移除受信任应用白名单
 */
fun removeSuperWhiteListForSystem(packageNameList: List<String>) {
    mService?.packageManager(
        packageNameList.toTypedArray(),
        false,
        PackageTypeEnum.SUPER_WHITE.ordinal
    )
}

/**
 * 获取受信任应用白名单
 */
fun getSuperWhiteListForSystem(): List<String> {
    return mService?.getPackages(PackageTypeEnum.SUPER_WHITE.ordinal)
        ?.toMutableList() ?: emptyList()
}

/**
 * 获取设备信息
 */
fun getDeviceInfo(): String? {
    return mService?.deviceInfo
}