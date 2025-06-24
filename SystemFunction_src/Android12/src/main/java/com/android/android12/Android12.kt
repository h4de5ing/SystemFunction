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
fun disableEthernet12(disable: Boolean, isSupported: (Boolean) -> Unit = {}) {
    try {
        iEthernetManager = IEthernetManager.Stub.asInterface(ServiceManager.getService("ethernet"))
        val methods = iEthernetManager?.javaClass?.methods?.map { it.name }
        methods?.apply {
            if (contains("Trackstop") && contains("Trackstart")) {
                isSupported(true)
                if (disable) iEthernetManager?.Trackstop()
                else iEthernetManager?.Trackstart()
            } else isSupported(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun addEthernetListener12(change: ((String, Boolean) -> Unit)) {
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

class IEthernetServiceListener1(val change: ((String, Boolean) -> Unit)) :
    IEthernetServiceListener.Stub() {
    override fun onAvailabilityChanged(iface: String, isAvailable: Boolean) {
        change(iface, isAvailable)
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

fun isDisableLockScreen12(
    context: Context,
    oldPassword: String,
    isDisable: Boolean,
    change: (String) -> Unit
) {
    val utils = LockPatternUtils(context)
    val type = getCredentialType12()
    if (type != 3 && type != 4) {
        change("failed： Only PIN code and password can be modified")
        return
    }
    val savedCredential = if (type == 3) LockscreenCredential.createPin(oldPassword)
    else LockscreenCredential.createPassword(oldPassword)
    val checkResult = utils.setLockCredential(
        LockscreenCredential.createNone(),
        savedCredential,
        0
    )
    if (!checkResult) {
        change("failed: Old password verification failed")
        return
    }
    utils.setLockScreenDisabled(isDisable, 0)
    change("success")
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

//TODO 需要selinux权限
fun startTcp5555() {
//    android.os.SystemProperties.set("persist.adb.tls_server.enable", "1")
//    android.os.SystemProperties.set("ro.adb.secure", "0")
//    android.os.SystemProperties.set("service.adb.tcp.port", "5555")
//    android.os.SystemProperties.set("service.adb.tls.port", "5555")
}

//打开adbd服务
fun startAdbd() {
    android.os.SystemProperties.set("ctl.start", "adbd")
}

//关闭adbd服务
fun stopAdbd() {
    android.os.SystemProperties.set("ctl.stop", "adbd")
}