package com.android.systemfunction

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.change
import com.android.systemfunction.ui.APPManagerActivity
import com.android.systemfunction.utils.*
import com.android.systemlib.*
import com.github.h4de5ing.baseui.alertConfirm
import com.github.h4de5ing.baseui.logD
import com.github.h4de5ing.filepicker.DialogUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var dm: DevicePolicyManager? = null
    private var componentName2 =
        ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            if (!isActiveDeviceManager(this, componentName2)) setMDM(this, componentName2)
        } catch (e: Exception) {
            "设置MDM失败".logD()
            e.printStackTrace()
        }
        updateUI()
        mdm.change { isChecked ->
            if (isChecked) {
                try {
                    "设置结果:${setMDM(this, componentName2)}".logD()
                } catch (e: Exception) {
                    Toast.makeText(this, "MDM设置失败:${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                try {
                    removeMDM(this, componentName2)
                } catch (e: Exception) {
                    Toast.makeText(this, "MDM取消失败:${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
        home.change { updateKT(ConfigEnum.DISABLE_HOME.name, if (it) "0" else "1") }
        recent.change { updateKT(ConfigEnum.DISABLE_RECENT.name, if (it) "0" else "1") }
        back.change { updateKT(ConfigEnum.DISABLE_BACK.name, if (it) "0" else "1") }
        navigation.change { updateKT(ConfigEnum.DISABLE_NAVIGATION.name, if (it) "0" else "1") }
        status.change { updateKT(ConfigEnum.DISABLE_STATUS.name, if (it) "0" else "1") }
        adb.change { updateKT(ConfigEnum.DISABLE_USB_DATA.name, if (it) "0" else "1") }
        bluetooth.change { updateKT(ConfigEnum.DISABLE_BLUETOOTH.name, if (it) "0" else "1") }
        wifi.change { updateKT(ConfigEnum.DISABLE_WIFI.name, if (it) "0" else "1") }
        data.change { updateKT(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, if (it) "0" else "1") }
        gps.change { updateKT(ConfigEnum.DISABLE_GPS.name, if (it) "0" else "1") }
        microphone.change { updateKT(ConfigEnum.DISABLE_MICROPHONE.name, if (it) "0" else "1") }
        screen_shot.change { updateKT(ConfigEnum.DISABLE_SCREEN_SHOT.name, if (it) "0" else "1") }
        screen_capture.change {
            updateKT(
                ConfigEnum.DISABLE_SCREEN_CAPTURE.name,
                if (it) "0" else "1"
            )
        }
        tf_card.change { updateKT(ConfigEnum.DISABLE_TF_CARD.name, if (it) "0" else "1") }
        phone_call.change { updateKT(ConfigEnum.DISABLE_PHONE_CALL.name, if (it) "0" else "1") }
        hot_spot.change { updateKT(ConfigEnum.DISABLE_HOT_SPOT.name, if (it) "0" else "1") }
        sms.change { updateKT(ConfigEnum.DISABLE_SMS.name, if (it) "0" else "1") }
        mms.change { updateKT(ConfigEnum.DISABLE_MMS.name, if (it) "0" else "1") }
        system_update.change {
            updateKT(
                ConfigEnum.DISABLE_SYSTEM_UPDATE.name,
                if (it) "0" else "1"
            )
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
            if (it) activeDeviceManager(
                this,
                BuildConfig.APPLICATION_ID,
                AdminReceiver::class.java.name
            )
            else removeActiveDeviceAdmin(
                this,
                BuildConfig.APPLICATION_ID,
                AdminReceiver::class.java.name
            )
        }
        reset.setOnClickListener { alertConfirm(this, "恢复出厂设置?") { if (it) reset(this) } }
        shut_down.setOnClickListener { alertConfirm(this, "关机?") { if (it) shutdown() } }
        reboot.setOnClickListener { alertConfirm(this, "重启?") { if (it) reboot() } }
        shot.setOnClickListener { testShot() }
        startService(Intent(this, ForegroundService::class.java))
        app_manager.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    APPManagerActivity::class.java
                )
            )
        }
        install.setOnClickListener {
            DialogUtils.selectFile(this, "请选择一个APK") {
                installAPK(
                    this,
                    it[0]
                )
            }
        }
        net_manager.setOnClickListener {
        }
        ("MDM包名:${getString(TestUtils.getInternalString())}").logD()
    }

    private var opt = ""
    private fun updateUI() {
        try {
            mdm.isChecked = dm!!.isAdminActive(componentName2)
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
            microphone.isChecked = isDisableMicrophone
            screen_shot.isChecked = isDisableScreenShot
            screen_capture.isChecked = isDisableScreenCapture
            tf_card.isChecked = isDisableTFCard
            phone_call.isChecked = isDisablePhoneCall
            hot_spot.isChecked = isDisableHotSpot
            sms.isChecked = isDisableSMS
            mms.isChecked = isDisableMMS
            system_update.isChecked = isDisableSystemUpdate
            restore_factory.isChecked = isDisableRestoreFactory
            device_manager.isChecked =
                isActiveDeviceManager(
                    this,
                    BuildConfig.APPLICATION_ID,
                    AdminReceiver::class.java.name
                )
            install_app.isChecked = isDisable(UserManager.DISALLOW_INSTALL_APPS)
            uninstall_app.isChecked = isDisable(UserManager.DISALLOW_UNINSTALL_APPS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disable(key: String, isDisable: Boolean) {
        disableMDM(
            this,
            ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name),
            key, isDisable
        )
    }

    private fun isDisable(key: String): Boolean = isDisableDMD(this, key)
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