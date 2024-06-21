package com.android.android12

import android.debug.IAdbManager
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.net.IEthernetManager
import android.net.IEthernetServiceListener
import android.os.Build
import android.os.ServiceManager
import android.service.SensorPrivacyIndividualEnabledSensorProto
import android.service.SensorPrivacyToggleSourceProto

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