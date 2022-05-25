package com.android.systemlib

import android.app.admin.DevicePolicyManager
import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.ServiceManager
import android.os.UserManager
import androidx.annotation.RequiresApi

/**
 * 需要DevicePolicyManage权限才能调用的接口
 */
/**
 * 静默激活设备管理器
 */
fun setActiveAdmin(componentName: ComponentName) {
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .setActiveAdmin(componentName, true, 0)
}

fun setProfileOwner(componentName: ComponentName) {
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .setProfileOwner(componentName, "oemconfig", 0)
}

fun setActiveProfileOwner(componentName: ComponentName) {
    try {
        setActiveAdmin(componentName)
        setProfileOwner(componentName)
    } catch (e: Exception) {
    }
}

/**
 * 这个方法等于 先设置 setActiveAdmin 在设置 setProfileOwner
 */
@Deprecated("不要调用这个方法，同时调用上面2个方法，效果等同")
fun setActiveProfileOwner(context: Context, componentName: ComponentName): Boolean {
    val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dm.setActiveProfileOwner(componentName, "oemconfig")
}


fun clearProfileOwner(componentName: ComponentName) {
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .clearProfileOwner(componentName)
}

/**
 * 静默取消激活设备管理
 */
fun removeActiveDeviceAdmin(context: Context, componentName: ComponentName) {
    (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .removeActiveAdmin(componentName)
}


/**
 * 判断是否激活设备管理器
 */
fun isAdminActive(context: Context, componentName: ComponentName): Boolean {
    return (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        .isAdminActive(componentName)
}

/**
 * 禁用摄像头
 */
fun setCameraDisabled(context: Context, componentName: ComponentName, isDisable: Boolean) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setCameraDisabled(componentName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 摄像头是否被禁用
 */
fun getCameraDisabled(context: Context, componentName: ComponentName): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .getCameraDisabled(componentName)
    } catch (e: Exception) {
        false
    }
}

/**
 * 禁用xx
 * @param context
 * @param componentName 具有dpm权限的Adminer
 * @param key {@link UserManager} 类的枚举
 * @param isDisable true 表示禁用 false 表示不禁用
 */
fun disableMDM(
    context: Context,
    componentName: ComponentName,
    key: String,
    isDisable: Boolean
) {
    try {
        val dm =
            context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (isDisable)
            dm.addUserRestriction(componentName, key)
        else
            dm.clearUserRestriction(componentName, key)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 查询xx是否被禁用
 */
fun isDisableDMD(context: Context, key: String): Boolean {
    return (context.applicationContext.getSystemService(Context.USER_SERVICE) as UserManager)
        .userRestrictions.getBoolean(key)
}

/**
 * 清除数据
 * 策略如下:
static final int WIPE_EUICC = 4;
public static final int WIPE_EXTERNAL_STORAGE = 1;
public static final int WIPE_RESET_PROTECTION_DATA = 2;
public static final int WIPE_SILENTLY = 8;
 */
fun wipeDate(context: Context) {
    val dm =
        context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    dm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
}

/**
 * 隐藏应用
 * pm list packages 无法显示包名
 */
@Deprecated("调用不需要dpm权限的hiddenAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun hiddenAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setApplicationHidden(componentName, packageName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否是隐藏应用
 */
@Deprecated("调用不需要dpm权限的isHiddenAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun isHiddenAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isApplicationHidden(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 暂停应用，可以看到图标，但是不能使用
 */
@Deprecated("调用不需要dpm权限的suspendedAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
@RequiresApi(Build.VERSION_CODES.N)
fun suspendedAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setPackagesSuspended(componentName, arrayOf(packageName), isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 应用是否被暂停
 */
@Deprecated("调用下面不需要dpm权限的isSuspendedAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
@RequiresApi(Build.VERSION_CODES.N)
fun isSuspendedAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isPackageSuspended(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 禁止卸载应用
 */
@Deprecated("调用不需要dpm权限的disUninstallAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun disUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setUninstallBlocked(componentName, packageName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否是禁止卸载
 */
@Deprecated("调用不需要dpm权限的isDisUninstallAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun isDisUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isUninstallBlocked(componentName, packageName)
    } catch (e: Exception) {
        false
    }
}

/**
 * 禁止截图
 */
fun setScreenCaptureDisabled(
    context: Context,
    componentName: ComponentName,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setScreenCaptureDisabled(componentName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否禁止截图
 */
fun getScreenCaptureDisabled(context: Context, componentName: ComponentName): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .getScreenCaptureDisabled(componentName)
    } catch (e: Exception) {
        false
    }
}