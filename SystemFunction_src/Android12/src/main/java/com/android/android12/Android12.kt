package com.android.android12

import android.content.Context
import android.debug.IAdbManager
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.net.IEthernetManager
import android.net.IEthernetServiceListener
import android.os.Build
import android.os.ServiceManager
import android.service.SensorPrivacyIndividualEnabledSensorProto
import android.service.SensorPrivacyToggleSourceProto
import com.android.internal.widget.ILockSettings
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockscreenCredential

/**
 * frameworks/base/services/core/java/com/android/server/SensorPrivacyService.java
 */
fun disableSensor12(isDisable: Boolean, sensor: Int) {
    if (Build.VERSION.SDK_INT == 31 || Build.VERSION.SDK_INT == 32) {
        val sensors = when (sensor) {
            1 -> SensorPrivacyManager.Sensors.MICROPHONE
            2 -> SensorPrivacyManager.Sensors.CAMERA
            else -> SensorPrivacyIndividualEnabledSensorProto.UNKNOWN
        }
        val spm =
            ISensorPrivacyManager.Stub.asInterface(ServiceManager.getService("sensor_privacy")) as ISensorPrivacyManager
//    val result = spm.isIndividualSensorPrivacyEnabled(0, SensorPrivacyManager.Sensors.CAMERA)
//    println("摄像头状态:${result}")
        spm.setIndividualSensorPrivacy(
            0, SensorPrivacyToggleSourceProto.SETTINGS, sensors, !isDisable
        )
//    spm.addIndividualSensorPrivacyListener(0, SensorPrivacyManager.Sensors.CAMERA,
//        object : ISensorPrivacyListener.Stub() {
//            override fun onSensorPrivacyChanged(enabled: Boolean) {
//            }
//        }
//    )
//移除监听
//    spm.removeIndividualSensorPrivacyListener(SensorPrivacyManager.Sensors.CAMERA, null)
    }
}

var iEthernetManager: IEthernetManager? = null
var ethernetListener: IEthernetServiceListener1? = null
fun disableEthernet12(disable: Boolean) {
    try {
        iEthernetManager = IEthernetManager.Stub.asInterface(ServiceManager.getService("ethernet"))
        val methods = iEthernetManager?.javaClass?.methods?.map { it.name }
        methods?.apply {
            if (contains("Trackstop") && contains("Trackstart")) {
                if (disable) iEthernetManager?.Trackstop()
                else iEthernetManager?.Trackstart()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun addEthernetListener12(change: () -> Unit) {
    try {
        ethernetListener = IEthernetServiceListener1(change)
        iEthernetManager?.addListener(ethernetListener)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun removeEthernetListener12() {
    iEthernetManager?.removeListener(ethernetListener)
}

class IEthernetServiceListener1(val change: () -> Unit) : IEthernetServiceListener.Stub() {
    override fun onAvailabilityChanged(iface: String?, isAvailable: Boolean) {
        change()
    }
}

fun getAdbWirelessPort12(): Int {
    var port = 5555
    try {
        val adb = IAdbManager.Stub.asInterface(ServiceManager.getService("adb"))
        port = adb.adbWirelessPort
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return port
}

fun allowWirelessDebugging(alwaysAllow: Boolean, ssid: String) {
    try {
        if (Build.VERSION.SDK_INT >= 31) {
            val adb = IAdbManager.Stub.asInterface(ServiceManager.getService("adb"))
            adb.allowWirelessDebugging(alwaysAllow, ssid)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun isDisableLockScreen12(context: Context, isDisable: Boolean) {
    val utils = LockPatternUtils(context)
    utils.setLockCredential(
        LockscreenCredential.createNone(),
        LockscreenCredential.createPin("123456"),
        0
    )
    utils.setLockScreenDisabled(isDisable, 0)
}

/**
 * 注意:这个接口是Android11(sdk-30)才有的接口
 * 无 -1
 * 滑动 -1
 * 图案 1
 * PIN码 3
 * 密码 4
 */
fun getCredentialType12(): Int {
    if (Build.VERSION.SDK_INT >= 30) {
        val lock = ILockSettings.Stub.asInterface(ServiceManager.getService("lock_settings"))
        return lock.getCredentialType(0)
    } else return -1
}
