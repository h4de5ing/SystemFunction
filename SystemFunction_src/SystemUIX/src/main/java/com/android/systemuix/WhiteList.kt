package com.android.systemuix

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.TextView

val whiteList = listOf(
    "com.android.kiosk",
    "com.android.systemfunction",
    "com.android.settingc",
    "com.android.buttonassignment",
    "com.android.settings",
    "com.scan.scan4710",
    "com.scanner.hardware",
    "com.scan.scan6703",
    "com.android.ota",
    "com.qti.factory",
    "com.emdoor.usbmode"
)

fun TextView.change(change: ((String) -> Unit)) = addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        change(s.toString())
    }
})

val normal = mutableListOf<PackageInfo>()
val dangerous = mutableListOf<PackageInfo>()
fun getNormalList(context: Context): List<PackageInfo> {
    val list = mutableListOf<PackageInfo>()
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
    val queryIntentActivities: List<ResolveInfo>? = context.packageManager?.queryIntentActivities(
        intent, PackageManager.GET_ACTIVITIES
    )
    val installedPackages: List<PackageInfo> = context.packageManager.getInstalledPackages(0)
    queryIntentActivities?.forEach { resolveInfo ->
        val packageInfo =
            installedPackages.firstOrNull { it.packageName == resolveInfo.activityInfo.packageName }
        packageInfo?.apply {
            if (!TextUtils.isEmpty(resolveInfo.activityInfo.name) && resolveInfo.resolvePackageName !in whiteList) {
                list.add(packageInfo)
            }
        }
    }
    normal.clear()
    normal.addAll(list)
    val l = installedPackages.subtract(list.toSet())
    dangerous.clear()
    dangerous.addAll(l)
    return list
}