package com.android.systemfunction

import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.change
import com.android.systemfunction.app.App
import com.android.systemfunction.databinding.ActivityMainBinding
import com.android.systemfunction.ui.APPManagerActivity
import com.android.systemfunction.ui.PackageListActivity
import com.android.systemfunction.utils.*
import com.android.systemlib.*
import com.github.h4de5ing.baseui.alertConfirm
import com.github.h4de5ing.baseui.logD
import com.github.h4de5ing.filepicker.DialogUtils
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            if (!isAdminActive(this, App.componentName2))
                setActiveProfileOwner(this, App.componentName2)
        } catch (e: Exception) {
            "MainActivity 设置MDM失败".logD()
        }
        updateUI()
        binding.home.change { updateKT(ConfigEnum.DISABLE_HOME.name, if (it) "0" else "1") }
        binding.recent.change { updateKT(ConfigEnum.DISABLE_RECENT.name, if (it) "0" else "1") }
        binding.back.change { updateKT(ConfigEnum.DISABLE_BACK.name, if (it) "0" else "1") }
        binding.navigation.change {
            updateKT(
                ConfigEnum.DISABLE_NAVIGATION.name, if (it) "0" else "1"
            )
        }
        binding.status.change { updateKT(ConfigEnum.DISABLE_STATUS.name, if (it) "0" else "1") }
        binding.adb.change {
            if (isDebug()) setUSBDataDisabled(this, it)
            else updateKT(ConfigEnum.DISABLE_USB_DATA.name, if (it) "0" else "1")
        }
        binding.bluetooth.change {
            updateKT(
                ConfigEnum.DISABLE_BLUETOOTH.name, if (it) "0" else "1"
            )
        }
        binding.wifi.change { updateKT(ConfigEnum.DISABLE_WIFI.name, if (it) "0" else "1") }
        binding.data.change {
            updateKT(
                ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, if (it) "0" else "1"
            )
        }
        binding.gps.change { updateKT(ConfigEnum.DISABLE_GPS.name, if (it) "0" else "1") }
        binding.camera.change { updateKT(ConfigEnum.DISABLE_CAMERA.name, if (it) "0" else "1") }
        binding.microphone.change {
            updateKT(
                ConfigEnum.DISABLE_MICROPHONE.name, if (it) "0" else "1"
            )
        }
        binding.screenShot.change {
            updateKT(
                ConfigEnum.DISABLE_SCREEN_SHOT.name, if (it) "0" else "1"
            )
        }
        binding.screenCapture.change {
            updateKT(ConfigEnum.DISABLE_SCREEN_CAPTURE.name, if (it) "0" else "1")
        }
        binding.tfCard.change { updateKT(ConfigEnum.DISABLE_TF_CARD.name, if (it) "0" else "1") }
        binding.phoneCall.change {
            updateKT(
                ConfigEnum.DISABLE_PHONE_CALL.name, if (it) "0" else "1"
            )
        }
        binding.hotSpot.change { updateKT(ConfigEnum.DISABLE_HOT_SPOT.name, if (it) "0" else "1") }
        binding.sms.change { updateKT(ConfigEnum.DISABLE_SMS.name, if (it) "0" else "1") }
        binding.mms.change { updateKT(ConfigEnum.DISABLE_MMS.name, if (it) "0" else "1") }
        binding.share.change { updateKT(ConfigEnum.DISABLE_SHARE.name, if (it) "0" else "1") }
        binding.systemUpdate.change {
            updateKT(ConfigEnum.DISABLE_SYSTEM_UPDATE.name, if (it) "0" else "1")
        }
        binding.installApp.change {
            updateKT(ConfigEnum.DISABLE_INSTALL_APP.name, if (it) "0" else "1")
        }
        binding.uninstallApp.change {
            updateKT(ConfigEnum.DISABLE_UNINSTALL_APP.name, if (it) "0" else "1")
        }
        binding.restoreFactory.change {
            updateKT(ConfigEnum.DISABLE_RESTORE_FACTORY.name, if (it) "0" else "1")
        }
        binding.deviceManager.change {
            try {
                if (it) setActiveProfileOwner(this, App.componentName2)
                else {
                    removeActiveDeviceAdmin(this, App.componentName2)
                    clearProfileOwner(App.componentName2)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.reset.setOnClickListener {
            alertConfirm(this, "${getString(R.string.reset)}?") { if (it) reset(this) }
        }
        binding.shutDown.setOnClickListener {
            alertConfirm(this, "${getString(R.string.shut_down)}?") { if (it) shutdown() }
        }
        binding.reboot.setOnClickListener {
            alertConfirm(this, "${getString(R.string.reboot)}?") { if (it) reboot() }
        }
        binding.shot.setOnClickListener {
            Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SYSRQ)
//            testShot()
        }
        binding.appManager.setOnClickListener {
            startActivity(
                Intent(
                    this, APPManagerActivity::class.java
                )
            )
        }
        binding.install.setOnClickListener {
            DialogUtils.selectFile(this, "select a APK file") {
                installAPK(this, it[0]) { status ->
                    println("APP安装状态:${status}")
                }
            }
        }
        binding.ota.setOnClickListener {
            DialogUtils.selectFile(this, "select a ZIP file") {
                ota(this, it[0])
            }
        }
        binding.getRecent.setOnClickListener {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.getRecentTasks(Int.MAX_VALUE, 0)
            tasks.forEach {
                if (it != null) {
                    val packageName = "${it.baseIntent.component?.packageName}"
                    val flags =
                        (it.baseIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        println("$packageName ${it.taskId} $flags")
                    }
                }
            }
        }
        binding.removeRecent.setOnClickListener {
            //get_recent(this)
            startActivity(
                Intent(
                    this, PackageListActivity::class.java
                )
            )
        }
        ("MDM包名:${getString(TestUtils.getInternalString())} TaskId:$taskId").logD()
        startService(Intent(this, ForegroundService::class.java))
        try {
            getStorage(this, binding.result)
            getFileType(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            binding.home.isChecked = isDisableHome
            binding.recent.isChecked = isDisableRecent
            binding.back.isChecked = isDisableBack
            binding.navigation.isChecked = isDisableNavigation
            binding.status.isChecked = isDisableStatus
            binding.adb.isChecked = isDisableUSBData
            binding.bluetooth.isChecked = isDisableBluetooth
            binding.wifi.isChecked = isDisableWIFI
            binding.data.isChecked = isDisableData
            binding.gps.isChecked = isDisableGPS
            binding.camera.isChecked = isDisableCamera
            binding.microphone.isChecked = isDisableMicrophone
            binding.screenShot.isChecked = isDisableScreenShot
            binding.screenCapture.isChecked = isDisableScreenCapture
            binding.tfCard.isChecked = isDisableTFCard
            binding.phoneCall.isChecked = isDisablePhoneCall
            binding.hotSpot.isChecked = isDisableHotSpot
            binding.sms.isChecked = isDisableSMS
            binding.mms.isChecked = isDisableMMS
            binding.share.isChecked = isDisableShare
            binding.systemUpdate.isChecked = isDisableSystemUpdate
            binding.restoreFactory.isChecked = isDisableRestoreFactory
            binding.deviceManager.isChecked = isAdminActive(this, App.componentName2)
            binding.installApp.isChecked = isDisableInstallApp
            binding.uninstallApp.isChecked = isDisableUnInstallApp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun testShot() {
        try {
            val bitmap = takeScreenShot()
            val file = File("/sdcard/Pictures/1.png")
            val fos = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            toast("/sdcard/Pictures/1.png success")
        } catch (e: Exception) {
            toast("/sdcard/Pictures/1.png fail")
            e.printStackTrace()
        }
    }
}