package com.android.appmonitor

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


fun getPackageFromPid(pid: Int): String {
    var pkgName = ""
    try {
        pkgName = File("/proc/$pid/cmdline").readText()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return pkgName
}

//TODO 需要缓存记录退出的pid所对应的包名123->com.android.aa
fun getPackageFromPid2(context: Context, pid: Int): String {
    val am = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
    val runningProcess = am.runningAppProcesses
    return runningProcess.firstOrNull { it.pid == pid }?.processName ?: ""
}

fun now(): String = SimpleDateFormat(
    "yyyy-MM-dd HH:mm:ss", Locale.CHINA
).format(Date(System.currentTimeMillis()))

fun writePackageList(context: Context): List<String>? {
    var list: List<String>? = null
    try {
        val pm = context.packageManager
        list = pm.getInstalledPackages(0).map { it.packageName }.toList()
//        val intent = Intent(Intent.ACTION_MAIN)
//        intent.addCategory(Intent.CATEGORY_LAUNCHER)
//        intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
//        val queryIntentActivities: List<ResolveInfo> = pm.queryIntentActivities(intent, 0x00002000)
//        list = queryIntentActivities.map { it.activityInfo.packageName }.toList()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun getTimesMonthMorning(): Long {
    val cal = Calendar.getInstance()
    cal.set(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
        0,
        0,
        0
    )
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
    return cal.timeInMillis
}

fun getUid(context: Context, packageName: String): Int {
    var uid = -1
    val pm = context.packageManager
    try {
        val applicationInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
        uid = applicationInfo.uid
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return uid
}

fun getPackageFromUid(context: Context, uid: Int): String? {
    return context.packageManager.getNameForUid(uid)
}

fun read2(path: String?): List<String?>? {
    var lines: List<String?>? = null
    val file = Paths.get(path)
    try {
        lines = Files.readAllLines(file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return lines
}