package com.android.systemlib

import android.app.AppOpsManager
import android.app.INotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.os.IDeviceIdleController
import android.os.ServiceManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.android12.grantNotificationListenerAccessGranted12
import com.android.internal.app.IAppOpsService
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Android 权限操作类
 * 代表具体的操作权限 https://android.googlesource.com/platform/frameworks/base/+/727e195ee8be4e9f2ac3f4c47c9c2bfb1e8916e9/core/proto/android/app/enums.proto
 * AppOpsManager.MODE_ALLOWED 0
 * AppOpsManager.MODE_IGNORED 1
 * AppOpsManager.MODE_ERRORED 2
 * AppOpsManager.MODE_DEFAULT 3
 * AppOpsManager.MODE_FOREGROUND 4
 * resetAllModes 重置全部权限
 * ```java
 * // AppOpsManager.java - operation ids for logging
 * enum AppOpEnum {
 *     APP_OP_NONE = -1//无
 *     APP_OP_COARSE_LOCATION = 0//粗略位置
 *     APP_OP_FINE_LOCATION = 1//精准位置
 *     APP_OP_GPS = 2//定位
 *     APP_OP_VIBRATE = 3//震动
 *     APP_OP_READ_CONTACTS = 4//读通讯录联系人
 *     APP_OP_WRITE_CONTACTS = 5//写通讯录联系人
 *     APP_OP_READ_CALL_LOG = 6//读通话记录
 *     APP_OP_WRITE_CALL_LOG = 7//写通话记录
 *     APP_OP_READ_CALENDAR = 8//读日历
 *     APP_OP_WRITE_CALENDAR = 9//写日历
 *     APP_OP_WIFI_SCAN = 10//扫描WiFi
 *     APP_OP_POST_NOTIFICATION = 11//通知
 *     APP_OP_NEIGHBORING_CELLS = 12//手机网络扫描
 *     APP_OP_CALL_PHONE = 13//打电话
 *     APP_OP_READ_SMS = 14//读短信
 *     APP_OP_WRITE_SMS = 15//写短信
 *     APP_OP_RECEIVE_SMS = 16//收短信
 *     APP_OP_RECEIVE_EMERGENCY_SMS = 17//收紧急短信
 *     APP_OP_RECEIVE_MMS = 18//收彩信
 *     APP_OP_RECEIVE_WAP_PUSH = 19//收wap push消息(广告)
 *     APP_OP_SEND_SMS = 20//发短信
 *     APP_OP_READ_ICC_SMS = 21//读取icc短信，不知道是啥
 *     APP_OP_WRITE_ICC_SMS = 22//写入icc短信
 *     APP_OP_WRITE_SETTINGS = 23//修改系统设置(网络音量亮度等)
 *     APP_OP_SYSTEM_ALERT_WINDOW = 24//显示在其他应用的上层(悬浮窗)
 *     APP_OP_ACCESS_NOTIFICATIONS = 25访问通知(查询，修改)
 *     APP_OP_CAMERA = 26//相机
 *     APP_OP_RECORD_AUDIO = 27//录音
 *     APP_OP_PLAY_AUDIO = 28//播放音频
 *     APP_OP_READ_CLIPBOARD = 29//读取剪切板
 *     APP_OP_WRITE_CLIPBOARD = 30//写入剪切板
 *     APP_OP_TAKE_MEDIA_BUTTONS = 31//媒体按钮
 *     APP_OP_TAKE_AUDIO_FOCUS = 32//音频焦点
 *     APP_OP_AUDIO_MASTER_VOLUME = 33//主音量
 *     APP_OP_AUDIO_VOICE_VOLUME = 34//语音音量
 *     APP_OP_AUDIO_RING_VOLUME = 35//铃声音量
 *     APP_OP_AUDIO_MEDIA_VOLUME = 36//媒体音量
 *     APP_OP_AUDIO_ALARM_VOLUME = 37//闹钟音量
 *     APP_OP_AUDIO_NOTIFICATION_VOLUME = 38//通知音量
 *     APP_OP_AUDIO_BLUETOOTH_VOLUME = 39//蓝牙音量
 *     APP_OP_WAKE_LOCK = 40//保持唤醒
 *     APP_OP_MONITOR_LOCATION = 41//监测位置
 *     APP_OP_MONITOR_HIGH_POWER_LOCATION = 42//监控高电耗位置信息服务
 *     APP_OP_GET_USAGE_STATS = 43//使用情况统计
 *     APP_OP_MUTE_MICROPHONE = 44//麦克风操作
 *     APP_OP_TOAST_WINDOW = 45//吐司Toast
 *     APP_OP_PROJECT_MEDIA = 46//投影媒体
 *     APP_OP_ACTIVATE_VPN = 47//激活vpn
 *     APP_OP_WRITE_WALLPAPER = 48//写入壁纸
 *     APP_OP_ASSIST_STRUCTURE = 49//辅助结构
 *     APP_OP_ASSIST_SCREENSHOT = 50//辅助屏幕截图
 *     APP_OP_READ_PHONE_STATE = 51//读取手机状态(号码，运营商，设备状态等)
 *     APP_OP_ADD_VOICEMAIL = 52//添加语音邮件
 *     APP_OP_USE_SIP = 53//使用sip
 *     APP_OP_PROCESS_OUTGOING_CALLS = 54//处理拨出电话
 *     APP_OP_USE_FINGERPRINT = 55//指纹
 *     APP_OP_BODY_SENSORS = 56//身体传感器
 *     APP_OP_READ_CELL_BROADCASTS = 57//读取小区广播
 *     APP_OP_MOCK_LOCATION = 58//模拟位置
 *     APP_OP_READ_EXTERNAL_STORAGE = 59//读取外部存储，一般是sdcard
 *     APP_OP_WRITE_EXTERNAL_STORAGE = 60//写入外部存储
 *     APP_OP_TURN_SCREEN_ON = 61//点亮屏幕
 *     APP_OP_GET_ACCOUNTS = 62//获取账号，如华米OV之类账号
 *     APP_OP_RUN_IN_BACKGROUND = 63//在后台运行
 *     APP_OP_AUDIO_ACCESSIBILITY_VOLUME = 64//无障碍功能音量
 *     APP_OP_READ_PHONE_NUMBERS = 65//读取手机号码
 *     APP_OP_REQUEST_INSTALL_PACKAGES = 66//请求安装应用
 *     APP_OP_PICTURE_IN_PICTURE = 67//进入画中画
 *     APP_OP_INSTANT_APP_START_FOREGROUND = 68
 *     APP_OP_ANSWER_PHONE_CALLS = 69//接听电话
 *     APP_OP_RUN_ANY_IN_BACKGROUND = 70//后台限制问题
 *     APP_OP_CHANGE_WIFI_STATE = 71//更改WiFi状态
 *     APP_OP_REQUEST_DELETE_PACKAGES = 72//请求删除应用
 *     APP_OP_BIND_ACCESSIBILITY_SERVICE = 73//使用无障碍服务
 *     APP_OP_ACCEPT_HANDOVER = 74//继续进行来自其他应用的通话
 *     APP_OP_MANAGE_IPSEC_TUNNELS = 75//管理ip隧道什么的
 *     APP_OP_START_FOREGROUND = 76//运行前台服务(通知Service，防被杀)
 *     APP_OP_BLUETOOTH_SCAN = 77//蓝牙扫描
 *     APP_OP_USE_BIOMETRIC = 78//生物识别硬件
 *     APP_OP_ACTIVITY_RECOGNITION = 79//识别身体活动，可能是计步器
 *     APP_OP_SMS_FINANCIAL_TRANSACTIONS = 80//付费短信权限
 *     APP_OP_READ_MEDIA_AUDIO = 81//存储的细分，读取音频
 *     APP_OP_WRITE_MEDIA_AUDIO = 82//存储的细分，写入音频
 *     APP_OP_READ_MEDIA_VIDEO = 83//读取视频
 *     APP_OP_WRITE_MEDIA_VIDEO = 84//写入视频
 *     APP_OP_READ_MEDIA_IMAGES = 85//读取图片
 *     APP_OP_WRITE_MEDIA_IMAGES = 86//写入图片
 *     APP_OP_LEGACY_STORAGE = 87//旧有存储，很少见
 *     APP_OP_ACCESS_ACCESSIBILITY = 88//无障碍
 *     APP_OP_READ_DEVICE_IDENTIFIERS = 89//读取设备识别码
 *     APP_OP_ACCESS_MEDIA_LOCATION = 90//从媒体文件读取位置(访问共享空间媒体文件)
 *     APP_OP_QUERY_ALL_PACKAGES = 91//获取应用列表
 *     APP_OP_MANAGE_EXTERNAL_STORAGE = 92//管理所有文件
 *     APP_OP_INTERACT_ACROSS_PROFILES = 93//必须固件签名密钥
 *     APP_OP_ACTIVATE_PLATFORM_VPN = 94//与vpn相关
 *     APP_OP_LOADER_USAGE_STATS = 95//允许数据加载器读取包的访问日志
 *     APP_OP_DEPRECATED_1 = 96 [deprecated = true]
 *     APP_OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = 97//未使用权限移除
 *     APP_OP_AUTO_REVOKE_MANAGED_BY_INSTALLER = 98//管理安装的权限
 *     APP_OP_NO_ISOLATED_STORAGE = 99//数据虚拟文件系统隔离
 * }
 * ```
 */

const val OPSTR_ADD_VOICEMAIL = "android:add_voicemail"
const val OPSTR_ANSWER_PHONE_CALLS = "android:answer_phone_calls"
const val OPSTR_BODY_SENSORS = "android:body_sensors"
const val OPSTR_CALL_PHONE = "android:call_phone"
const val OPSTR_CAMERA = "android:camera"
const val OPSTR_COARSE_LOCATION = "android:coarse_location"
const val OPSTR_FINE_LOCATION = "android:fine_location"
const val OPSTR_GET_USAGE_STATS = "android:get_usage_stats"
const val OPSTR_MOCK_LOCATION = "android:mock_location"
const val OPSTR_MONITOR_HIGH_POWER_LOCATION = "android:monitor_location_high_power"
const val OPSTR_MONITOR_LOCATION = "android:monitor_location"
const val OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture"
const val OPSTR_PROCESS_OUTGOING_CALLS = "android:process_outgoing_calls"
const val OPSTR_READ_CALENDAR = "android:read_calendar"
const val OPSTR_READ_CALL_LOG = "android:read_call_log"
const val OPSTR_READ_CELL_BROADCASTS = "android:read_cell_broadcasts"
const val OPSTR_READ_CONTACTS = "android:read_contacts"
const val OPSTR_READ_EXTERNAL_STORAGE = "android:read_external_storage"
const val OPSTR_READ_PHONE_NUMBERS = "android:read_phone_numbers"
const val OPSTR_READ_PHONE_STATE = "android:read_phone_state"
const val OPSTR_READ_SMS = "android:read_sms"
const val OPSTR_RECEIVE_MMS = "android:receive_mms"
const val OPSTR_RECEIVE_SMS = "android:receive_sms"
const val OPSTR_RECEIVE_WAP_PUSH = "android:receive_wap_push"
const val OPSTR_RECORD_AUDIO = "android:record_audio"
const val OPSTR_SEND_SMS = "android:send_sms"
const val OPSTR_SYSTEM_ALERT_WINDOW = "android:system_alert_window"
const val OPSTR_USE_FINGERPRINT = "android:use_fingerprint"
const val OPSTR_USE_SIP = "android:use_sip"
const val OPSTR_WRITE_CALENDAR = "android:write_calendar"
const val OPSTR_WRITE_CALL_LOG = "android:write_call_log"
const val OPSTR_WRITE_CONTACTS = "android:write_contacts"
const val OPSTR_WRITE_EXTERNAL_STORAGE = "android:write_external_storage"
const val OPSTR_WRITE_SETTINGS = "android:write_settings"

/**hide*/
const val OPSTR_READ_MEDIA_AUDIO = "android:read_media_audio"
const val OPSTR_READ_MEDIA_VIDEO = "android:read_media_video"
const val OPSTR_READ_MEDIA_IMAGES = "android:read_media_images"
const val OPSTR_ACTIVITY_RECOGNITION = "android:activity_recognition_source"
const val OPSTR_RUN_IN_BACKGROUND = "android:run_in_background"


const val APP_OP_NONE = -1//无
const val APP_OP_COARSE_LOCATION = 0//粗略位置
const val APP_OP_FINE_LOCATION = 1//精准位置
const val APP_OP_GPS = 2//定位
const val APP_OP_VIBRATE = 3//震动
const val APP_OP_READ_CONTACTS = 4//读通讯录联系人
const val APP_OP_WRITE_CONTACTS = 5//写通讯录联系人
const val APP_OP_READ_CALL_LOG = 6//读通话记录
const val APP_OP_WRITE_CALL_LOG = 7//写通话记录
const val APP_OP_READ_CALENDAR = 8//读日历
const val APP_OP_WRITE_CALENDAR = 9//写日历
const val APP_OP_WIFI_SCAN = 10//扫描WiFi
const val APP_OP_POST_NOTIFICATION = 11//通知
const val APP_OP_NEIGHBORING_CELLS = 12//手机网络扫描
const val APP_OP_CALL_PHONE = 13//打电话
const val APP_OP_READ_SMS = 14//读短信
const val APP_OP_WRITE_SMS = 15//写短信
const val APP_OP_RECEIVE_SMS = 16//收短信
const val APP_OP_RECEIVE_EMERGENCY_SMS = 17//收紧急短信
const val APP_OP_RECEIVE_MMS = 18//收彩信
const val APP_OP_RECEIVE_WAP_PUSH = 19//收wap push消息(广告)
const val APP_OP_SEND_SMS = 20//发短信
const val APP_OP_READ_ICC_SMS = 21//读取icc短信
const val APP_OP_WRITE_ICC_SMS = 22//写入icc短信
const val APP_OP_WRITE_SETTINGS = 23//修改系统设置(网络音量亮度等)
const val APP_OP_SYSTEM_ALERT_WINDOW = 24//显示在其他应用的上层(悬浮窗)
const val APP_OP_ACCESS_NOTIFICATIONS = 25//访问通知(查询，修改)
const val APP_OP_CAMERA = 26//相机
const val APP_OP_RECORD_AUDIO = 27//录音
const val APP_OP_PLAY_AUDIO = 28//播放音频
const val APP_OP_READ_CLIPBOARD = 29//读取剪切板
const val APP_OP_WRITE_CLIPBOARD = 30//写入剪切板
const val APP_OP_TAKE_MEDIA_BUTTONS = 31//媒体按钮
const val APP_OP_TAKE_AUDIO_FOCUS = 32//音频焦点
const val APP_OP_AUDIO_MASTER_VOLUME = 33//主音量
const val APP_OP_AUDIO_VOICE_VOLUME = 34//语音音量
const val APP_OP_AUDIO_RING_VOLUME = 35//铃声音量
const val APP_OP_AUDIO_MEDIA_VOLUME = 36//媒体音量
const val APP_OP_AUDIO_ALARM_VOLUME = 37//闹钟音量
const val APP_OP_AUDIO_NOTIFICATION_VOLUME = 38//通知音量
const val APP_OP_AUDIO_BLUETOOTH_VOLUME = 39//蓝牙音量
const val APP_OP_WAKE_LOCK = 40//保持唤醒
const val APP_OP_MONITOR_LOCATION = 41//监测位置
const val APP_OP_MONITOR_HIGH_POWER_LOCATION = 42//监控高电耗位置信息服务
const val APP_OP_GET_USAGE_STATS = 43//使用情况统计
const val APP_OP_MUTE_MICROPHONE = 44//麦克风操作
const val APP_OP_TOAST_WINDOW = 45//吐司Toast
const val APP_OP_PROJECT_MEDIA = 46//投影媒体
const val APP_OP_ACTIVATE_VPN = 47//激活vpn
const val APP_OP_WRITE_WALLPAPER = 48//写入壁纸
const val APP_OP_ASSIST_STRUCTURE = 49//辅助结构
const val APP_OP_ASSIST_SCREENSHOT = 50//辅助屏幕截图
const val APP_OP_READ_PHONE_STATE = 51//读取手机状态(号码，运营商，设备状态等)
const val APP_OP_ADD_VOICEMAIL = 52//添加语音邮件
const val APP_OP_USE_SIP = 53//使用sip
const val APP_OP_PROCESS_OUTGOING_CALLS = 54//处理拨出电话
const val APP_OP_USE_FINGERPRINT = 55//指纹
const val APP_OP_BODY_SENSORS = 56//身体传感器
const val APP_OP_READ_CELL_BROADCASTS = 57//读取小区广播
const val APP_OP_MOCK_LOCATION = 58//模拟位置
const val APP_OP_READ_EXTERNAL_STORAGE = 59//读取外部存储
const val APP_OP_WRITE_EXTERNAL_STORAGE = 60//写入外部存储
const val APP_OP_TURN_SCREEN_ON = 61//点亮屏幕
const val APP_OP_GET_ACCOUNTS = 62//获取账号，如google账号之类账号
const val APP_OP_RUN_IN_BACKGROUND = 63//在后台运行
const val APP_OP_AUDIO_ACCESSIBILITY_VOLUME = 64//无障碍功能音量
const val APP_OP_READ_PHONE_NUMBERS = 65//读取手机号码
const val APP_OP_REQUEST_INSTALL_PACKAGES = 66//请求安装应用
const val APP_OP_PICTURE_IN_PICTURE = 67//进入画中画
const val APP_OP_INSTANT_APP_START_FOREGROUND = 68
const val APP_OP_ANSWER_PHONE_CALLS = 69//接听电话
const val APP_OP_RUN_ANY_IN_BACKGROUND = 70//后台限制问题
const val APP_OP_CHANGE_WIFI_STATE = 71//更改WiFi状态
const val APP_OP_REQUEST_DELETE_PACKAGES = 72//请求删除应用
const val APP_OP_BIND_ACCESSIBILITY_SERVICE = 73//使用无障碍服务
const val APP_OP_ACCEPT_HANDOVER = 74//继续进行来自其他应用的通话
const val APP_OP_MANAGE_IPSEC_TUNNELS = 75//管理ip隧道什么的
const val APP_OP_START_FOREGROUND = 76//运行前台服务(通知Service，防被杀)
const val APP_OP_BLUETOOTH_SCAN = 77//蓝牙扫描
const val APP_OP_USE_BIOMETRIC = 78//生物识别硬件
const val APP_OP_ACTIVITY_RECOGNITION = 79//识别身体活动，可能是计步器
const val APP_OP_SMS_FINANCIAL_TRANSACTIONS = 80//付费短信权限
const val APP_OP_READ_MEDIA_AUDIO = 81//存储的细分，读取音频
const val APP_OP_WRITE_MEDIA_AUDIO = 82//存储的细分，写入音频
const val APP_OP_READ_MEDIA_VIDEO = 83//读取视频
const val APP_OP_WRITE_MEDIA_VIDEO = 84//写入视频
const val APP_OP_READ_MEDIA_IMAGES = 85//读取图片
const val APP_OP_WRITE_MEDIA_IMAGES = 86//写入图片
const val APP_OP_LEGACY_STORAGE = 87//旧有存储，很少见
const val APP_OP_ACCESS_ACCESSIBILITY = 88//无障碍
const val APP_OP_READ_DEVICE_IDENTIFIERS = 89//读取设备识别码
const val APP_OP_ACCESS_MEDIA_LOCATION = 90//从媒体文件读取位置(访问共享空间媒体文件)
const val APP_OP_QUERY_ALL_PACKAGES = 91//获取应用列表
const val APP_OP_MANAGE_EXTERNAL_STORAGE = 92//管理所有文件
const val APP_OP_INTERACT_ACROSS_PROFILES = 93//必须固件签名密钥
const val APP_OP_ACTIVATE_PLATFORM_VPN = 94//与vpn相关
const val APP_OP_LOADER_USAGE_STATS = 95//允许数据加载器读取包的访问日志

//const val   APP_OP_DEPRECATED_1 = 96 [deprecated = true]
const val APP_OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED = 97//未使用权限移除
const val APP_OP_AUTO_REVOKE_MANAGED_BY_INSTALLER = 98//管理安装的权限
const val APP_OP_NO_ISOLATED_STORAGE = 99//数据虚拟文件系统隔离

val MODE_ALLOWED = 0
val MODE_IGNORED = 1
val MODE_ERRORED = 2
val MODE_DEFAULT = 3
val MODE_FOREGROUND = 4
fun setMode(context: Context, code: Int, packageName: String, mode: Int) {
    try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val uid = applicationInfo.uid
        val iAppOpsManager =
            IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE))
        iAppOpsManager.setMode(code, uid, packageName, mode)
        iAppOpsManager.setUidMode(code, uid, mode)
        println("配置权限,uid=${uid},code=${code},packageName=${packageName},mode=${mode}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 检查setMode的授权结果
 */
fun checkUsageStatsViaReflection(
    context: Context, opCode: Int, targetPackageName: String
): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return try {
        val method: Method = appOps.javaClass.getMethod(
            "checkOp",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java
        )
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(targetPackageName, 0)
        val uid = applicationInfo.uid
        val result = method.invoke(appOps, opCode, uid, targetPackageName) as Int
        result == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        Log.e("UsageStatsChecker", "Reflection method failed", e)
        false
    }
}

/**
 * 根据包名设置通知监听权限
 */
fun grantNotificationListenerAccessGranted(context: Context, packageName: String) {
    findNotificationListenerServices(context, packageName).forEach {
        val serviceComponent = ComponentName(packageName, it)
        if (Build.VERSION.SDK_INT <= 30) {
            grantNotificationListenerAccessGranted10(serviceComponent)
        } else {
            grantNotificationListenerAccessGranted12(serviceComponent)
        }
    }
}

/**
 * 适配Android10的通知权限
 */
fun grantNotificationListenerAccessGranted10(serviceComponent: ComponentName) {
    val iNotificationManager = INotificationManager.Stub.asInterface(
        ServiceManager.getService(Context.NOTIFICATION_SERVICE)
    )
    iNotificationManager.setNotificationListenerAccessGrantedForUser(
        serviceComponent, 0, true
    )
    Log.d(
        "GrantUtils", "grantNotificationListenerAccessGranted10 ${serviceComponent.className}"
    )
}

/**
 * 搜索通知监听服务的列表
 */
fun findNotificationListenerServices(context: Context, packageName: String): List<String> {
    val servicesWithPermission = mutableListOf<String>()
    try {
        val packageInfo = context.packageManager.getPackageInfo(
            packageName, PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS
        )
        if (packageInfo.services != null) {
            for (service in packageInfo.services) {
                if (service.permission == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE") {
                    servicesWithPermission.add(service.name)
                    Log.d("ServiceFinder", "找到通知监听服务: ${service.name}")
                }
            }
        }
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("ServiceFinder", "找不到包: $packageName", e)
    } catch (e: Exception) {
        Log.e("ServiceFinder", "获取服务信息时出错", e)
    }
    return servicesWithPermission
}

/**
 * 打印本设备支持的op code
 */
fun getOpCode() {
    try {
        val appOpsClass = Class.forName("android.app.AppOpsManager")
        val fields: Array<Field> = appOpsClass.declaredFields
        val opFields = mutableListOf<Field>()
        // 过滤出公共静态整型字段，并且字段名以 "OP_" 开头
        for (field in fields) {
            if (field.name.startsWith("OP_") && java.lang.reflect.Modifier.isStatic(field.modifiers) && java.lang.reflect.Modifier.isPublic(
                    field.modifiers
                ) && field.type == Int::class.javaPrimitiveType
            ) {
                opFields.add(field)
            }
        }
        opFields.sortBy { it.name }
        Log.d("OpCodeDumper", "===== AppOpsManager OP Codes =====")
        for (field in opFields) {
            val opName = field.name
            val opValue = field.getInt(null) // 获取静态字段的值
            Log.d("OpCodeDumper", "$opName = $opValue")
        }
        Log.d("OpCodeDumper", "===== Total: ${opFields.size} OP Codes =====")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


private fun isRuntimePermission(pm: PackageManager, permission: String): Boolean {
    return try {
        val info = pm.getPermissionInfo(permission, 0)
        info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

/**
 * 给具体的某个权限授权,适用于低版本的Android，比如Android12以下
 * Android 11及以上用MANAGE_EXTERNAL_STORAGE用ops 92
 */
fun grantPermission(context: Context, packageName: String, permission: String) {
    val iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    val pm: PackageManager = context.packageManager
    try {
        if (isRuntimePermission(pm, permission)) {
            iPackageManager.grantRuntimePermission(packageName, permission, 0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 一键给所有权限授权
 */
fun grantPermission(context: Context, packageName: String) {
    val iPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    val pm: PackageManager = context.packageManager
    pm.getPackageInfo(
        packageName,
        PackageManager.GET_PERMISSIONS
    ).requestedPermissions?.forEach { permission ->
        try {
            if (isRuntimePermission(pm, permission)) {
                iPackageManager.grantRuntimePermission(packageName, permission, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private val MODE_UNKNOWN = 0
val MODE_UNRESTRICTED = 1//无限制
val MODE_OPTIMIZED = 2//优化
val MODE_RESTRICTED = 3//受限

/**
 * 获取应用电池优化状态
 */
fun getBatteryOptimization(context: Context, packageName: String): Int {
    var mode: Int = MODE_UNKNOWN
    try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val uid = applicationInfo.uid
        val iDeviceIdleController =
            (IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle")) as IDeviceIdleController)
        val iAppOpsManager =
            IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE))
        val allowListed = iDeviceIdleController.isPowerSaveWhitelistApp(packageName)
        val aomMode = iAppOpsManager.checkOperation(APP_OP_RUN_ANY_IN_BACKGROUND, uid, packageName)
        mode = if (aomMode == MODE_IGNORED && !allowListed) MODE_RESTRICTED
        else if (aomMode == MODE_ALLOWED) if (allowListed) MODE_UNRESTRICTED else MODE_OPTIMIZED
        else MODE_UNKNOWN
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return mode
}
//无限制 1 MODE_RESTRICTED = AppOpsManager.MODE_IGNORED + !allowListed
//优化 2 MODE_UNRESTRICTED = AppOpsManager.MODE_ALLOWED + allowListed
//受限 3 MODE_OPTIMIZED = AppOpsManager.MODE_ALLOWED + !allowListed
/**
 * 设置应用电池优化状态
 */
@RequiresApi(Build.VERSION_CODES.M)
fun setBatteryOptimization(context: Context, packageName: String, mode: Int) = try {
    val iDeviceIdleController =
        (IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle")) as IDeviceIdleController)
    val iAppOpsManager =
        IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE))
    val packageManager = context.packageManager
    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
    val uid = applicationInfo.uid
    when (mode) {
        MODE_RESTRICTED -> iDeviceIdleController.removePowerSaveWhitelistApp(packageName)
        MODE_UNRESTRICTED -> {
            iAppOpsManager.setMode(APP_OP_RUN_ANY_IN_BACKGROUND, uid, packageName, MODE_ALLOWED)
            iDeviceIdleController.addPowerSaveWhitelistApp(packageName)
        }

        MODE_OPTIMIZED -> {
            iAppOpsManager.setMode(APP_OP_RUN_ANY_IN_BACKGROUND, uid, packageName, MODE_ALLOWED)
            iDeviceIdleController.removePowerSaveWhitelistApp(packageName)
        }
    }
    println("电池优化 packageName=${packageName},mode=${mode}")
} catch (e: Exception) {
    e.printStackTrace()
}