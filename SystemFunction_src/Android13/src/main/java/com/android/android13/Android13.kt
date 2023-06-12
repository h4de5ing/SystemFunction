package com.android.android13

import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.os.Build
import android.os.ServiceManager
import android.service.SensorPrivacyIndividualEnabledSensorProto
import android.service.SensorPrivacyToggleSourceProto

fun disableSensor13(isDisable: Boolean, sensor: Int) {
    if (Build.VERSION.SDK_INT == 33) {
        val sensors = when (sensor) {
            1 -> SensorPrivacyManager.Sensors.MICROPHONE
            2 -> SensorPrivacyManager.Sensors.CAMERA
            else -> SensorPrivacyIndividualEnabledSensorProto.UNKNOWN
        }
        val spm =
            ISensorPrivacyManager.Stub.asInterface(ServiceManager.getService("sensor_privacy")) as ISensorPrivacyManager
        spm.setToggleSensorPrivacy(0, SensorPrivacyToggleSourceProto.SETTINGS, sensors, !isDisable)
    }
}