package com.android.android14

import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.os.ServiceManager
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

fun lockScreen(mode: Int = 0){
    if (Build.VERSION.SDK_INT >= 34) {
        try {
            TurnOffScreen.log("Start")
            val surfaceComposer = SurfaceComposer.getInstance()
            val displayIds = surfaceComposer.getPhysicalDisplayIds()
            for (displayId in displayIds) {
                TurnOffScreen.log("displayId: $displayId, mode: $mode")
                surfaceComposer.setPowerMode(displayId, mode)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}