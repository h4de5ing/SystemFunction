package com.android.android12

import android.app.INotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.debug.IAdbManager
import android.hardware.ISensorPrivacyManager
import android.hardware.SensorPrivacyManager
import android.net.IEthernetManager
import android.net.IEthernetServiceListener
import android.os.Build
import android.os.PowerManager
import android.os.ServiceManager
import android.service.SensorPrivacyIndividualEnabledSensorProto
import android.service.SensorPrivacyToggleSourceProto
import android.service.dreams.DreamService
import android.service.dreams.IDreamManager
import android.util.Log
import com.android.internal.widget.ILockSettings
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockscreenCredential
import java.lang.reflect.Method

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
    context: Context, oldPassword: String, isDisable: Boolean, change: (String) -> Unit
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
        LockscreenCredential.createNone(), savedCredential, 0
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

//需要selinux权限
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

/**
 * 根据ComponentName设置通知监听权限
 */
fun grantNotificationListenerAccessGranted12(serviceComponent: ComponentName) {
    val iNotificationManager = INotificationManager.Stub.asInterface(
        ServiceManager.getService(Context.NOTIFICATION_SERVICE)
    )
    iNotificationManager.setNotificationListenerAccessGrantedForUser(
        serviceComponent, 0, true, true
    )
    Log.d(
        "GrantUtils", "grantNotificationListenerAccessGranted12 ${serviceComponent.className}"
    )
}

/**
 * 屏保app可以参考
 * 万花筒 com.android.dreams.basic/com.android.dreams.basic.Colors
 * 时钟 com.android.deskclock/com.android.deskclock.Screensaver
 * com.google.android.deskclock/com.android.deskclock.Screensaver
 *     IDreamManager的所有接口
 *     void dream();//启动屏保
 *
 *     void awaken();
 *
 *     (maxTargetSdk = 30, trackingBug = 170729553)
 *     void setDreamComponents(in ComponentName[] componentNames);//设置屏保
 *
 *     (maxTargetSdk = 30, trackingBug = 170729553)
 *     ComponentName[] getDreamComponents();//获取屏保
 *
 *     ComponentName getDefaultDreamComponentForUser(int userId);//获取默认屏保
 *
 *     void testDream(int userId, in ComponentName componentName);//测试屏保
 *
 *     boolean isDreaming();//是否已经进入屏保状态
 *
 *     void finishSelf(in IBinder token, boolean immediate);//结束屏保
 *
 *     void startDozing(in IBinder token, int screenState, int screenBrightness);
 *
 *     void stopDozing(in IBinder token);
 *
 *     void forceAmbientDisplayEnabled(boolean enabled);
 *
 *     ComponentName[] getDreamComponentsForUser(int userId);
 *
 *     void setDreamComponentsForUser(int userId, in ComponentName[] componentNames);
 *
 * 是否打开屏保 Settings.Secure.SCREENSAVER_ENABLED
 * 休眠的时候是否打开屏保 Settings.Secure.SCREENSAVER_ACTIVATE_ON_SLEEP
 * 底座的时候是否打开屏保 Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK
 * 查看开关状态 settings list secure|grep screensaver
 */
/**
 * 获取所有安装的屏保app
 */
fun getDreamPackage(context: Context) {
    try {
        val pm = context.packageManager
        val dreamIntent = Intent(DreamService.SERVICE_INTERFACE)
        val resolveInfos = pm.queryIntentServices(dreamIntent, PackageManager.GET_META_DATA)
        println("getDreamPackageSize=${resolveInfos.size}")
        resolveInfos.forEach {
            println("getDreamPackage=${it.serviceInfo.packageName}/${it.serviceInfo.name}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 启动屏保
 */
fun dream() {
    try {
        val iDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"))
        if (!iDreamManager.isDreaming) iDreamManager.dream()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 通过PowerManager启动屏保
 */
fun setDefaultDreamTime(context: Context, time: Long) {
    try {
        val powerManager: PowerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val method: Method =
            powerManager.javaClass.getMethod("dream", Long::class.javaPrimitiveType)
        method.invoke(powerManager, time)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 通过ComponentName启用屏保，适合调试的调用
 */
fun testDream(componentName: ComponentName) {
    try {
        val iDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"))
        iDreamManager.testDream(0, componentName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 获取已经设置的屏保
 */
fun getDreamComponents(): ComponentName? {
    var componentName: ComponentName? = null
    try {
        val iDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"))
        val list = iDreamManager.dreamComponents
        list.forEach {
            println("获取已经安装的屏保app=${it.packageName}/${it.className}")
        }
        componentName = list.firstOrNull()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return componentName
}

fun setDreamComponents(componentName: ComponentName) {
    try {
        val iDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"))
        iDreamManager.dreamComponents = arrayOf(componentName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 获取默认屏保
 * settings get secure screensaver_default_component
 */
@Deprecated("这个方法暂时无用")
fun getDefaultDreamComponent12() {
    try {
        val iDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"))
        val componentName = iDreamManager.getDefaultDreamComponentForUser(0)
        println("默认屏保=${componentName.packageName}/${componentName.className}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}