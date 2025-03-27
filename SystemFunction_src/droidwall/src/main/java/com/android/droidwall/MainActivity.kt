package com.android.droidwall

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.*
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.droidwall.App.Companion.fwDao
import com.android.droidwall.App.Companion.iNetD
import com.android.droidwall.db.FirewallData
import com.android.droidwall.services.ForegroundService
import com.android.droidwall.utils.SPUtils
import com.android.droidwall.utils.configs
import com.android.droidwall.utils.insert2DB
import com.android.droidwall.utils.updateKT

class MainActivity : AppCompatActivity() {
    private val adapter = HomeGridAppAdapter()
    private val allAppList = mutableListOf<ApplicationInfo>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            startService(Intent(this, ForegroundService::class.java))
            val service =
                INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
            val toggle = findViewById<AppCompatToggleButton>(R.id.toggle)
            toggle.isChecked = service.isFirewallEnabled
            val toggleValue = SPUtils.getSp(this, "toggle", false)
            toggle.isChecked = toggleValue == true
            toggle.setOnCheckedChangeListener { _, isChecked ->
//                service.isFirewallEnabled = isChecked //true 白名单  false 黑名单
//                iConnectivityManager.setAirplaneMode(isChecked)//飞行模式
//                service.setFirewallUidRule(0, 10105, if (isChecked) 1 else 2)
//                service.setFirewallUidRules(1, intArrayOf(10105), intArrayOf(1))
                service.setFirewallChainEnabled(1, isChecked)
                SPUtils.setSP(this, "toggle", isChecked)
                if (isChecked) {
                    fwDao.selectAllConfig().forEach {
                        try {
                            service.setFirewallUidRule(1, it.uid, if (it.isWhite) 1 else 2)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            //const int FIREWALL_CHAIN_NONE = 0
            //const int FIREWALL_CHAIN_DOZABLE = 1
            //const int FIREWALL_CHAIN_STANDBY = 2
            //const int FIREWALL_CHAIN_POWERSAVE = 3
            //chain 1 3  white (rule 1 allow)
            //chain 2 black (rule 2 deny)
            findViewById<AppCompatToggleButton>(R.id.toggle2).setOnCheckedChangeListener { _, isChecked ->
                iNetD?.setFirewallUidRules(1, intArrayOf(0), intArrayOf(0))//清空白名单
                fwDao.deleteAllConfig()
//                service.setFirewallUidRule(1, 10105, 1)//添加到白名单
            }
            val list = findViewById<RecyclerView>(R.id.list)
            list.layoutManager = LinearLayoutManager(this)
            list.adapter = adapter
            adapter.setOnClickListener(object : HomeGridAppAdapter.OnCheckedChangeListener {
                override fun onCheckedChanged(uid: Int, isChecked: Boolean) {
                    updateKT(uid, isChecked)
                    iNetD?.setFirewallUidRule(1, uid, if (isChecked) 1 else 2)
                    print("uid: $uid isChecked: $isChecked")
                }
            })
            val chain = findViewById<EditText>(R.id.chain)
            val value = findViewById<EditText>(R.id.value)
            findViewById<Button>(R.id.setting).setOnClickListener {
                try {
                    service.setFirewallUidRule(
                        chain.text.toString().toInt(), 10105, value.text.toString().toInt()
                    )
                    Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            loadData()
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    val newList = mutableListOf<FirewallData>()
                    configs.forEach { fw ->
                        val applicationInfo = allAppList.firstOrNull { it.uid == fw.uid }
                        applicationInfo?.apply {
                            val appName = packageManager.getApplicationLabel(applicationInfo)
                            val icon = applicationInfo.loadIcon(packageManager)
                            val fwNew = FirewallData(0, fw.uid, fw.isWhite)
                            fwNew.appName = "$appName"
                            fwNew.icon = icon
                            newList.add(fwNew)
                        }
                    }
                    adapter.setNewInstance(newList)
                    adapter.notifyDataSetChanged()
                }
            }, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadData() {
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
                    if (applicationInfo.uid > 1000) {
                        val permissions = packageManager.getPackageInfo(
                            packageInfo.packageName, PackageManager.GET_PERMISSIONS
                        ).requestedPermissions
                        if (permissions?.contains("android.permission.INTERNET") == true) {
                            allAppList.add(applicationInfo)
                            insert2DB(applicationInfo.uid, false)
                        }
                    }
                }
            }
    }
}