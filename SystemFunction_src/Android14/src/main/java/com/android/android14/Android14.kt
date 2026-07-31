package com.android.android14

import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.os.ServiceManager
import android.permission.IPermissionManager
import com.android.android14.TurnOffScreen.SurfaceComposer

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

fun setRuntimePermission14(
    packageName: String,
    permission: String,
    granted: Boolean,
) {
    val permissionManager = IPermissionManager.Stub.asInterface(
        ServiceManager.getService("permissionmgr"),
    )
    if (granted) {
        permissionManager.grantRuntimePermission(packageName, permission, 0)
    } else {
        permissionManager.revokeRuntimePermission(
            packageName,
            permission,
            0,
            "SettingC permission manager",
        )
    }
}

/**
 * Android 14才有的接口，只息屏不锁屏
 */
fun lockScreen(mode: Int = 0) {
    if (Build.VERSION.SDK_INT >= 34) {
        try {
            TurnOffScreen.log("Start")
            SurfaceComposer.instance?.apply {
                val displayIds = physicalDisplayIds
                for (displayId in displayIds) {
                    TurnOffScreen.log("displayId: $displayId, mode: $mode")
                    setPowerMode(displayId, mode)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
