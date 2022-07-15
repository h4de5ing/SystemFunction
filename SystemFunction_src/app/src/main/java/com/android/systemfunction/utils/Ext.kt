package com.android.systemfunction.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.android.mdmsdk.BuildConfig
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.PackageTypeEnum
import com.android.systemfunction.app.App.Companion.application
import com.android.systemfunction.app.App.Companion.systemDao
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.bean.KeyValue
import com.android.systemfunction.bean.SettingsBean
import com.android.systemfunction.db.Config
import com.android.systemfunction.db.PackageList
import com.android.systemlib.*
import com.github.h4de5ing.base.delayed
import com.github.h4de5ing.baseui.logD
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.*
import kotlin.properties.Delegates

//TODO 将一部分工具方法放在lib中去
//TODO 可以重写这一部分,不毕保存每一个选项的值，通过数据库来跟踪即可
var isDisableHome = false
var isDisableRecent = false
var isDisableBack = false
var isDisableNavigation = false
var isDisableStatus = false
var isDisableUSBData = false
var isDisableBluetooth = false
var isDisableWIFI = false
var isDisableData = false
var isDisableGPS = false
var isDisableCamera = false
var isDisableMicrophone = false
var isDisableScreenShot = false
var isDisableScreenCapture = false
var isDisableTFCard = false
var isDisablePhoneCall = false
var isDisableHotSpot = false
var isDisableSMS = false
var isDisableMMS = false
var isDisableShare = false
var isDisableSystemUpdate = false
var isDisableRestoreFactory = false
var isDisableInstallApp = false
var isDisableUnInstallApp = false

//保存所有的配置项目
private val configs = mutableListOf<Config>()

//更新某一项目,如果某个项目是null 那么证明重来没有加入过，就insert一条
fun updateKT(key: String, value: String) {
    configs.firstOrNull { it.key == key }?.apply {
        this.value = value
        this.update()
    } ?: Config(0, key, value).insert()
}

var allDBPackages = mutableListOf<PackageList>()
fun updatePackageDB(type: Int, isAdd: Boolean, list: List<String>) {
    allDBPackages.firstOrNull { it.type == type }?.apply {
        if (isAdd) {
            this.packages = this.getPackageList().union(list).toString()
        } else {
            this.packages = this.getPackageList().subtract(list.toSet()).toString()
        }
        this.update()
    } ?: PackageList(0, type, list.toString()).insert()
}

//处理APP的禁止安装卸载 等操作
fun updateAPP(type: Int, isAdd: Boolean, list: List<String>) {
    val dbList =
        allDBPackages.firstOrNull { it.type == type }?.getPackageList() ?: emptyList()
    when (type) {
        PackageTypeEnum.DISABLE_UNINSTALL.ordinal -> {//禁止卸载
            if (isAdd) {
                "禁止卸载 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList.toSet()).forEach {
                    ("禁止卸载 add：${it}").logD()
                    disUninstallAPP(it, true)
                }
            } else {
                ("禁止卸载 remove:${dbList} ->${list} =${dbList.subtract(list.toSet())}").logD()
                dbList.intersect(list.toSet()).forEach {
                    ("禁止卸载 remove：${it}").logD()
                    disUninstallAPP(it, false)
                }
            }
        }
        PackageTypeEnum.INSTALL.ordinal -> {
            if (isAdd) {
                "允许安装 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList.toSet()).forEach {
                    ("允许安装 add：${it}").logD()
                    if (isHiddenAPP(it))
                        hiddenAPP(it, true)
                }
            } else {
                ("允许安装 remove:${dbList} ->${list} =${dbList.subtract(list.toSet())}").logD()
                dbList.intersect(list.toSet()).forEach {
                    ("允许安装 remove：${it}").logD()
                    if (isHiddenAPP(it))
                        hiddenAPP(it, false)
                }
            }
        }
        PackageTypeEnum.DISABLE_INSTALL.ordinal -> {//禁止安装
            if (isAdd) {
                "禁止安装 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList.toSet()).forEach {
                    ("禁止安装 add：${it}").logD()
                    if (isHiddenAPP(it))
                        hiddenAPP(it, true)
                }
            } else {
                ("禁止安装 remove:${dbList} ->${list} =${dbList.subtract(list.toSet())}").logD()
                dbList.intersect(list.toSet()).forEach {
                    ("禁止安装 remove：${it}").logD()
                    if (isHiddenAPP(it))
                        hiddenAPP(it, false)
                }
            }
        }
        PackageTypeEnum.PERSISTENT.ordinal -> {//应用保活
            val applist = getPowerSaveWhitelistApp(application)
            if (isAdd) {
                "应用保活 add:${applist} ->${list} =${applist.union(list)}".logD()
                applist.union(list).subtract(applist.toSet()).forEach {
                    ("应用保活 add：${it}").logD()
                    if (!isPowerSaveWhitelistApp(application, it))
                        addPowerSaveWhitelistApp(application, it)
                }
            } else {
                "应用保活 remove:${applist} ->${list} =${applist.union(list)}".logD()
                applist.intersect(list.toSet()).forEach {
                    ("应用保活 remove：${it}").logD()
                    if (isPowerSaveWhitelistApp(application, it))
                        removePowerSaveWhitelistApp(application, it)
                }
            }
        }
        PackageTypeEnum.SUPER_WHITE.ordinal -> {//应用受信任  默认自动权限
            if (isAdd) {
                "信任 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList.toSet()).forEach {
                    ("信任 add：${it}").logD()
                    grantAllPermission(it)
                }
            } else {
                ("信任 remove:${dbList} ->${list} =${dbList.subtract(list.toSet())}").logD()
                dbList.intersect(list.toSet()).forEach {
                    ("信任 remove：${it}").logD()
                }
            }
        }
    }
}

fun updateInstallAPK() {
    delayed(1000) {
        setSystemGlobal(
            application,
            ConfigEnum.DISABLE_INSTALL_APP.name.lowercase(Locale.ROOT),
            allDBPackages.firstOrNull { it.type == PackageTypeEnum.DISABLE_INSTALL.ordinal }
                ?.getPackageList().toString()
        )
        setSystemGlobal(
            application,
            ConfigEnum.INSTALL_APP.name.lowercase(Locale.ROOT),
            allDBPackages.firstOrNull { it.type == PackageTypeEnum.INSTALL.ordinal }
                ?.getPackageList().toString()
        )
    }
}

fun firstUpdatePackage(data: MutableList<PackageList>) {
    allDBPackages.clear()
    allDBPackages.addAll(data)
}

private fun PackageList.update() = systemDao.updatePackages(this)
private fun PackageList.insert() = systemDao.insertPackages(this)

@RequiresApi(Build.VERSION_CODES.N)
fun import2DB(list: List<KeyValue>) = list.forEach { updateKT(it.key, it.value) }

private fun Config.update() = systemDao.updateConfig(this)
private fun Config.insert() = systemDao.insertConfig(this)
fun getKt(key: String): String? = configs.firstOrNull { it.key == key }?.value
fun firstUpdate(data: List<Config>) {
    configs.clear()
    configs.addAll(data)
    isDisableHome =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_HOME.name }?.value.toString() == "0"
    isDisableBack =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_BACK.name }?.value.toString() == "0"
    isDisableRecent =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_RECENT.name }?.value.toString() == "0"
    isDisableNavigation =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_NAVIGATION.name }?.value.toString() == "0"
    isDisableStatus =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_STATUS.name }?.value.toString() == "0"
    isDisableUSBData =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_USB_DATA.name }?.value.toString() == "0"
    isDisableBluetooth =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_BLUETOOTH.name }?.value.toString() == "0"
    isDisableWIFI =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_WIFI.name }?.value.toString() == "0"
    isDisableData =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_DATA_CONNECTIVITY.name }?.value.toString() == "0"
    isDisableGPS =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_GPS.name }?.value.toString() == "0"
    isDisableCamera =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_CAMERA.name }?.value.toString() == "0"
    isDisableMicrophone =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_MICROPHONE.name }?.value.toString() == "0"
    isDisableScreenShot =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SCREEN_SHOT.name }?.value.toString() == "0"
    isDisableScreenCapture =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SCREEN_CAPTURE.name }?.value.toString() == "0"
    isDisableTFCard =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_TF_CARD.name }?.value.toString() == "0"
    isDisablePhoneCall =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_PHONE_CALL.name }?.value.toString() == "0"
    isDisableHotSpot =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_HOT_SPOT.name }?.value.toString() == "0"
    isDisableSMS =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SMS.name }?.value.toString() == "0"
    isDisableMMS =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_MMS.name }?.value.toString() == "0"
    isDisableShare =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SHARE.name }?.value.toString() == "0"
    isDisableSystemUpdate =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SYSTEM_UPDATE.name }?.value.toString() == "0"
    isDisableRestoreFactory =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_RESTORE_FACTORY.name }?.value.toString() == "0"
    isDisableInstallApp =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_INSTALL_APP.name }?.value.toString() == "0"
    isDisableUnInstallApp =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_UNINSTALL_APP.name }?.value.toString() == "0"
}

interface ChangeBoolean {
    fun change(boolean: Boolean)
}

fun isDebug(): Boolean = BuildConfig.DEBUG || File("/sdcard/debug").exists()

var changeBoolean: ChangeBoolean? = null
var isDisableScreenShotReceivedChange: Boolean by Delegates.observable(false) { _, _, new ->
    changeBoolean?.change(new)
}

fun setBooleanChange(change: ((Boolean) -> Unit)) {
    changeBoolean = object : ChangeBoolean {
        override fun change(boolean: Boolean) {
            change(boolean)
        }
    }
}

fun TextInputEditText.change(change: ((String) -> Unit)) {
    this.addTextChangedListener { doOnTextChanged { text, _, _, _ -> change("$text") } }
}

fun timer(delay: Long, block: () -> Unit) {
    Timer().schedule(object : TimerTask() {
        override fun run() {
            block()
        }
    }, 0, delay)
}

@SuppressLint("WrongConstant", "QueryPermissionsNeeded")
fun getInstallApp(): List<AppBean> {
    val list = mutableListOf<AppBean>()
    val pm = application.packageManager
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
    val queryIntentActivities: List<ResolveInfo> =
        pm.queryIntentActivities(intent, 0x00002000)
//    PackageManager.MATCH_UNINSTALLED_PACKAGES
//    PackageManager.MATCH_DISABLED_COMPONENTS
    queryIntentActivities.forEach {
        if (application.packageName != it.activityInfo.packageName) {
            try {
                list.add(
                    AppBean(
                        "${it.loadLabel(pm)}",
                        it.activityInfo.packageName,
                        it.activityInfo.name,
                        drawable2ByteArray(it.loadIcon(pm))
                    )
                )
            } catch (e: Exception) {
            }
        }
    }
    "应用个数:${queryIntentActivities.size}".logD()
    return list.distinctBy { it.packageName }
}

/**
 * 从setting providers读取数据
 * content query --uri content://settings/global/wifi_on
 * content insert --uri content://settings/global --bind name:s:preferred_network_mode1 --bind value:i:0
 */
fun getAllSettingsForSettingsBean(context: Context): SettingsBean {
    val global = getUriFor(context, Uri.parse("content://settings/global"))
    val system = getUriFor(context, Uri.parse("content://settings/system"))
    val secure = getUriFor(context, Uri.parse("content://settings/secure"))
    return SettingsBean(global, system, secure)
}

/**
 * 从json解析数据
 */
fun parserJson(json: String): SettingsBean {
    val obj = JsonParser.parseString(json) as JsonObject
    val global = obj.getAsJsonArray("global").toList()
    val system = obj.getAsJsonArray("system").toList()
    val secure = obj.getAsJsonArray("secure").toList()
    return SettingsBean(global, system, secure)
}

fun putSettings(context: Context, bean: SettingsBean) {
    putAllSettings(context, Uri.parse("content://settings/global"), bean.global)
    putAllSettings(context, Uri.parse("content://settings/system"), bean.system)
    putAllSettings(context, Uri.parse("content://settings/secure"), bean.secure)
}

/**
 * 获取apn
 * content query --uri content://telephony/carriers/ --where "mcc=460"
 * /system/etc/apns-conf.xml
 * /data/data/com.android.providers.telephony/databases/mmssms.db
 */
fun getAPN(context: Context): String {
    val uri = Uri.parse("content://telephony/carriers")
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    val names = mutableListOf<String>()
    val sb = StringBuilder()
    if (cursor != null && cursor.moveToFirst()) {
        cursor.columnNames.forEach { names.add(it) }
        println(names)
        while (cursor.moveToNext()) {
            names.forEach { name ->
                val index = cursor.getColumnIndex(name)
                if (index >= 0) {
                    val value = cursor.getString(index)
                    sb.append("$value,")
                }
            }
            sb.append("\n")
        }
        cursor.close()
    }
    return sb.toString()
}

/**
 * 删除apn数据库
 */
fun cleanAPN(context: Context) {
    val uri = Uri.parse("content://telephony/carriers")
    val result = context.contentResolver.delete(uri, "_id>=?", arrayOf("0"))
    println("删除结果:${result}")
}


/**
 * 数据量太大
 * android.os.TransactionTooLargeException: data parcel size 5461200 bytes
 *  [_id, name, numeric, mcc, mnc, carrier_id, apn, user, server, password, proxy, port, mmsproxy, mmsport, mmsc, authtype, type, current, protocol, roaming_protocol, carrier_enabled, bearer, bearer_bitmask, network_type_bitmask, mvno_type, mvno_match_data, sub_id, profile_id, modem_cognitive, max_conns, wait_time, max_conns_time, mtu, edited, user_visible, user_editable, owned_by, apn_set_id, skip_464xlat]
 */
fun setAPN(context: Context, list: List<String>, done: ((String, Boolean) -> Unit)) {
    val uri = Uri.parse("content://telephony/carriers")
    val resolver = context.contentResolver
    for (i in list.indices) {
        val line = list[i]
        try {
            val items = line.split(",")
            if (items.size > 3) {
                val _id = items[0]
                val value = ContentValues()
                value.put("name", items[1])
                value.put("numeric", items[2])
                value.put("mcc", items[3])
                value.put("mnc", items[4])
                value.put("apn", items[6])
                value.put("user", items[7])
                value.put("server", items[8])
                value.put("password", items[9])
                value.put("proxy", items[10])
                value.put("port", items[11])
                value.put("mmsproxy", items[12])
                value.put("mmsport", items[13])
                value.put("mmsc", items[14])
                value.put("type", items[16])
                val result = resolver.insert(uri, value)
                println("insert result：${result}")
                done("${i}/${list.size}", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //备份结束
    done("", true)
}

private fun JsonArray.toList(): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    this.forEach { element ->
        try {
            val obj = element as JsonObject
            obj.keySet().forEach { list.add(Pair(it, obj[it].asString)) }
        } catch (e: Exception) {
            println("null $element ${e.message}")
        }
    }
    return list
}