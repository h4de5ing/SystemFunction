package com.android.systemfunction.utils

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import com.android.mdmsdk.BuildConfig
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.PackageTypeEnum
import com.android.systemfunction.app.App
import com.android.systemfunction.app.App.Companion.application
import com.android.systemfunction.app.App.Companion.systemDao
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.bean.KeyValue
import com.android.systemfunction.db.Config
import com.android.systemfunction.db.PackageList
import com.android.systemlib.addPowerSaveWhitelistApp
import com.android.systemlib.getPowerSaveWhitelistApp
import com.android.systemlib.isPowerSaveWhitelistApp
import com.android.systemlib.removePowerSaveWhitelistApp
import com.github.h4de5ing.base.delayed
import com.github.h4de5ing.baseui.logD
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
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
var isDisableMicrophone = false
var isDisableScreenShot = false
var isDisableScreenCapture = false
var isDisableTFCard = false
var isDisablePhoneCall = false
var isDisableHotSpot = false
var isDisableSMS = false
var isDisableMMS = false
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
            this.packages = this.getPackageList().subtract(list).toString()
        }
        this.update()
    } ?: PackageList(0, type, list.toString()).insert()
}

//处理APP的禁止安装卸载 等操作
fun updateAPP(type: Int, isAdd: Boolean, list: List<String>) {
    val dbList =
        allDBPackages.firstOrNull { it.type == type }?.let { it.getPackageList() } ?: emptyList()
    when (type) {
        PackageTypeEnum.DISABLE_UNINSTALL.ordinal -> {//禁止卸载
            if (isAdd) {
                "禁止卸载 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList).forEach {
                    ("禁止卸载 add：${it}").logD()
                    disUninstallAPP(application, App.componentName2, it, true)
                }
            } else {
                ("禁止卸载 remove:${dbList} ->${list} =${dbList.subtract(list)}").logD()
                dbList.intersect(list).forEach {
                    ("禁止卸载 remove：${it}").logD()
                    disUninstallAPP(application, App.componentName2, it, false)
                }
            }
        }
        PackageTypeEnum.DISABLE_INSTALL.ordinal -> {//禁止安装
            if (isAdd) {
                "禁止安装 add:${dbList} ->${list} =${dbList.union(list)}".logD()
                dbList.union(list).subtract(dbList).forEach {
                    ("禁止安装 add：${it}").logD()
                    if (isHiddenAPP(application, App.componentName2, it))
                        hiddenAPP(application, App.componentName2, it, true)
                }
            } else {
                ("禁止安装 remove:${dbList} ->${list} =${dbList.subtract(list)}").logD()
                dbList.intersect(list).forEach {
                    ("禁止安装 remove：${it}").logD()
                    if (isHiddenAPP(application, App.componentName2, it))
                        hiddenAPP(application, App.componentName2, it, false)
                }
            }
        }
        PackageTypeEnum.PERSISTENT.ordinal -> {//应用保活
            val applist = getPowerSaveWhitelistApp(application)
            if (isAdd) {
                "应用保活 add:${applist} ->${list} =${applist.union(list)}".logD()
                applist.union(list).subtract(applist).forEach {
                    ("应用保活 add：${it}").logD()
                    if (isPowerSaveWhitelistApp(application, it))
                        addPowerSaveWhitelistApp(application, it)
                }
            } else {
                "应用保活 add:${applist} ->${list} =${applist.union(list)}".logD()
                applist.union(list).subtract(applist).forEach {
                    ("应用保活 remove：${it}").logD()
                    if (isPowerSaveWhitelistApp(application, it))
                        removePowerSaveWhitelistApp(application, it)
                }
            }
        }
    }
}

fun updateInstallAPK() {
    delayed(1000) {//延迟1秒钟处理禁止安装列表
        allDBPackages.firstOrNull { it.type == PackageTypeEnum.DISABLE_INSTALL.ordinal }?.let {
            it.getPackageList().forEach { packageName ->
                packageName.logD()
                hiddenAPP(application, App.componentName2, packageName, true)
            }
        }
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
    isDisableSystemUpdate =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_SYSTEM_UPDATE.name }?.value.toString() == "0"
    isDisableRestoreFactory =
        configs.firstOrNull { it.key == ConfigEnum.DISABLE_RESTORE_FACTORY.name }?.value.toString() == "0"
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

@SuppressLint("WrongConstant")
fun getInstallApp(): List<AppBean> {
    val list = mutableListOf<AppBean>()
    val pm = application.packageManager
    val intent = Intent(Intent.ACTION_MAIN)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
    val queryIntentActivities: List<ResolveInfo> =
        pm.queryIntentActivities(intent, PackageManager.GET_UNINSTALLED_PACKAGES)
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

private fun drawable2Bitmap(icon: Drawable): Bitmap {
    val bitmap =
        Bitmap.createBitmap(
            icon.intrinsicWidth,
            icon.intrinsicHeight,
            if (icon.opacity == PixelFormat.OPAQUE) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        )
    val canvas = Canvas(bitmap)
    icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
    icon.draw(canvas)
    return bitmap
}

//序列化 Drawable->Bitmap->ByteArray
fun drawable2ByteArray(icon: Drawable): ByteArray {
    return bitmap2ByteArray(drawable2Bitmap(icon))
}

private fun bitmap2ByteArray(bitmap: Bitmap): ByteArray {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return baos.toByteArray()
}

//反序列化 ByteArray->Bitmap->Drawable
fun byteArray2Drawable(byteArray: ByteArray): Drawable? {
    val bitmap = byteArray2Bitmap(byteArray)
    return if (bitmap == null) null else BitmapDrawable(bitmap)
}

private fun byteArray2Bitmap(byteArray: ByteArray): Bitmap? {
    return if (byteArray.isNotEmpty()) BitmapFactory.decodeByteArray(
        byteArray,
        0,
        byteArray.size
    ) else null
}

/**
 * 禁止卸载单个应用
 */
fun disUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setUninstallBlocked(componentName, packageName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun isDisUninstallAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isUninstallBlocked(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 暂停应用，可以看到图标，但是不能使用
 */
@RequiresApi(Build.VERSION_CODES.N)
fun suspendedAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setPackagesSuspended(componentName, arrayOf(packageName), isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@RequiresApi(Build.VERSION_CODES.N)
fun isSuspendedAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isPackageSuspended(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 隐藏应用
 * pm list packages 无法显示包名
 */
fun hiddenAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setApplicationHidden(componentName, packageName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun isHiddenAPP(
    context: Context,
    componentName: ComponentName,
    packageName: String,
): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .isApplicationHidden(componentName, packageName)
    } catch (e: Exception) {
        return false
    }
}

/**
 * 禁止截图
 */
fun setScreenCaptureDisabled(
    context: Context,
    componentName: ComponentName,
    isDisable: Boolean
) {
    try {
        (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .setScreenCaptureDisabled(componentName, isDisable)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 是否禁止截图
 */
fun getScreenCaptureDisabled(context: Context, componentName: ComponentName): Boolean {
    return try {
        return (context.applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
            .getScreenCaptureDisabled(componentName)
    } catch (e: Exception) {
        return false
    }
}