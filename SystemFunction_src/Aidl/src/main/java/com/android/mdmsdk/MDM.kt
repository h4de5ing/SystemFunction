package com.android.mdmsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

//第三方系统调用

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

fun unbind(context: Context) {
    context.unbindService(conn)
}

fun isBind(): Boolean {
    return mService != null
}

fun setHomeKeyDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_HOME.name, isDisable)
}

fun isHomeKeyDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_HOME.name)
}

fun setRecentKeyDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_RECENT.name, isDisable)
}

fun isRecentKeyDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_RECENT.name)
}

fun setBackKeyDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_BACK.name, isDisable)
}

fun isBackKeyDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_BACK.name)
}

fun setNavigaBarDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_NAVIGATION.name, isDisable)
}

fun isNavigaBarDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_NAVIGATION.name)
}

fun setStatusBarDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_STATUS.name, isDisable)
}

fun isStatusBarDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_STATUS.name)
}

fun setBluetoothDisable(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_BLUETOOTH.name, isDisable)
}

fun isBluetoothDisabled(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_BLUETOOTH.name)
}

fun setHotSpotDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_HOT_SPOT.name, isDisable)
}

fun isHotSpotDisabled(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_HOT_SPOT.name)
}

fun setWifiDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_WIFI.name, isDisable)
}

fun isWifiDisabled(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_WIFI.name)
}

fun setGPSDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_GPS.name, isDisable)
}

fun isGPSDisable(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_GPS.name)
}

fun setDataConnectivityDisabled(isDisable: Boolean) {
    mService?.setDisable(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, isDisable)
}

fun isDataConnectivityDisabled(): Boolean {
    return mService!!.isDisable(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name)
}