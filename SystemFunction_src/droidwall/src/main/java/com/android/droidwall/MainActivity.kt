package com.android.droidwall

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.IConnectivityManager
import android.net.INetworkPolicyManager
import android.os.Bundle
import android.os.INetworkManagementService
import android.os.ServiceManager
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val adapter = HomeGridAppAdapter()
    private val allAppList = mutableListOf<ApplicationInfo>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            val service: INetworkManagementService =
                INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
            val iConnectivityManager: IConnectivityManager =
                IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"))
            val connectivityManager: ConnectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkPolicyService =
                INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"))
            val toggle = findViewById<AppCompatToggleButton>(R.id.toggle)
//            toggle.isChecked = service.isFirewallEnabled
            toggle.setOnCheckedChangeListener { _, isChecked ->
//                service.isFirewallEnabled = isChecked //true 白名单  false 黑名单
                //10105 chrome
//                iConnectivityManager.setAirplaneMode(isChecked)//飞行模式
//                service.setFirewallUidRule(0, 10105, if (isChecked) 1 else 2)
//                service.setFirewallChainEnabled()
//                if (isChecked) white(service) else black(service)
//                service.setFirewallUidRules(1, intArrayOf(10105), intArrayOf(1))
                service.setFirewallChainEnabled(1, isChecked)
            }
            //const int FIREWALL_CHAIN_NONE = 0
            //const int FIREWALL_CHAIN_DOZABLE = 1
            //const int FIREWALL_CHAIN_STANDBY = 2
            //const int FIREWALL_CHAIN_POWERSAVE = 3
            //chain 1 3  white (rule 1 allow)
            //chain 2 black (rule 2 deny)
            findViewById<AppCompatToggleButton>(R.id.toggle2).setOnCheckedChangeListener { _, isChecked ->
                service.setFirewallUidRules(1, intArrayOf(10105), intArrayOf(1))
            }
            val list = findViewById<RecyclerView>(R.id.list)
            list.setHasFixedSize(true)
            list.layoutManager = LinearLayoutManager(this)
            list.adapter = adapter
            adapter.setOnCheckedChangeListener { uid, isChecked ->
                println("$uid  $isChecked")
                service.setFirewallUidRule(1, uid, if (isChecked) 1 else 2)
            }
            loadData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadData() {
        allAppList.clear()
        val installedPackages: List<PackageInfo> = packageManager.getInstalledPackages(0)
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
        packageManager.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES)
            .forEach { resolveInfo ->
                val packageInfo =
                    installedPackages.firstOrNull { it.packageName == resolveInfo.activityInfo.packageName }
                if (packageInfo != null && !TextUtils.isEmpty(resolveInfo.activityInfo.name)) {
                    val applicationInfo =
                        packageManager.getApplicationInfo(packageInfo.packageName, 0)
//                    if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)//排除系统应用
                    if (applicationInfo.uid > 1000) allAppList.add(applicationInfo)
                }
            }
        allAppList.sortBy { it.uid }
        adapter.setNewInstance(allAppList)
    }
}