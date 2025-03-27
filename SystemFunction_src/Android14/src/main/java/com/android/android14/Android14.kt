package com.android.android14

import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.os.ServiceManager

fun setProfileOwner14(componentName: ComponentName) {
    if (Build.VERSION.SDK_INT >= 34) {
        IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
            .setProfileOwner(componentName, 0)
    }
}


fun setLock14(callerPackageName: String): Boolean {
    return try {
        IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"))
            .lockNow(0, callerPackageName, false)
        true
    } catch (_: Exception) {
        false
    }
}