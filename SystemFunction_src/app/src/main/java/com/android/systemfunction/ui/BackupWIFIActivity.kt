package com.android.systemfunction.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.systemfunction.R
import com.android.systemfunction.bean.WIFIBean
import com.android.systemlib.*
import com.github.h4de5ing.filepicker.DialogUtils
import kotlinx.android.synthetic.main.activity_backup_wifi.*
import java.io.File
import java.util.regex.Pattern
import kotlin.io.buffered
import kotlin.io.readLines

/**
WifiDppConfiguratorActivity  生成wifi二维码
WifiDppEnrolleeActivity      扫描wifi二维码
 */
@RequiresApi(Build.VERSION_CODES.Q)
class BackupWIFIActivity : AppCompatActivity() {
    private var wifiManager: WifiManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_wifi)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        backup.setOnClickListener {
            DialogUtils.selectDir(this, "select dir", true) { files ->
                try {
                    val backFile = File("${files[0]}${File.separator}${files[1]}")
                    if (backFile.exists())
                        backFile.delete()
                    val list = mutableListOf<WIFIBean>()
                    getPrivilegedConfiguredNetworks(this).forEach { config ->
                        try {
                            val bean = WIFIBean(
                                config.SSID.removeQuotes(),
                                getSecurityString(config),
                                config.preSharedKey.removeQuotes(),
                                config.hiddenSSID
                            )
                            list.add(bean)
                            write2File(backFile.absolutePath, "${bean}\n", true)
                        } catch (_: Exception) {
                        }
                    }
                    println(list)
                } catch (e: Exception) {
                    toast("${e.message}")
                }
            }
        }
        clear.setOnClickListener {
            wifiManager?.configuredNetworks?.forEach {
                wifiManager?.removeNetwork(it.networkId)
            }
        }
        restore.setOnClickListener {
            DialogUtils.selectFile(this, "select file") { files ->
                try {
                    files[0].stream().buffered().reader("utf-8").readLines().forEach { wifiLine ->
                        parseZxingWifiQrCode(wifiLine)
                    }
                } catch (e: Exception) {
                    toast("${e.message}")
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    private fun getSecurityString(config: WifiConfiguration): String {
        if (config.allowedKeyManagement[WifiConfiguration.KeyMgmt.SAE]) return "SAE"
        if (config.allowedKeyManagement[WifiConfiguration.KeyMgmt.OWE]) return "nopass"
        //WifiConfiguration.KeyMgmt.WPA2_PSK
        if (config.allowedKeyManagement[WifiConfiguration.KeyMgmt.WPA_PSK] || config.allowedKeyManagement[4]) return "WPA"
        return if (config.wepKeys[0] == null) "nopass" else "WEP"
    }

    private fun String.removeQuotes(): String = this.replace("\"", "")

    /**
     * https://stackoverflow.com/questions/12016918/android-wifimanager-addnetwork-returns-1
     */
    private fun parseZxingWifiQrCode(message: String) {
        try {
            val keyValuesList = getKeyValueList(message)
            val security = getValueOrNull(keyValuesList, "T:")
            val ssid = getValueOrNull(keyValuesList, "S:")
            val password = getValueOrNull(keyValuesList, "P:")
            val hiddenSsidString = getValueOrNull(keyValuesList, "H:")
            val hiddenSsid = "true".equals(hiddenSsidString, true)
            println("$ssid $password $security $hiddenSsid")
            addWifi(this, ssid, password)
            val config = WifiConfiguration()
            config.status = WifiConfiguration.Status.DISABLED
            config.priority = 40

            config.SSID = "\"${ssid}\""
            config.hiddenSSID = hiddenSsid
//            config.preSharedKey = password
            if ("nopass" == security) {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                config.allowedAuthAlgorithms.clear()
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            } else if ("WPA" == security) {
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                config.preSharedKey = "\"${password}\""
            } else if ("WEP" == security) {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)

                if (getHexKey(password)) config.wepKeys[0] = password
                else config.wepKeys[0] = "\"${password}\""
                config.wepTxKeyIndex = 0
            }
//            val netId = addWifiConfig(this, config)
            val netId = wifiManager?.addNetwork(config)
            println("添加wifi:${netId}")
            wifiManager?.enableNetwork(netId!!, true)
        } catch (e: Exception) {
            toast(message)
        }
    }

    private fun getValueOrNull(list: List<String>, prefix: String): String {
        list.forEach { if (it.startsWith(prefix)) return it.substring(prefix.length) }
        return ""
    }

    private fun getKeyValueList(qrCode: String): List<String> {
        val keyValuesString = qrCode.substring("WIFI:".length)
        val regex = "(?<!\\\\)" + Pattern.quote(";")
        return keyValuesString.split(regex.toRegex()).toTypedArray().toList()
    }

    /**
     * WEP has two kinds of password, a hex value that specifies the key or
     * a character string used to generate the real hex. This checks what kind of
     * password has been supplied. The checks correspond to WEP40, WEP104 & WEP232
     * @param s
     * @return
     */
    private fun getHexKey(s: String?): Boolean {
        if (s == null) return false
        val len = s.length
        if (len != 10 && len != 26 && len != 58) return false
        for (i in 0 until len) {
            val c = s[i]
            if (c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F') continue
            return false
        }
        return true
    }
}