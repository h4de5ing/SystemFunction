package com.android.systemfunction

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.mdmsdk.IRemoteInterface
import com.android.systemfunction.app.App.Companion.systemDao
import com.android.systemfunction.utils.*
import com.android.systemlib.*

@RequiresApi(Build.VERSION_CODES.O)
class ForegroundService : Service(), LifecycleOwner {

    override fun onBind(intent: Intent): IBinder {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return MyBinder()
    }

    class MyBinder : IRemoteInterface.Stub() {
        override fun setDisable(key: String?, disable: Boolean) {
            updateKT("$key", if (disable) "0" else "1")
        }

        override fun isDisable(key: String?): Boolean {
            return getKt("$key") == "0"
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        return super.onUnbind(intent)
    }

    override fun onStart(intent: Intent?, startId: Int) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        super.onStart(intent, startId)
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private var wifiManager: WifiManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mLocationManager: LocationManager? = null
    private var cm: ConnectivityManager? = null
    private var tm: TelephonyManager? = null
    private var WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        Log.i("gh0st", "MDM ForegroundService 启动了")
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        systemDao.observerConfigChange().observe(this) {
            println("MDM服务监控到数据库更新了")
            firstUpdate(systemDao.selectAllConfig())
            updateAllState()
        }
        initReceiver()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("mobile_data"),
            false,
            dataMonitor
        )
    }

    private val dataMonitor = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            updateData()
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("接受到广播:${intent?.action}")
            when (intent?.action) {
                //数据流量状态
                ConnectivityManager.CONNECTIVITY_ACTION -> updateData()
                //热点状态
                WIFI_AP_STATE_CHANGED_ACTION -> updateAP()
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                }
                //wifi状态
                WifiManager.WIFI_STATE_CHANGED_ACTION ->
                    updateWIFI(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0))
                //蓝牙状态
                BluetoothAdapter.ACTION_STATE_CHANGED ->
                    updateBluetooth(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0))
                //gps状态
                LocationManager.PROVIDERS_CHANGED_ACTION -> updateGPS()
            }
        }
    }

    private fun initReceiver() {
        val filter = IntentFilter()
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter.addAction(WIFI_AP_STATE_CHANGED_ACTION)
        registerReceiver(stateReceiver, filter)
    }

    private fun updateWIFI(state: Int) {
        if (state == WifiManager.WIFI_STATE_ENABLED && isDisableWIFI) {
            wifiManager?.isWifiEnabled = false
            showToast("根据安全策略，禁止使用WIFI")
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateBluetooth(state: Int) {
        if (state == BluetoothAdapter.STATE_ON && isDisableBluetooth) {
            bluetoothAdapter?.disable()
            showToast("根据安全策略，禁止使用蓝牙")
        }
    }

    private fun isGPSOpen(): Boolean {
        var state =
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) mLocationManager!!.isLocationEnabled else mLocationManager!!.isProviderEnabled(
                Settings.System.LOCATION_PROVIDERS_ALLOWED
            ))
//        val gps = mLocationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = mLocationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (/*gps==true||*/state || network == true) {
            return true
        }
        return false
    }

    private fun updateGPS() {
        if (isGPSOpen() && isDisableGPS) {
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            showToast("根据安全策略，禁止使用GPS")
        }
    }


    private fun updateData() {
        val state = Settings.Secure.getInt(contentResolver, "mobile_data", 0) == 1
        if (state && isDisableData) {
            tm?.isDataEnabled = false
            showToast("根据安全策略，禁止使用数据流量")
        }
    }

    private fun updateAP() {
        if (isHotSpotDisabled(this) && isDisableHotSpot) {
            setHotSpotDisabled(this, true)
            showToast("根据安全策略，禁止使用热点")
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        unregisterReceiver(stateReceiver)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    //TODO 测试监听广播以及各种状态是否有漏监听
    private fun updateAllState() {
        updateStatusBar()
        updateWIFI(wifiManager!!.wifiState)
        updateBluetooth(bluetoothAdapter!!.state)
        updateGPS()
        updateData()
        updateAP()
    }

    //更新状态栏
    private fun updateStatusBar() {
        var status = DISABLE_NONE
        if (isDisableHome) status = status or DISABLE_HOME
        if (isDisableRecent) status = status or DISABLE_RECENT
        if (isDisableBack) status = status or DISABLE_BACK
        if (isDisableNavigation) status = status or STATUS_DISABLE_NAVIGATION
        if (isDisableStatus) status = status or DISABLE_EXPAND
        setStatusBarInt(this, status)
    }

    private fun showToast(message: String) {
        println(message)
//        Looper.prepare()
//        RealtimeToast.makeText(this, message, Toast.LENGTH_SHORT).show()
//        Looper.loop()
    }
}