package com.android.systemfunction

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.change
import com.android.systemfunction.app.App
import com.android.systemfunction.ui.APPManagerActivity
import com.android.systemfunction.ui.PackageListActivity
import com.android.systemfunction.utils.*
import com.android.systemlib.*
import com.github.h4de5ing.baseui.alertConfirm
import com.github.h4de5ing.baseui.logD
import com.github.h4de5ing.filepicker.DialogUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
//            if (!isAdminActive(this, App.componentName2)) setActiveProfileOwner(
//                this,
//                App.componentName2
//            )
        } catch (e: Exception) {
            "MainActivity 设置MDM失败".logD()
        }
        updateUI()
        home.change { updateKT(ConfigEnum.DISABLE_HOME.name, if (it) "0" else "1") }
        recent.change { updateKT(ConfigEnum.DISABLE_RECENT.name, if (it) "0" else "1") }
        back.change { updateKT(ConfigEnum.DISABLE_BACK.name, if (it) "0" else "1") }
        navigation.change { updateKT(ConfigEnum.DISABLE_NAVIGATION.name, if (it) "0" else "1") }
        status.change { updateKT(ConfigEnum.DISABLE_STATUS.name, if (it) "0" else "1") }
        adb.change {
            if (isDebug())
                setUSBDataDisabled(this, it)
            else updateKT(ConfigEnum.DISABLE_USB_DATA.name, if (it) "0" else "1")
        }
        bluetooth.change { updateKT(ConfigEnum.DISABLE_BLUETOOTH.name, if (it) "0" else "1") }
        wifi.change { updateKT(ConfigEnum.DISABLE_WIFI.name, if (it) "0" else "1") }
        data.change { updateKT(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, if (it) "0" else "1") }
        gps.change { updateKT(ConfigEnum.DISABLE_GPS.name, if (it) "0" else "1") }
        camera.change { updateKT(ConfigEnum.DISABLE_CAMERA.name, if (it) "0" else "1") }
        microphone.change { updateKT(ConfigEnum.DISABLE_MICROPHONE.name, if (it) "0" else "1") }
        screen_shot.change { updateKT(ConfigEnum.DISABLE_SCREEN_SHOT.name, if (it) "0" else "1") }
        screen_capture.change {
            updateKT(ConfigEnum.DISABLE_SCREEN_CAPTURE.name, if (it) "0" else "1")
        }
        tf_card.change { updateKT(ConfigEnum.DISABLE_TF_CARD.name, if (it) "0" else "1") }
        phone_call.change { updateKT(ConfigEnum.DISABLE_PHONE_CALL.name, if (it) "0" else "1") }
        hot_spot.change { updateKT(ConfigEnum.DISABLE_HOT_SPOT.name, if (it) "0" else "1") }
        sms.change { updateKT(ConfigEnum.DISABLE_SMS.name, if (it) "0" else "1") }
        mms.change { updateKT(ConfigEnum.DISABLE_MMS.name, if (it) "0" else "1") }
        share.change { updateKT(ConfigEnum.DISABLE_SHARE.name, if (it) "0" else "1") }
        system_update.change {
            updateKT(ConfigEnum.DISABLE_SYSTEM_UPDATE.name, if (it) "0" else "1")
        }
        install_app.change {
            updateKT(ConfigEnum.DISABLE_INSTALL_APP.name, if (it) "0" else "1")
        }
        uninstall_app.change {
            updateKT(ConfigEnum.DISABLE_UNINSTALL_APP.name, if (it) "0" else "1")
        }
        restore_factory.change {
            updateKT(ConfigEnum.DISABLE_RESTORE_FACTORY.name, if (it) "0" else "1")
        }
        device_manager.change {
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
        reset.setOnClickListener {
            alertConfirm(this, "${getString(R.string.reset)}?") { if (it) reset(this) }
        }
        shut_down.setOnClickListener {
            alertConfirm(this, "${getString(R.string.shut_down)}?") { if (it) shutdown() }
        }
        reboot.setOnClickListener {
            alertConfirm(this, "${getString(R.string.reboot)}?") { if (it) reboot() }
        }
        shot.setOnClickListener { testShot() }
        app_manager.setOnClickListener {
//            Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_SYSRQ)
            startActivity(
                Intent(
                    this, APPManagerActivity::class.java
                )
            )
        }
        install.setOnClickListener {
            DialogUtils.selectFile(this, "select a APK file") {
                installAPK(this, it[0]) { status ->
                    println("APP安装状态:${status}")
                }
            }
        }
        ota.setOnClickListener {
            DialogUtils.selectFile(this, "select a ZIP file") {
                ota(this, it[0])
            }
        }
        get_recent.setOnClickListener {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.getRecentTasks(Int.MAX_VALUE, 0)
            tasks.forEach {
                if (it != null) {
                    val packageName = "${it.baseIntent.component?.packageName}"
                    val flags =
                        (it.baseIntent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    println("$packageName ${it.taskId} $flags")
                }
            }
        }
        remove_recent.setOnClickListener {
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
            getStorage(this, result)
            getFileType(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            home.isChecked = isDisableHome
            recent.isChecked = isDisableRecent
            back.isChecked = isDisableBack
            navigation.isChecked = isDisableNavigation
            status.isChecked = isDisableStatus
            adb.isChecked = isDisableUSBData
            bluetooth.isChecked = isDisableBluetooth
            wifi.isChecked = isDisableWIFI
            data.isChecked = isDisableData
            gps.isChecked = isDisableGPS
            camera.isChecked = isDisableCamera
            microphone.isChecked = isDisableMicrophone
            screen_shot.isChecked = isDisableScreenShot
            screen_capture.isChecked = isDisableScreenCapture
            tf_card.isChecked = isDisableTFCard
            phone_call.isChecked = isDisablePhoneCall
            hot_spot.isChecked = isDisableHotSpot
            sms.isChecked = isDisableSMS
            mms.isChecked = isDisableMMS
            share.isChecked = isDisableShare
            system_update.isChecked = isDisableSystemUpdate
            restore_factory.isChecked = isDisableRestoreFactory
            device_manager.isChecked = isAdminActive(this, App.componentName2)
            install_app.isChecked = isDisableInstallApp
            uninstall_app.isChecked = isDisableUnInstallApp
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