package com.android.systemfunction.bean

import android.net.wifi.WifiConfiguration

data class WIFIBean(
    val S: String,//ssid
    val T: String,//加密方式 nopass/WEP/WPA/SAE
    val P: String,//密码
    val H: Boolean,//是否隐藏
) {
    /**
     * Construct a barcode string for WiFi network login.
     * See https://en.wikipedia.org/wiki/QR_code#WiFi_network_login
     */
    override fun toString(): String {
        WifiConfiguration.KeyMgmt.NONE
        return "WIFI:S:$S;T:$T;P:$P;H:$H;;"
    }
}