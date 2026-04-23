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
fun setMode(context: Context, code: Int, packageName: String, mode: Int = MODE_ALLOWED) {
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
        val aomMode =
            iAppOpsManager.checkOperation(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63, uid, packageName)
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
            iAppOpsManager.setMode(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63,
                uid,
                packageName,
                MODE_ALLOWED
            )
            iDeviceIdleController.addPowerSaveWhitelistApp(packageName)
        }

        MODE_OPTIMIZED -> {
            iAppOpsManager.setMode(/*APP_OP_RUN_ANY_IN_BACKGROUND*/63,
                uid,
                packageName,
                MODE_ALLOWED
            )
            iDeviceIdleController.removePowerSaveWhitelistApp(packageName)
        }
    }
    println("电池优化 packageName=${packageName},mode=${mode}")
} catch (e: Exception) {
    e.printStackTrace()
}

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