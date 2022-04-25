package com.android.mdmclient

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.os.postDelayed
import com.android.mdmsdk.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            bind(this)
            delayed(1000) {
                runOnUiThread {
                    home.isChecked = isHomeKeyDisable()
                    home.change { setHomeKeyDisabled(it) }
                    recent.isChecked = isRecentKeyDisable()
                    recent.change { setRecentKeyDisable(it) }
                    back.isChecked = isBackKeyDisable()
                    back.change { setBackKeyDisable(it) }
                    navigation.isChecked = isNavigaBarDisable()
                    navigation.change { setNavigaBarDisable(it) }
                    status.isChecked = isStatusBarDisable()
                    status.change { setStatusBarDisable(it) }
                    bluetooth.isChecked = isBluetoothDisabled()
                    bluetooth.change { setBluetoothDisable(it) }
                    wifi.isChecked = isWifiDisabled()
                    wifi.change { setWifiDisabled(it) }
                    data.isChecked = isDataConnectivityDisabled()
                    data.change { setDataConnectivityDisabled(it) }
                    gps.isChecked = isGPSDisable()
                    gps.change { setGPSDisabled(it) }
                    hot_spot.isChecked = isHotSpotDisabled()
                    hot_spot.change { setHotSpotDisabled(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            unbind(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun AppCompatCheckBox.change(change: ((Boolean) -> Unit)) {
        this.setOnCheckedChangeListener { _, isChecked ->
            change(isChecked)
        }
    }

    fun delayed(delay: Long, block: () -> Unit) {
        Handler().postDelayed(delay) { block() }
    }
}