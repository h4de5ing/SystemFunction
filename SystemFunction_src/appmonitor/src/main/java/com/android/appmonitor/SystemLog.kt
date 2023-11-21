package com.android.appmonitor

import android.annotation.SuppressLint
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.telephony.TelephonyManager
import android.text.TextUtils


/**
 * 回调 包名和状态（打开/关闭）
 */
fun setCameraUsageListener(onChange: ((String, Int) -> Unit)) {

}

/**
 * 回调包名
 */
fun setGpsUsageListener(onChange: ((String) -> Unit)) {

}

/**
 * 回调包名
 */
fun setNfcUsageListener(onChange: ((String) -> Unit)) {

}

/**
 * 回调软件和状态（打开/关闭）
 */
fun setAppUsageListener(onChange: ((String, Int) -> Unit)) {

}


lateinit var listener: (String, String, Int) -> Unit

/**
 * 回调 包名 权限 状态(同意，禁止，永久禁止)
 */
fun setAppPermissionRequestListener(onChange: ((String, String, Int) -> Unit)) {
    listener = onChange
}

/**
 * @param filePaths 过滤的列表
 * @param previousTime 从现在到以前的时间段
 *
 * @return 数据集合 packageName,filePath,fileOperateType(增删改查),logTime
 */
fun getFileUageRecordList(filePaths: List<String>, previousTime: Long): List<Map<String, String>> {
    val returnList = mutableListOf<Map<String, String>>()
    val list = read2("/sdcard/log")
    list?.forEach {
        try {
            it?.apply {
                val splits = it.split(",")
                val time = splits[0]
                val packageName = splits[1]
                val typeItems = splits[2].split("=")
                val typeStr = when (typeItems[0]) {
                    "createNewFile" -> "create"
                    "delete" -> "delete"
                    "FileInputStream" -> "input"
                    "FileOutputStream" -> "output"
                    else -> ""
                }
                if (!TextUtils.isEmpty(typeStr)) {
                    val path = typeItems[1]
                    if (filePaths.any { whitePath -> path.startsWith(whitePath) } && time.toLong() >= previousTime) {
                        returnList.add(
                            mapOf(
                                "packageName" to packageName,
                                "filePath" to path,
                                "fileOperateType" to typeStr,
                                "logTime" to time
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }
    }
    return returnList
}

/**
 * @param packageName 包名
 * @param previousTime 从现在到以前的时间段
 *
 * @return packageName,url,lotTime
 *
 */
fun getNetworkRecordList(packageName: String, previousTime: Long): List<Map<String, String>> {
    val returnList = mutableListOf<Map<String, String>>()
    val list = read2("/sdcard/log")
    list?.forEach {
        try {
            it?.apply {
                val splits = it.split(",")
                val time = splits[0]
                val packageName2 = splits[1]
                val typeItems = splits[2].split("=")
                val typeStr = typeItems[0]
                if ("openConnection" == typeStr || "SocketgetInputStream" == typeStr) {
                    if (packageName2 == packageName && time.toLong() >= previousTime) {
                        val url = typeItems[1]
                        returnList.add(
                            mapOf(
                                "packageName" to packageName2,
                                "url" to url,
                                "logTime" to time
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
//            e.printStackTrace()
        }
    }
    return returnList
}

/**
 * @param packageName 包名
 * @param previousTime 从现在到以前的时间段
 *
 * @return packageName,traffic(使用的字节数)
 *
 * 附录：
 * NetworkStatsManager
 * NetworkStatsRecorder
 */
@SuppressLint("MissingPermission")
fun getTrafficByPackageName(
    context: Context, packageName: String, previousTime: Long
): Map<String, String> {
    try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subId = tm.subscriberId
        println("获取到的SubId=${subId}")
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        nsm.querySummary(
            ConnectivityManager.TYPE_MOBILE,
            subId,
            getTimesMonthMorning(),
            System.currentTimeMillis()
        )
    } catch (_: Exception) {
    }
    val rx = TrafficStats.getUidRxBytes(getUid(context, packageName))
    val tx = TrafficStats.getUidTxBytes(getUid(context, packageName))
    return mapOf(packageName to "${tx + rx}")
}


