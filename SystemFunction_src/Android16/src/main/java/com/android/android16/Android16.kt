package com.android.android16

import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.content.pm.IPackageManager
import android.os.Build
import android.os.ServiceManager

/**
 * Android 16 (API 36) 平台兼容性代码
 */

/**
 * Android 16 setActiveAdmin 新增第4个参数 callerPackageName
 */
fun setActiveAdmin16(componentName: ComponentName) {
    if (Build.VERSION.SDK_INT >= 36) {
        IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
            .setActiveAdmin(componentName, true, 0, componentName.packageName)
    }
}

/**
 * Android 16 setPackagesSuspendedAsUser 参数同Android15（9个参数）
 */
fun setPackagesSuspendedAsUser16(packageName: String, isHidden: Boolean) {
    val ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    ipm.setPackagesSuspendedAsUser(
        arrayOf(packageName), isHidden, null, null, null, 0, "android", 0, 0
    )
}
