package com.android.systemlib

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import java.util.Locale

private const val WIFI_TYPE_NO_PASSWD: Int = 0x11
private const val WIFI_TYPE_WEP: Int = 0x12
private const val WIFI_TYPE_WPA_WPA2: Int = 0x13

private fun wifiManager(context: Context): WifiManager =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

private fun mapWifiType(typeString: String): Int =
    when (typeString.uppercase(Locale.getDefault())) {
        "WEP" -> WIFI_TYPE_WEP
        "WPA", "WPA2" -> WIFI_TYPE_WPA_WPA2
        else -> WIFI_TYPE_NO_PASSWD
    }

fun isWifiOpened(context: Context): Boolean = wifiManager(context).isWifiEnabled

@SuppressLint("MissingPermission")
fun openWifi(context: Context): Boolean = wifiManager(context).setWifiEnabled(true)

@SuppressLint("MissingPermission")
fun closeWifi(context: Context): Boolean = wifiManager(context).setWifiEnabled(false)

@SuppressLint("MissingPermission")
fun addNetwork(context: Context, ssid: String, passwd: String, typeString: String): Boolean =
    addNetwork(wifiManager(context), ssid, passwd, mapWifiType(typeString))
