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
import java.io.File
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
 */


val MODE_ALLOWED = 0//访问者可以访问该敏感操作
val MODE_IGNORED = 1//访问者不可以访问该敏感操作，但是不会引发crash;
val MODE_ERRORED = 2//访问者不可以访问该敏感操作，会引发crash;
val MODE_DEFAULT = 3//访问者来决定访问该敏感操作的准入规则。
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
        val aomMode = iAppOpsManager.checkOperation(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63, uid, packageName)
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
            iAppOpsManager.setMode(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63, uid, packageName, MODE_ALLOWED)
            iDeviceIdleController.addPowerSaveWhitelistApp(packageName)
        }

        MODE_OPTIMIZED -> {
            iAppOpsManager.setMode(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63, uid, packageName, MODE_ALLOWED)
            iDeviceIdleController.removePowerSaveWhitelistApp(packageName)
        }
    }
    println("电池优化 packageName=${packageName},mode=${mode}")
} catch (e: Exception) {
    e.printStackTrace()
}

/**
 * Android 权限操作列表，来源于Android系统源码，可能会有遗漏，欢迎补充
Android 14 (API 34)
============================================================
int     APP_OP_ (AppProtoEnums)                                  OPSTR_ string
----------------------------------------------------------------------------------------------------
-1      APP_OP_NONE                                              ?
0       APP_OP_COARSE_LOCATION                                   android:coarse_location
1       APP_OP_FINE_LOCATION                                     android:fine_location
2       APP_OP_GPS                                               android:gps
3       APP_OP_VIBRATE                                           android:vibrate
4       APP_OP_READ_CONTACTS                                     android:read_contacts
5       APP_OP_WRITE_CONTACTS                                    android:write_contacts
6       APP_OP_READ_CALL_LOG                                     android:read_call_log
7       APP_OP_WRITE_CALL_LOG                                    android:write_call_log
8       APP_OP_READ_CALENDAR                                     android:read_calendar
9       APP_OP_WRITE_CALENDAR                                    android:write_calendar
10      APP_OP_WIFI_SCAN                                         android:wifi_scan
11      APP_OP_POST_NOTIFICATION                                 android:post_notification
12      APP_OP_NEIGHBORING_CELLS                                 android:neighboring_cells
13      APP_OP_CALL_PHONE                                        android:call_phone
14      APP_OP_READ_SMS                                          android:read_sms
15      APP_OP_WRITE_SMS                                         android:write_sms
16      APP_OP_RECEIVE_SMS                                       android:receive_sms
17      APP_OP_RECEIVE_EMERGENCY_SMS                             android:receive_emergency_broadcast
18      APP_OP_RECEIVE_MMS                                       android:receive_mms
19      APP_OP_RECEIVE_WAP_PUSH                                  android:receive_wap_push
20      APP_OP_SEND_SMS                                          android:send_sms
21      APP_OP_READ_ICC_SMS                                      android:read_icc_sms
22      APP_OP_WRITE_ICC_SMS                                     android:write_icc_sms
23      APP_OP_WRITE_SETTINGS                                    android:write_settings
24      APP_OP_SYSTEM_ALERT_WINDOW                               android:system_alert_window
25      APP_OP_ACCESS_NOTIFICATIONS                              android:access_notifications
26      APP_OP_CAMERA                                            android:camera
27      APP_OP_RECORD_AUDIO                                      android:record_audio
28      APP_OP_PLAY_AUDIO                                        android:play_audio
29      APP_OP_READ_CLIPBOARD                                    android:read_clipboard
30      APP_OP_WRITE_CLIPBOARD                                   android:write_clipboard
31      APP_OP_TAKE_MEDIA_BUTTONS                                android:take_media_buttons
32      APP_OP_TAKE_AUDIO_FOCUS                                  android:take_audio_focus
33      APP_OP_AUDIO_MASTER_VOLUME                               android:audio_master_volume
34      APP_OP_AUDIO_VOICE_VOLUME                                android:audio_voice_volume
35      APP_OP_AUDIO_RING_VOLUME                                 android:audio_ring_volume
36      APP_OP_AUDIO_MEDIA_VOLUME                                android:audio_media_volume
37      APP_OP_AUDIO_ALARM_VOLUME                                android:audio_alarm_volume
38      APP_OP_AUDIO_NOTIFICATION_VOLUME                         android:audio_notification_volume
39      APP_OP_AUDIO_BLUETOOTH_VOLUME                            android:audio_bluetooth_volume
40      APP_OP_WAKE_LOCK                                         android:wake_lock
41      APP_OP_MONITOR_LOCATION                                  android:monitor_location
42      APP_OP_MONITOR_HIGH_POWER_LOCATION                       android:monitor_location_high_power
43      APP_OP_GET_USAGE_STATS                                   android:get_usage_stats
44      APP_OP_MUTE_MICROPHONE                                   android:mute_microphone
45      APP_OP_TOAST_WINDOW                                      android:toast_window
46      APP_OP_PROJECT_MEDIA                                     android:project_media
47      APP_OP_ACTIVATE_VPN                                      android:activate_vpn
48      APP_OP_WRITE_WALLPAPER                                   android:write_wallpaper
49      APP_OP_ASSIST_STRUCTURE                                  android:assist_structure
50      APP_OP_ASSIST_SCREENSHOT                                 android:assist_screenshot
51      APP_OP_READ_PHONE_STATE                                  android:read_phone_state
52      APP_OP_ADD_VOICEMAIL                                     android:add_voicemail
53      APP_OP_USE_SIP                                           android:use_sip
54      APP_OP_PROCESS_OUTGOING_CALLS                            android:process_outgoing_calls
55      APP_OP_USE_FINGERPRINT                                   android:use_fingerprint
56      APP_OP_BODY_SENSORS                                      android:body_sensors
57      APP_OP_READ_CELL_BROADCASTS                              android:read_cell_broadcasts
58      APP_OP_MOCK_LOCATION                                     android:mock_location
59      APP_OP_READ_EXTERNAL_STORAGE                             android:read_external_storage
60      APP_OP_WRITE_EXTERNAL_STORAGE                            android:write_external_storage
61      APP_OP_TURN_SCREEN_ON                                    android:turn_screen_on
62      APP_OP_GET_ACCOUNTS                                      android:get_accounts
63      APP_OP_RUN_IN_BACKGROUND                                 android:run_in_background
64      APP_OP_AUDIO_ACCESSIBILITY_VOLUME                        android:audio_accessibility_volume
65      APP_OP_READ_PHONE_NUMBERS                                android:read_phone_numbers
66      APP_OP_REQUEST_INSTALL_PACKAGES                          android:request_install_packages
67      APP_OP_PICTURE_IN_PICTURE                                android:picture_in_picture
68      APP_OP_INSTANT_APP_START_FOREGROUND                      android:instant_app_start_foreground
69      APP_OP_ANSWER_PHONE_CALLS                                android:answer_phone_calls
70      APP_OP_RUN_ANY_IN_BACKGROUND                             android:run_any_in_background
71      APP_OP_CHANGE_WIFI_STATE                                 android:change_wifi_state
72      APP_OP_REQUEST_DELETE_PACKAGES                           android:request_delete_packages
73      APP_OP_BIND_ACCESSIBILITY_SERVICE                        android:bind_accessibility_service
74      APP_OP_ACCEPT_HANDOVER                                   android:accept_handover
75      APP_OP_MANAGE_IPSEC_TUNNELS                              android:manage_ipsec_tunnels
76      APP_OP_START_FOREGROUND                                  android:start_foreground
77      APP_OP_BLUETOOTH_SCAN                                    android:bluetooth_scan
78      APP_OP_USE_BIOMETRIC                                     android:use_biometric
79      APP_OP_ACTIVITY_RECOGNITION                              android:activity_recognition
80      APP_OP_SMS_FINANCIAL_TRANSACTIONS                        android:sms_financial_transactions
81      APP_OP_READ_MEDIA_AUDIO                                  android:read_media_audio
82      APP_OP_WRITE_MEDIA_AUDIO                                 android:write_media_audio
83      APP_OP_READ_MEDIA_VIDEO                                  android:read_media_video
84      APP_OP_WRITE_MEDIA_VIDEO                                 android:write_media_video
85      APP_OP_READ_MEDIA_IMAGES                                 android:read_media_images
86      APP_OP_WRITE_MEDIA_IMAGES                                android:write_media_images
87      APP_OP_LEGACY_STORAGE                                    android:legacy_storage
88      APP_OP_ACCESS_ACCESSIBILITY                              android:access_accessibility
89      APP_OP_READ_DEVICE_IDENTIFIERS                           android:read_device_identifiers
90      APP_OP_ACCESS_MEDIA_LOCATION                             android:access_media_location
91      APP_OP_QUERY_ALL_PACKAGES                                android:query_all_packages
92      APP_OP_MANAGE_EXTERNAL_STORAGE                           android:manage_external_storage
93      APP_OP_INTERACT_ACROSS_PROFILES                          android:interact_across_profiles
94      APP_OP_ACTIVATE_PLATFORM_VPN                             android:activate_platform_vpn
95      APP_OP_LOADER_USAGE_STATS                                android:loader_usage_stats
96      APP_OP_DEPRECATED_1                                      ?
97      APP_OP_AUTO_REVOKE_PERMISSIONS_IF_UNUSED                 android:auto_revoke_permissions_if_unused
98      APP_OP_AUTO_REVOKE_MANAGED_BY_INSTALLER                  android:auto_revoke_managed_by_installer
99      APP_OP_NO_ISOLATED_STORAGE                               android:no_isolated_storage
100     APP_OP_PHONE_CALL_MICROPHONE                             android:phone_call_microphone
101     APP_OP_PHONE_CALL_CAMERA                                 android:phone_call_camera
102     APP_OP_RECORD_AUDIO_HOTWORD                              android:record_audio_hotword
103     APP_OP_MANAGE_ONGOING_CALLS                              android:manage_ongoing_calls
104     APP_OP_MANAGE_CREDENTIALS                                android:manage_credentials
105     APP_OP_USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER               android:use_icc_auth_with_device_identifier
106     APP_OP_RECORD_AUDIO_OUTPUT                               android:record_audio_output
107     APP_OP_SCHEDULE_EXACT_ALARM                              android:schedule_exact_alarm
108     APP_OP_FINE_LOCATION_SOURCE                              android:fine_location_source
109     APP_OP_COARSE_LOCATION_SOURCE                            android:coarse_location_source
110     APP_OP_MANAGE_MEDIA                                      android:manage_media
111     APP_OP_BLUETOOTH_CONNECT                                 android:bluetooth_connect
112     APP_OP_UWB_RANGING                                       android:uwb_ranging
113     APP_OP_ACTIVITY_RECOGNITION_SOURCE                       android:activity_recognition_source
114     APP_OP_BLUETOOTH_ADVERTISE                               android:bluetooth_advertise
115     APP_OP_RECORD_INCOMING_PHONE_AUDIO                       android:record_incoming_phone_audio
116     APP_OP_NEARBY_WIFI_DEVICES                               android:nearby_wifi_devices
117     APP_OP_ESTABLISH_VPN_SERVICE                             android:establish_vpn_service
118     APP_OP_ESTABLISH_VPN_MANAGER                             android:establish_vpn_manager
119     APP_OP_ACCESS_RESTRICTED_SETTINGS                        android:access_restricted_settings
120     APP_OP_RECEIVE_AMBIENT_TRIGGER_AUDIO                     android:receive_ambient_trigger_audio
121     APP_OP_RECEIVE_EXPLICIT_USER_INTERACTION_AUDIO           android:receive_explicit_user_interaction_audio
122     APP_OP_RUN_USER_INITIATED_JOBS                           android:run_user_initiated_jobs
123     APP_OP_READ_MEDIA_VISUAL_USER_SELECTED                   android:read_media_visual_user_selected
124     APP_OP_SYSTEM_EXEMPT_FROM_SUSPENSION                     android:system_exempt_from_suspension
125     APP_OP_SYSTEM_EXEMPT_FROM_DISMISSIBLE_NOTIFICATIONS      android:system_exempt_from_dismissible_notifications
126     APP_OP_READ_WRITE_HEALTH_DATA                            android:read_write_health_data
127     APP_OP_FOREGROUND_SERVICE_SPECIAL_USE                    android:foreground_service_special_use
128     APP_OP_SYSTEM_EXEMPT_FROM_POWER_RESTRICTIONS             android:system_exempt_from_power_restrictions
129     APP_OP_SYSTEM_EXEMPT_FROM_HIBERNATION                    android:system_exempt_from_hibernation
130     APP_OP_SYSTEM_EXEMPT_FROM_ACTIVITY_BG_START_RESTRICTION  android:system_exempt_from_activity_bg_start_restriction
131     APP_OP_CAPTURE_CONSENTLESS_BUGREPORT_ON_USERDEBUG_BUILD  android:capture_consentless_bugreport_on_userdebug_build
132     APP_OP_BODY_SENSORS_WRIST_TEMPERATURE                    android:deprecated_2
133     APP_OP_USE_FULL_SCREEN_INTENT                            android:use_full_screen_intent
134     APP_OP_CAMERA_SANDBOXED                                  android:camera_sandboxed
135     APP_OP_RECORD_AUDIO_SANDBOXED                            android:record_audio_sandboxed
 */
fun dumpAppOps(): String {
    // 1. 从 AppProtoEnums 反射 APP_OP_ 常量：int值 -> 常量名
    val opIntToName = mutableMapOf<Int, String>()
    runCatching {
        Class.forName("android.app.AppProtoEnums").declaredFields
            .filter { it.name.startsWith("APP_OP_") }
            .forEach { field ->
                field.isAccessible = true
                val value = field.get(null) as? Int ?: return@forEach
                opIntToName[value] = field.name
            }
    }

    // 2. 从 AppOpsManager 反射 OPSTR_ 常量：string值 -> 常量名
    val opStrToName = mutableMapOf<String, String>()
    val opStrValues = mutableListOf<String>()
    AppOpsManager::class.java.declaredFields
        .filter { it.name.startsWith("OPSTR_") }
        .forEach { field ->
            field.isAccessible = true
            val value = field.get(null) as? String ?: return@forEach
            opStrToName[value] = field.name
            opStrValues += value
        }

    // 3. 用 AppOpsManager.strOpToOp 把 opStr 转成 int，建立 int -> opStr 映射
    val strOpToOpMethod = runCatching {
        AppOpsManager::class.java.getDeclaredMethod("strOpToOp", String::class.java)
            .also { it.isAccessible = true }
    }.getOrNull()

    val opIntToStr = mutableMapOf<Int, String>()
    opStrValues.forEach { opStr ->
        val opInt = runCatching { strOpToOpMethod?.invoke(null, opStr) as? Int }.getOrNull()
            ?: return@forEach
        opIntToStr[opInt] = opStr
    }

    // 4. 合并，按 int 值排序
    val allOpInts = (opIntToName.keys + opIntToStr.keys).toSortedSet()

    val sb = StringBuilder()
    val header = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    sb.appendLine(header)
    sb.appendLine("=".repeat(60))
    sb.appendLine("%-6s  %-55s  %s".format("int", "APP_OP_ (AppProtoEnums)", "OPSTR_ string"))
    sb.appendLine("-".repeat(100))

    allOpInts.forEach { opInt ->
        val protoName = opIntToName[opInt] ?: "?"
        val opStr = opIntToStr[opInt] ?: "?"
        val line = "%-6d  %-55s  %s".format(opInt, protoName, opStr)
        sb.appendLine(line)
        Log.d("AppOps", line)
    }

    // 5. 写入 /sdcard/ops.txt
    runCatching {
        File("/sdcard/ops.txt").writeText(sb.toString())
        sb.appendLine()
        sb.appendLine("已写入 /sdcard/ops.txt")
    }.onFailure {
        sb.appendLine()
        sb.appendLine("写入失败: ${it.message}")
    }

    return sb.toString()
}