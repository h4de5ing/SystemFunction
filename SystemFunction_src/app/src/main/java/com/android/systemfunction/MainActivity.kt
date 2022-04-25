package com.android.systemfunction

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.systemfunction.enums.ConfigEnum
import com.android.systemfunction.utils.*
import com.android.systemlib.removeMDM
import com.android.systemlib.setMDM
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName =
            ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)
//        mdm.isChecked = false
        mdm.checked { isChecked ->
            val id = TestUtils.getInternalString()
            println("resources：" + resources.getString(id))
            if (isChecked) {
                try {
                    println("设置结果:${setMDM(this, componentName)}")
                } catch (e: Exception) {
                    Toast.makeText(this, "MDM设置失败:${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            } else {
                try {
                    removeMDM(this, componentName)
                } catch (e: Exception) {
                    Toast.makeText(this, "MDM取消失败:${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
        home.isChecked = isDisableHome
        home.checked { updateKT(ConfigEnum.DISABLE_HOME.name, if (it) "0" else "1") }

        recent.isChecked = isDisableRecent
        recent.checked { updateKT(ConfigEnum.DISABLE_RECENT.name, if (it) "0" else "1") }
        back.isChecked = isDisableBack
        back.checked { updateKT(ConfigEnum.DISABLE_BACK.name, if (it) "0" else "1") }
        navigation.isChecked = isDisableNavigation
        navigation.checked { updateKT(ConfigEnum.DISABLE_NAVIGATION.name, if (it) "0" else "1") }
        status.isChecked = isDisableStatus
        status.checked { updateKT(ConfigEnum.DISABLE_STATUS.name, if (it) "0" else "1") }
        usb_data.isChecked = isDisableUSBData
        usb_data.checked { updateKT(ConfigEnum.DISABLE_USB_DATA.name, if (it) "0" else "1") }
        bluetooth.isChecked = isDisableBluetooth
        bluetooth.checked { updateKT(ConfigEnum.DISABLE_BLUETOOTH.name, if (it) "0" else "1") }
        wifi.isChecked = isDisableWIFI
        wifi.checked { updateKT(ConfigEnum.DISABLE_WIFI.name, if (it) "0" else "1") }
        data.isChecked = isDisableData
        data.checked { updateKT(ConfigEnum.DISABLE_DATA_CONNECTIVITY.name, if (it) "0" else "1") }
        gps.isChecked = isDisableGPS
        gps.checked { updateKT(ConfigEnum.DISABLE_GPS.name, if (it) "0" else "1") }
        microphone.isChecked = isDisableMicrophone
        microphone.checked {
            updateKT(
                ConfigEnum.DISABLE_MICROPHONE.name,
                if (it) "0" else "1"
            )
        }
        screen_shot.isChecked = isDisableScreenShot
        screen_shot.checked { updateKT(ConfigEnum.DISABLE_SCREEN_SHOT.name, if (it) "0" else "1") }
        screen_capture.isChecked = isDisableScreenCapture
        screen_capture.checked {
            updateKT(
                ConfigEnum.DISABLE_SCREEN_CAPTURE.name,
                if (it) "0" else "1"
            )
        }
        tf_card.isChecked = isDisableTFCard
        tf_card.checked { updateKT(ConfigEnum.DISABLE_TF_CARD.name, if (it) "0" else "1") }
        phone_call.isChecked = isDisablePhoneCall
        phone_call.checked { updateKT(ConfigEnum.DISABLE_PHONE_CALL.name, if (it) "0" else "1") }
        hot_spot.isChecked = isDisableHotSpot
        hot_spot.checked { updateKT(ConfigEnum.DISABLE_HOT_SPOT.name, if (it) "0" else "1") }
        sms.isChecked = isDisableSMS
        sms.checked { updateKT(ConfigEnum.DISABLE_SMS.name, if (it) "0" else "1") }
        mms.isChecked = isDisableMMS
        mms.checked { updateKT(ConfigEnum.DISABLE_MMS.name, if (it) "0" else "1") }
        system_update.isChecked = isDisableSystemUpdate
        system_update.checked {
            updateKT(
                ConfigEnum.DISABLE_SYSTEM_UPDATE.name,
                if (it) "0" else "1"
            )
        }
        restore_factory.isChecked = isDisableRestoreFactory
        restore_factory.checked {
            updateKT(
                ConfigEnum.DISABLE_RESTORE_FACTORY.name,
                if (it) "0" else "1"
            )
        }

        //setBackKeyDisable(this, isToggle)
        //dm.setCameraDisabled(componentName, isToggle)
//        get.setOnClickListener {
//            val componentName2 =
//                ComponentName(
//                    BuildConfig.APPLICATION_ID,
//                    DeviceAdminSample.DeviceAdminSampleReceiver::class.java.name
//                )
//            if (dm.isAdminActive(componentName)) {
//                startActivity(Intent(this, DeviceAdminSample::class.java))
//            } else {
//                val intent = Intent()
//                intent.action = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
//                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
//                startActivity(intent)
//            }
//        }
//        activateDeviceManager(MainActivity@ this, BuildConfig.APPLICATION_ID,AdminReceiver::class.java.name)
        //if (dm.isAdminActive(componentName)) {
        //将本应用设置为应用管理器
        //activateDeviceManager(this, BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)
        //}
        startService(Intent(this, ForegroundService::class.java))
        println("MDM包名:${getString(TestUtils.getInternalString())}")
    }
}