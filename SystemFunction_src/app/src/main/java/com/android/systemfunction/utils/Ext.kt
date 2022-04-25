package com.android.systemfunction.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.android.systemfunction.app.App.Companion.systemDao
import com.android.systemfunction.bean.KeyValue
import com.android.systemfunction.db.Config
import com.android.systemfunction.enums.ConfigEnum

//TODO 可以重写这一部分
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

//保存所有的配置项目
private val configs = mutableListOf<Config>()

//更新某一项目,如果某个项目是null 那么证明重来没有加入过，就insert一条
fun updateKT(key: String, value: String) {
    configs.firstOrNull { it.key == key }?.apply {
        this.value = value
        this.update()
    } ?: Config(0, key, value).insert()
}

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