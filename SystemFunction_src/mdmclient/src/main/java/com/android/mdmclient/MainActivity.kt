package com.android.mdmclient

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import com.android.mdmclient.databinding.ActivityMainBinding
import com.android.mdmsdk.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bind(this)
        binding.home.change {
            if (isBind()) setHomeKeyDisabled(it) else {
                Toast.makeText(this, "未绑定,请检查com.android.systemfunction是否安装", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.recent.change { setRecentKeyDisable(it) }
        binding.back.change { setBackKeyDisable(it) }
        binding.navigation.change { setNavigaBarDisable(it) }
        binding.status.change { setStatusBarDisable(it) }
        binding.usbData.change { setUSBDataDisabled(it) }
        binding.bluetooth.change { setBluetoothDisable(it) }
        binding.wifi.change { setWifiDisabled(it) }
        binding.data.change { setDataConnectivityDisabled(it) }
        binding.gps.change { setGPSDisabled(it) }
        binding.microphone.change { setMicrophoneDisable(it) }
        binding.screenShot.change { setScreenShotDisable(it) }
        binding.screenCapture.change { setScreenCaptureDisabled(it) }
        binding.tfCard.change { setTFCardDisabled(it) }
        binding.phoneCall.change { setCallPhoneDisabled(it) }
        binding.hotSpot.change { setHotSpotDisabled(it) }
        binding.sms.change { disableSms(it) }
        binding.restoreFactory.change { setRestoreFactoryDisabled(it) }
        val addList = listOf(
            "com.guoshi.httpcanary",
            "com.zhihu.android",
            "com.coolapk.market"
        )
        val removeList = listOf("com.guoshi.httpcanary")
        binding.disInstall.change {
            if (it)
                addForbiddenInstallApp(addList)
            else
                removeForbiddenInstallApp(removeList)
        }
        binding.getDisInstall.setOnClickListener {
            binding.textDisInstall.text = "禁止安装列表 ${getForbiddenInstallAppList()}"
        }

        binding.install.change {
            if (it)
                addInstallPackageTrustList(addList)
            else
                removeInstallPackageTrustList(removeList)
        }
        binding.getInstall.setOnClickListener {
            binding.textInstall.text = "允许安装列表 ${getInstallPackageTrustList()}"
        }

        binding.disUninstall.change {
            if (it)
                addDisallowedUninstallPackages(addList)
            else
                removeDisallowedUninstallPackages(removeList)
        }
        binding.getDisUninstall.setOnClickListener {
            binding.textUninstall.text = "禁止卸载列表 ${getDisallowedUninstallPackageList()}"
        }

        binding.persistent.change {
            if (it)
                addPersistentApp(addList)
            else
                removePersistentApp(removeList)
        }
        binding.getPersistent.setOnClickListener {
            binding.textPersistent.text = "保活列表 ${getPersistentApp()}"
        }

        binding.superWhite.change {
            if (it)
                setSuperWhiteListForSystem(addList)
            else
                removeSuperWhiteListForSystem(removeList)
        }
        binding.getSuperWhite.setOnClickListener {
            binding.textSuper.text = "受信任列表 ${getSuperWhiteListForSystem()}"
        }

        delayed(1000) { runOnUiThread { updateUI() } }
    }

    private fun updateUI() {
        if (isBind()) {
            binding.home.isChecked = isHomeKeyDisable()
            binding.recent.isChecked = isRecentKeyDisable()
            binding.back.isChecked = isBackKeyDisable()
            binding.navigation.isChecked = isNavigaBarDisable()
            binding.status.isChecked = isStatusBarDisable()
            binding.usbData.isChecked = isUSBDataDisabled()
            binding.bluetooth.isChecked = isBluetoothDisabled()
            binding.wifi.isChecked = isWifiDisabled()
            binding.data.isChecked = isDataConnectivityDisabled()
            binding.gps.isChecked = isGPSDisable()
            binding.microphone.isChecked = isMicrophoneDisable()
            binding.screenShot.isChecked = isScreenShot()
            binding.screenCapture.isChecked = isScreenCaptureDisabled()
            binding.tfCard.isChecked = isTFCardDisabled()
            binding.phoneCall.isChecked = isCallPhoneDisabled()
            binding.hotSpot.isChecked = isHotSpotDisabled()
            binding.sms.isChecked = isSmsDisable()
            binding.restoreFactory.isChecked = isRestoreFactoryDisable()
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