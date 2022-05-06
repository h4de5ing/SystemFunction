package com.android.mdmclient

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import com.android.mdmsdk.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bind(this)
        home.change {
            if (isBind()) setHomeKeyDisabled(it) else {
                Toast.makeText(this, "未绑定,请检查com.android.systemfunction是否安装", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        recent.change { setRecentKeyDisable(it) }
        back.change { setBackKeyDisable(it) }
        navigation.change { setNavigaBarDisable(it) }
        status.change { setStatusBarDisable(it) }
        usb_data.change { setUSBDataDisabled(it) }
        bluetooth.change { setBluetoothDisable(it) }
        wifi.change { setWifiDisabled(it) }
        data.change { setDataConnectivityDisabled(it) }
        gps.change { setGPSDisabled(it) }
        microphone.change { setMicrophoneDisable(it) }
        screen_shot.change { setScreenShotDisable(it) }
        screen_capture.change { setScreenCaptureDisabled(it) }
        tf_card.change { setTFCardDisabled(it) }
        phone_call.change { setCallPhoneDisabled(it) }
        hot_spot.change { setHotSpotDisabled(it) }
        sms.change { disableSms(it) }
        restore_factory.change { setRestoreFactoryDisabled(it) }
        val addList = listOf(
            "com.guoshi.httpcanary",
            "com.zhihu.android",
            "com.coolapk.market"
        )
        val removeList = listOf("com.guoshi.httpcanary")
        dis_install.change {
            if (it)
                addForbiddenInstallApp(addList)
            else
                removeForbiddenInstallApp(removeList)
        }
        get_dis_install.setOnClickListener {
            text_dis_install.text = "禁止安装列表 ${getForbiddenInstallAppList()}"
        }

        install.change {
            if (it)
                addInstallPackageTrustList(addList)
            else
                removeInstallPackageTrustList(removeList)
        }
        get_install.setOnClickListener {
            text_install.text = "允许安装列表 ${getInstallPackageTrustList()}"
        }

        dis_uninstall.change {
            if (it)
                addDisallowedUninstallPackages(addList)
            else
                removeDisallowedUninstallPackages(removeList)
        }
        get_dis_uninstall.setOnClickListener {
            text_uninstall.text = "禁止卸载列表 ${getDisallowedUninstallPackageList()}"
        }

        persistent.change {
            if (it)
                addPersistentApp(addList)
            else
                removePersistentApp(removeList)
        }
        get_persistent.setOnClickListener {
            text_persistent.text = "保活列表 ${getPersistentApp()}"
        }

        super_white.change {
            if (it)
                setSuperWhiteListForSystem(addList)
            else
                removeSuperWhiteListForSystem(removeList)
        }
        get_super_white.setOnClickListener {
            text_super.text = "受信任列表 ${getSuperWhiteListForSystem()}"
        }

        delayed(1000) { runOnUiThread { updateUI() } }
    }

    private fun updateUI() {
        if (isBind()) {
            home.isChecked = isHomeKeyDisable()
            recent.isChecked = isRecentKeyDisable()
            back.isChecked = isBackKeyDisable()
            navigation.isChecked = isNavigaBarDisable()
            status.isChecked = isStatusBarDisable()
            usb_data.isChecked = isUSBDataDisabled()
            bluetooth.isChecked = isBluetoothDisabled()
            wifi.isChecked = isWifiDisabled()
            data.isChecked = isDataConnectivityDisabled()
            gps.isChecked = isGPSDisable()
            microphone.isChecked = isMicrophoneDisable()
            screen_shot.isChecked = isScreenShot()
            screen_capture.isChecked = isScreenCaptureDisabled()
            tf_card.isChecked = isTFCardDisabled()
            phone_call.isChecked = isCallPhoneDisabled()
            hot_spot.isChecked = isHotSpotDisabled()
            sms.isChecked = isSmsDisable()
            restore_factory.isChecked = isRestoreFactoryDisable()

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unbind(this)
    }

    private fun delayed(delay: Long, block: () -> Unit) {
        Handler().postDelayed(delay) { block() }
    }
}