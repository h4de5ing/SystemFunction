package com.android.android12

import android.annotation.SuppressLint
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.os.Build
import android.os.ServiceManager
import android.service.SensorPrivacyIndividualEnabledSensorProto
import android.service.SensorPrivacyToggleSourceProto

/**
 * frameworks/base/services/core/java/com/android/server/SensorPrivacyService.java
 */
@SuppressLint("WrongConstant")
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
            0,
            SensorPrivacyToggleSourceProto.SETTINGS,
            sensors,
            !isDisable
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