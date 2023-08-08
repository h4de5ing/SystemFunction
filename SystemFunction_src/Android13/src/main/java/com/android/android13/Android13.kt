package com.android.android13

import android.hardware.ICameraService
import android.hardware.ICameraServiceListener
import android.hardware.ISensorPrivacyListener
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.net.IEthernetManager
import android.net.IEthernetServiceListener
import android.net.IpConfiguration
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

var iEthernetManager: IEthernetManager? = null
var ethernetListener: IEthernetServiceListener1? = null
fun disableEthernet13(disable: Boolean) {
    iEthernetManager =
        IEthernetManager.Stub.asInterface(ServiceManager.getService("ethernet"))
    iEthernetManager?.setEthernetEnabled(!disable)
}

fun addEthernetListener13() {
    iEthernetManager =
        IEthernetManager.Stub.asInterface(ServiceManager.getService("ethernet"))
    ethernetListener = IEthernetServiceListener1()
    iEthernetManager?.addListener(ethernetListener)
}

fun removeEthernetListener13() {
    iEthernetManager?.removeListener(ethernetListener)
}

private var isInsert = false

class IEthernetServiceListener1 : IEthernetServiceListener.Stub() {
    private var disable = false
    private var whatEth: String? = null
    override fun onEthernetStateChanged(state: Int) {
        //当state=0时代表以太网被禁用。
        disable = (state == 0)
    }

    override fun onInterfaceStateChanged(
        iface: String?,
        state: Int,
        role: Int,
        configuration: IpConfiguration?
    ) {
        if (state == 2) {
            //state=2表示网线连接到了机器
            isInsert = true
            whatEth = iface
        }
        //当网线拔出时，且此时状态为禁用状态，则先将以太网设置为允许，在设置为不允许，防止当再次插上网线后，无法启用以太网问题。
        //当以太网拔出时state == 1, iface.equals(whatEth)作用是判断具体端口号是否是插入时记录的端口号。disable是判断有没有被禁用。
        //isInsert的作用是限制下面的代码在不连接网线作用下不断的点击禁用启用时执行
        if (state == 1 && iface.equals(whatEth) && disable && isInsert) {
            isInsert = false
            iEthernetManager?.setEthernetEnabled(true)
            iEthernetManager?.setEthernetEnabled(false)
        }
        //下面这个判断的作用是当网线拔出时，点击禁用后再启用时，不执行上面的if语句中的代码！
        if (state == 1 && iface.equals(whatEth)) isInsert = false
    }
}