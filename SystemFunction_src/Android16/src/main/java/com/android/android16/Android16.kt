package com.android.android16

import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
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
