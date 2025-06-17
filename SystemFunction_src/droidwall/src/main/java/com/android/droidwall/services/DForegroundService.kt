package com.android.droidwall.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.IConnectivityManager
import android.net.INetworkPolicyManager
import android.os.IBinder
import android.os.INetworkManagementService
import android.os.ServiceManager
import com.android.droidwall.App.Companion.fwDao
import com.android.droidwall.utils.SPUtils

class DForegroundService : Service() {
    private var service: INetworkManagementService? = null
    override fun onCreate() {
        super.onCreate()
        try {
            println("监控服务开始运行")
            service =
                INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
            val iConnectivityManager: IConnectivityManager =
                IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"))
            val connectivityManager: ConnectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkPolicyService =
                INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"))
            //如果开启了防火墙，开机后就执行防火墙策略
            val toggle = SPUtils.getSp(this, "toggle", false)
            service?.setFirewallChainEnabled(1, toggle == true)
            if (toggle == true) {
                fwDao.selectAllConfig().forEach {
                    try {
                        service?.setFirewallUidRule(1, it.uid, if (it.isWhite) 1 else 2)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}