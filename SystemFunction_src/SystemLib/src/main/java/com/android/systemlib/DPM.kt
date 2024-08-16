package com.android.systemlib

import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.IDevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ProxyInfo
import android.net.Uri
import android.os.Build
import android.os.ServiceManager
import android.os.UserManager
import androidx.annotation.RequiresApi
import com.android.android12.getCredentialType12
import com.android.android12.isDisableLockScreen12
import com.android.android13.setLock
import com.android.android14.setLock14
import com.android.android14.setProfileOwner14
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy

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

/**
 * 取消激活设备管理器
 */
fun removeActiveAdmin(componentName: ComponentName) {
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .removeActiveAdmin(componentName, 0)
}

/**
 * 激活admin 后直接调用此方法
 */
fun setProfileOwner(componentName: ComponentName) {
    if (Build.VERSION.SDK_INT < 34) {
        IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
            .setProfileOwner(componentName, componentName.packageName, 0)
    } else {
        setProfileOwner14(componentName)
    }
}

fun setActiveProfileOwner(componentName: ComponentName) {
    try {
        setActiveAdmin(componentName)
        setProfileOwner(componentName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 这个方法等于 先设置 setActiveAdmin 在设置 setProfileOwner
 */
@Deprecated("不要调用这个方法，同时调用上面2个方法，效果等同")
fun setActiveProfileOwner(context: Context, componentName: ComponentName): Boolean {
    val dm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dm.setActiveProfileOwner(componentName, componentName.packageName)
}

/**
 * 获取当前设置的MDM
 */
fun getProfileOwnerAsUser(): ComponentName =
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .getProfileOwnerAsUser(0)


fun clearProfileOwner(componentName: ComponentName) {
    IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
        .clearProfileOwner(componentName)
}

/**
 * 判断此包名是否以及申请了DPM权限
 */
fun isProfileOwnerApp(context: Context, packageName: String): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isProfileOwnerApp(packageName)
}

/**
 * 打开DPM页面
 */
fun openProfileOwner(activity: Activity, componentName: ComponentName) {
    val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
    intent.putExtra(
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, componentName
    )
    if (intent.resolveActivity(activity.packageManager) != null) {
        activity.startActivityForResult(intent, 11)
    }
}

/**
 * 手动申请MDM权限
 */
fun setAdmin(activity: Activity, componentName: ComponentName) {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
    intent.putExtra(
        DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活此设备管理员后可免root停用应用"
    )
    activity.startActivityForResult(intent, 12)
}


/**
 * 判断是否激活设备管理器
 */
fun isAdminActive(context: Context, componentName: ComponentName): Boolean {
    return (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).isAdminActive(
        componentName
    )
}


/**
 * 获取系统预设的包名
frameworks\base\core\res\res\values\config.xml
<string name="config_defaultSupervisionProfileOwnerComponent" translatable="false">com.android.systemfunction/com.android.systemfunction.AdminReceiver</string>
 */
fun getProfileOwnerComponent(context: Context): String {
    return try {
        @SuppressLint("PrivateApi") val c = Class.forName("com.android.internal.R\$string")
        val obj = c.newInstance()
        val field = c.getField("config_defaultSupervisionProfileOwnerComponent")
        val id = field.getInt(obj)
        context.getString(id)
    } catch (e: Exception) {
        ""
    }
}

/**
 * 禁用摄像头
 */
fun setCameraDisabled(context: Context, componentName: ComponentName, isDisable: Boolean) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setCameraDisabled(
            componentName,
            isDisable
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 摄像头是否被禁用
 */
fun getCameraDisabled(context: Context, componentName: ComponentName): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).getCameraDisabled(
            componentName
        )
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
    isDisable: Boolean,
    change: (Boolean) -> Unit = {}
) {
    try {
        val dm =
            context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (isDisable) dm.addUserRestriction(componentName, key)
        else dm.clearUserRestriction(componentName, key)
        change(true)
    } catch (e: Exception) {
        change(false)
        e.printStackTrace()
    }
}

/**
 * 查询xx是否被禁用
 */
fun isDisableDMD(context: Context, key: String): Boolean {
    return (context.applicationContext.getSystemService(Context.USER_SERVICE) as UserManager).userRestrictions.getBoolean(
        key
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun setDelegatedScopes(
    context: Context, componentName: ComponentName, packageName: String
) {
    val dm =
        context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    dm.setDelegatedScopes(
        componentName, packageName, arrayListOf(DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL)
    )
}

/**
 * 设置冻结系统升级策略
 */
fun setSystemUpdatePolicy(
    context: Context, componentName: ComponentName, policy: SystemUpdatePolicy
) {
    try {
        if (Build.VERSION.SDK_INT >= 28) {
            val dm =
                context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dm.setSystemUpdatePolicy(componentName, policy)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
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
    (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).wipeData(
        DevicePolicyManager.WIPE_EXTERNAL_STORAGE
    )
}

/**
 * 隐藏应用
 * pm list packages 无法显示包名
 */
@Deprecated("调用不需要dpm权限的hiddenAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun hiddenAPP(
    context: Context, componentName: ComponentName, packageName: String, isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setApplicationHidden(
            componentName, packageName, isDisable
        )
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
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).isApplicationHidden(
            componentName, packageName
        )
    } catch (e: Exception) {
        false
    }
}

/**
 * 暂停应用，可以看到图标，但是不能使用
 */
@Deprecated("调用不需要dpm权限的suspendedAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
@RequiresApi(Build.VERSION_CODES.N)
fun suspendedAPP(
    context: Context, componentName: ComponentName, packageName: String, isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setPackagesSuspended(
            componentName, arrayOf(packageName), isDisable
        )
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
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).isPackageSuspended(
            componentName, packageName
        )
    } catch (e: Exception) {
        false
    }
}

/**
 * 禁止卸载应用
 */
@Deprecated("调用不需要dpm权限的disUninstallAPP方法", ReplaceWith(""), DeprecationLevel.WARNING)
fun disUninstallAPP(
    context: Context, componentName: ComponentName, packageName: String, isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setUninstallBlocked(
            componentName, packageName, isDisable
        )
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
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).isUninstallBlocked(
            componentName, packageName
        )
    } catch (e: Exception) {
        false
    }
}

/**
 * 禁止截图
 */
fun setScreenCaptureDisabled(context: Context, componentName: ComponentName, isDisable: Boolean) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setScreenCaptureDisabled(
            componentName, isDisable
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否禁止截图
 */
fun getScreenCaptureDisabled(context: Context, componentName: ComponentName): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).getScreenCaptureDisabled(
            componentName
        )
    } catch (e: Exception) {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun setStatusBarDisabled(context: Context, componentName: ComponentName, isDisable: Boolean) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setStatusBarDisabled(
            componentName, isDisable
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * kiosk模式
 */
fun kiosk(context: Context, admin: ComponentName, packages: Array<String>): Boolean {
    return try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).setLockTaskPackages(
            admin,
            packages
        )
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * 立即锁定设备
 */
fun lock(callerPackageName: String): Boolean {
    return if (Build.VERSION.SDK_INT >= 34) setLock14(callerPackageName)
    else setLock()
}

/**
 * 获取密码强度常量类
 */
fun getPasswordQualityList(context: Context): List<Pair<String, Int>> {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        return Class.forName("android.app.admin.DevicePolicyManager").fields.filter {
            it.name.startsWith(
                "PASSWORD_QUALITY_"
            )
        }.map {
            Pair(it.name, it.getInt(dpm))
        }
    } catch (e: Exception) {
        return listOf()
    }
}

fun getPasswordQuality(context: Context, admin: ComponentName): Int {
    var quality = -1
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        quality = dpm.getPasswordQuality(admin)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return quality
}

/**
 * 设置密码强度
 */
fun setPasswordQuality(context: Context, admin: ComponentName, quality: Int) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setPasswordQuality(admin, quality)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun setDisableLockScreen(
    context: Context,
    oldPassword: String,
    isDisable: Boolean,
    change: (String) -> Unit = {}
) {
    isDisableLockScreen12(context, oldPassword, isDisable, change)
}

/**
 * 设置密码最小长度
 */
fun setPasswordMinimumLength(context: Context, admin: ComponentName, length: Int) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setPasswordMinimumLength(admin, length)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 设置密码xx时间失效
 */
fun setPasswordExpirationTimeout(context: Context, admin: ComponentName, timeout: Long) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setPasswordExpirationTimeout(admin, timeout)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 设置xx 毫秒后锁屏
 */
fun setMaximumTimeToLock(context: Context, admin: ComponentName, timeMs: Long) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setMaximumTimeToLock(admin, timeMs)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getCredentialType(): Int = if (Build.VERSION.SDK_INT >= 30) getCredentialType12() else -1

/**
 * 重置锁屏密码
 */
fun resetPassword(context: Context, password: String, change: (String) -> Unit = {}) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.resetPassword(password, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
        change("success")
    } catch (e: Exception) {
        change("failed: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * @description 设置密码最大失败次数
 * 0则为取消
 */
fun setMaximumFailedPasswordsForWipe(
    admin: ComponentName,
    context: Context,
    num: Int,
    change: (String) -> Unit = {}
) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setMaximumFailedPasswordsForWipe(admin, num)
        change("success")
    } catch (e: Exception) {
        change("failed: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * @description 获取密码最大失败次数
 */
fun getMaximumFailedPasswordsForWipe(admin: ComponentName, context: Context): Int {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        return dpm.getMaximumFailedPasswordsForWipe(admin)
    } catch (e: Exception) {
        e.printStackTrace()
        return -1
    }
}

/**
 * @param context 上下文
 * @param admin DPM的包名
 * @param type 类型 @see Proxy.Type,有3个选项Proxy.Type.HTTP,Proxy.Type.SOCKS,Proxy.Type.DIRECT
 * @param host 127.0.0.1:8080
 * @param exclusionList 代理白名单,就是不走代理的域名
 *
 */
fun setGlobalProxy(
    context: Context,
    admin: ComponentName,
    type: String,
    host: String,
    exclusionList: List<String>
) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        val address = InetSocketAddress(host.split(":")[0], host.split(":")[1].toInt())
        val type1 = when (type) {
            "1" -> Proxy.Type.HTTP
            "2" -> Proxy.Type.SOCKS
            else -> Proxy.Type.DIRECT
        }
        dpm.setGlobalProxy(admin, Proxy(type1, address), exclusionList)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 设置独立于全局的代理
 */
fun setRecommendedGlobalProxy(
    context: Context,
    admin: ComponentName,
    proxyInfo: ProxyInfo
) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setRecommendedGlobalProxy(admin, proxyInfo)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/***
 * 清除代理
 */
fun clearGlobalProxy(context: Context, admin: ComponentName) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.setGlobalProxy(admin, Proxy.NO_PROXY, listOf())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 触发系统日志收集
 */
@SuppressLint("NewApi")
fun bugreport(context: Context, admin: ComponentName) {
    try {
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        dpm.requestBugreport(admin)
//        IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
//            .requestBugreport(admin)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * cert格式的证书安装
 */
fun installCer(
    context: Context,
    admin: ComponentName,
    file: String,
    error: (String) -> Unit = {},
    success: (String) -> Unit = {}
) {
    try {
        val cert = File(file).readBytes()
        val dpm =
            (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
        val result = dpm.installCaCert(admin, cert)
        if (result) success("success")
        else error("failed")
    } catch (e: Exception) {
        error("installCer failed: ${e.message}")
    }
}