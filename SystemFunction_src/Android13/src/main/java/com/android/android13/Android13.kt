package com.android.android13

import android.hardware.ICameraService
import android.hardware.ICameraServiceListener
import android.hardware.ISensorPrivacyListener
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.os.Build
import android.os.RemoteException
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

private var isp: ISensorPrivacyManager? = null
private var iSensorPrivacyListener: ISensorPrivacyListener1? = null
fun addIndividualSensorPrivacyListener(change: (Boolean) -> Unit) {
    try {
        if (Build.VERSION.SDK_INT == 33) {
            iSensorPrivacyListener = ISensorPrivacyListener1(change)
            isp =
                ISensorPrivacyManager.Stub.asInterface(ServiceManager.getService("sensor_privacy"))
            isp?.addToggleSensorPrivacyListener(iSensorPrivacyListener)
            isp?.setToggleSensorPrivacy(
                0,
                SensorPrivacyToggleSourceProto.SETTINGS,
                SensorPrivacyManager.Sensors.CAMERA,
                false
            )
        }
    } catch (e: RemoteException) {
        e.printStackTrace()
    }
}

class ISensorPrivacyListener1(val change: (Boolean) -> Unit) : ISensorPrivacyListener.Stub() {
    override fun onSensorPrivacyChanged(p0: Int, p1: Int, enabled: Boolean) {
        if (Build.VERSION.SDK_INT == 33) {
            isp?.setToggleSensorPrivacy(
                0,
                SensorPrivacyToggleSourceProto.SETTINGS,
                SensorPrivacyManager.Sensors.CAMERA,
                false
            )
            change(enabled)
        }
    }
}


fun removeIndividualSensorPrivacyListener() {
    try {
        if (Build.VERSION.SDK_INT == 33) isp?.removeToggleSensorPrivacyListener(
            iSensorPrivacyListener
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun cameraListener(
    onStatusChanged: ((Int, String) -> Unit),
    onTorchStatusChanged: ((Int, String) -> Unit)
) {
    try {
        val cameraService =
            ICameraService.Stub.asInterface(ServiceManager.getService("media.camera"))
        cameraService.addListener(object : ICameraServiceListener.Stub() {
            override fun onStatusChanged(status: Int, cameraId: String) =
                onStatusChanged(status, cameraId)

            override fun onPhysicalCameraStatusChanged(
                status: Int,
                cameraId: String,
                physicalCameraId: String
            ) {
            }

            override fun onTorchStatusChanged(status: Int, cameraId: String) =
                onTorchStatusChanged(status, cameraId)

            override fun onTorchStrengthLevelChanged(s: String, i: Int) {}
            override fun onCameraAccessPrioritiesChanged() {}
            override fun onCameraOpened(cameraId: String, clientPackageId: String) {}
            override fun onCameraClosed(cameraId: String) {
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}