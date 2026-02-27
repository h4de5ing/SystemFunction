package com.android.android15

import android.content.pm.IPackageManager
import android.os.ServiceManager
import android.view.accessibility.IAccessibilityManager

/**
 * Android 15 (API 35) 平台兼容性代码
 */

/**
 * Android 15 setPackagesSuspendedAsUser 参数从7个变为9个
 * 新增 int suspendingUserType, int targetUserId 参数
 */
fun setPackagesSuspendedAsUser15(packageName: String, isHidden: Boolean) {
    val ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
    ipm.setPackagesSuspendedAsUser(
        arrayOf(packageName), isHidden, null, null, null, 0, "android", 0, 0
    )
}

/**
 * Android 15 INfcAdapter.enable/disable 被移除，改用 NfcManager
 */
fun enableNFC15() {
    try {
        val nfcAdapterClass = Class.forName("android.nfc.NfcAdapter")
        val getDefaultAdapter = nfcAdapterClass.getMethod("getDefaultAdapter")
        val adapter = getDefaultAdapter.invoke(null)
        if (adapter != null) {
            val enableMethod = nfcAdapterClass.getMethod("enable")
            enableMethod.invoke(adapter)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun disableNFC15() {
    try {
        val nfcAdapterClass = Class.forName("android.nfc.NfcAdapter")
        val getDefaultAdapter = nfcAdapterClass.getMethod("getDefaultAdapter")
        val adapter = getDefaultAdapter.invoke(null)
        if (adapter != null) {
            val disableMethod = nfcAdapterClass.getMethod("disable", Boolean::class.javaPrimitiveType)
            disableMethod.invoke(adapter, true)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Android 15 getInstalledAccessibilityServiceList 返回类型变为 ParceledListSlice
 */
fun getInstalledAccessibilityServiceList15() {
    val am = IAccessibilityManager.Stub.asInterface(
        ServiceManager.getService("accessibility")
    )
    am.getInstalledAccessibilityServiceList(0)
}
