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
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.mdmsdk.ConfigEnum
import com.android.mdmsdk.IRemoteInterface
import com.android.systemfunction.app.App
import com.android.systemfunction.app.App.Companion.systemDao
import com.android.systemfunction.utils.*
import com.android.systemlib.*
import com.github.h4de5ing.baseui.logD
import java.util.*

//https://blog.csdn.net/qq_35501560/article/details/105948631
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

        override fun removeWifi(ssid: String?) {
            removeWifiConfig(App.application, "$ssid")
        }

        override fun deviceManager(packageName: String?, className: String?, isRemove: Boolean) {
            if (isRemove) removeActiveDeviceAdmin(App.application, App.componentName2)
            else setActiveAdmin(App.componentName2)
        }

        override fun defaultLauncher(packageName: String?, isClean: Boolean) {
            if (isClean) clearDefaultLauncher(App.application, "$packageName")
            else setDefaultLauncher(App.application, "$packageName")
        }

        override fun shutdown(isReboot: Boolean) {
            if (isReboot) reboot() else shutdown()
        }

        override fun resetDevice() {
            reset(App.application)
//            wipeDate(App.application)
        }

        //添加移除包
        override fun packageManager(list: Array<out String>, isAdd: Boolean, type: Int) {
            updateAPP(type, isAdd, list.toList())
            updatePackageDB(type, isAdd, list.toList())
        }

        //获取包
        override fun getPackages(type: Int): Array<String> {
            return systemDao.selectAllPackagesList(type)[0].getPackageList().toTypedArray()
        }

        override fun setSettings(key: String?, value: String?) {
            key?.apply {
                Settings.Global.putString(App.application.contentResolver, key, value)
            }
        }

        override fun getSettings(key: String?): String {
            return Settings.Global.getString(App.application.contentResolver, key)
        }

        override fun getDeviceInfo(): String {
            return Build.getSerial()
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
    private var windowManager: WindowManager? = null
    private var userManager: UserManager? = null
    private var WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        "MDM ForegroundService 启动了".logD()
        try {
            if (!isAdminActive(this, App.componentName2)) setActiveProfileOwner(
                this,
                App.componentName2
            )
        } catch (e: Exception) {
            "ForegroundService设置MDM失败".logD()
        }
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        systemDao.observerConfigChange().observe(this) {
            "MDM服务监控到config更新了".logD()
            firstUpdate(systemDao.selectAllConfig())
            updateAllState()
        }
        systemDao.observerPackagesList()
            .observe(this) {
                "MDM服务监控到packages更新了".logD()
                firstUpdatePackage(systemDao.selectAllPackages())
            }
        initReceiver()
        observer("mobile_data") { updateData() }
        observer("adb_enabled") { updateADB() }
        //startService(Intent(this, OnePixelWindowService::class.java))
        if (BuildConfig.DEBUG)
            Settings.System.putInt(contentResolver, "screen_off_timeout", Int.MAX_VALUE)
        loadApp()
        updateAllState()
        val test = "isDemoUser:${userManager?.isDemoUser} " +
                "isManagedProfile:${userManager?.isManagedProfile} " +
                "isDemoUser:${userManager?.isDemoUser} " +
                "isSystemUser：${userManager?.isSystemUser} " +
                "isUserAGoat：${userManager?.isUserAGoat} " +
                "isUserUnlocked：${userManager?.isUserUnlocked} " +
                "isQuietModeEnabled：${
                    userManager?.isQuietModeEnabled(
                        UserHandle.getUserHandleForUid(
                            0
                        )
                    )
                }"
        test.logD()
        if (isDebug()) setUSBDataDisabled(this, false)
    }

    private fun loadApp() {

    }

    private fun observer(name: String, block: (Boolean) -> Unit) {
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(name),
            false,
            oo { block(it) })
    }

    private fun oo(block: (Boolean) -> Unit): ContentObserver {
        return object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                block(selfChange)
            }
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            "接受到广播:${intent?.action}".logD()
            when (intent?.action) {
                //数据流量状态
                ConnectivityManager.CONNECTIVITY_ACTION -> updateData()
                //热点状态
                WIFI_AP_STATE_CHANGED_ACTION -> updateAP()
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {}
                //wifi状态
                WifiManager.WIFI_STATE_CHANGED_ACTION ->
                    updateWIFI(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0))
                //蓝牙状态
                BluetoothAdapter.ACTION_STATE_CHANGED ->
                    updateBluetooth(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0))
                //gps状态
                LocationManager.PROVIDERS_CHANGED_ACTION -> updateGPS()
                Intent.ACTION_PACKAGE_ADDED -> updateInstallAPK()
                Intent.ACTION_PACKAGE_REMOVED -> updateInstallAPK()
                Intent.ACTION_PACKAGE_REPLACED -> updateInstallAPK()
                Intent.ACTION_PACKAGE_CHANGED -> updateInstallAPK()
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> updateInstallAPK()
                Intent.ACTION_MEDIA_MOUNTED -> updateTFCard()
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
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        filter.addDataScheme("package")
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
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                mLocationManager!!.isLocationEnabled
            else mLocationManager!!.isProviderEnabled(Settings.System.LOCATION_PROVIDERS_ALLOWED))
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
//            mobile_data(this,true)
            showToast("根据安全策略，禁止使用数据流量")
        }
    }

    private fun updateAP() {
        if (isHotSpotDisabled(this) && isDisableHotSpot) {
            setHotSpotDisabled(this, true)
            showToast("根据安全策略，禁止使用热点")
        }
    }

    private fun updateADB() {
        if (isUSBDataDisabled(this) && isDisableUSBData) {
            setUSBDataDisabled(this, true)
            showToast("根据安全策略，禁止使用adb")
        }
    }

    private fun updateWindow() {
        if (isDisableScreenShot || isDisableScreenCapture) {
            isDisableScreenShotReceivedChange = true
            showToast("根据安全策略，禁止使用截屏")
        } else {
            isDisableScreenShotReceivedChange = false
        }
    }

    private fun updateTFCard() {
        if (isDisableTFCard) {
            unmount(this)
        }
    }

    private fun updateShare() {
        setSystemGlobal(
            this,
            ConfigEnum.DISABLE_SHARE.name.lowercase(Locale.ROOT),
            if (isDisableShare) "1" else "0"
        )
    }

    private fun updateSystemUpdate() {
        setSystemGlobal(
            this,
            ConfigEnum.DISABLE_SYSTEM_UPDATE.name.lowercase(Locale.ROOT),
            if (isDisableSystemUpdate) "1" else "0"
        )
        disable(UserManager.DISALLOW_SAFE_BOOT, isDisableBluetooth)
    }

    private fun updateMMS() {
        hiddenAPP("com.android.mms.service", isDisableMMS)
        hiddenAPP("com.android.mms", isDisableMMS)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        unregisterReceiver(stateReceiver)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    private fun updateAllState() {
        updateStatusBar()
        updateWIFI(wifiManager!!.wifiState)
        updateBluetooth(bluetoothAdapter!!.state)
        updateGPS()
        updateData()
        updateAP()
        updateADB()
        updateInstallAPK()
        updateWindow()
        updateTFCard()
        updateShare()
        updateSystemUpdate()
        updateMMS()
        try {
            if (!isDebug()) {
                disable(UserManager.DISALLOW_USB_FILE_TRANSFER, isDisableUSBData)
                disable(UserManager.DISALLOW_DEBUGGING_FEATURES, isDisableUSBData)
            }

            disable(UserManager.DISALLOW_BLUETOOTH, isDisableBluetooth)
            disable(UserManager.DISALLOW_CONFIG_BLUETOOTH, isDisableBluetooth)

            disable(UserManager.DISALLOW_CONFIG_WIFI, isDisableWIFI)

            disable(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, isDisableData)
            disable(UserManager.DISALLOW_DATA_ROAMING, isDisableData)
            disable(UserManager.DISALLOW_CONFIG_DATE_TIME, isDisableData)

            disable(UserManager.DISALLOW_CONFIG_LOCATION, isDisableGPS)
            disable(UserManager.DISALLOW_SHARE_LOCATION, isDisableGPS)

            disable(UserManager.DISALLOW_MICROPHONE_TOGGLE, isDisableMicrophone)
            disable(UserManager.DISALLOW_UNMUTE_MICROPHONE, isDisableMicrophone)

            disable(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, isDisableTFCard)
            disable(UserManager.DISALLOW_OUTGOING_CALLS, isDisablePhoneCall)
            disable(UserManager.DISALLOW_CONFIG_TETHERING, isDisableHotSpot)
            disable(UserManager.DISALLOW_SMS, isDisableSMS)
            disable(UserManager.DISALLOW_FACTORY_RESET, isDisableRestoreFactory)
            disable(UserManager.DISALLOW_INSTALL_APPS, isDisableInstallApp)
            disable(UserManager.DISALLOW_UNINSTALL_APPS, isDisableUnInstallApp)
            disable(UserManager.DISALLOW_CONTENT_CAPTURE, isDisableScreenShot)
            setScreenCaptureDisabled(this, App.componentName2, isDisableScreenShot)
            disable("no_camera", isDisableCamera)
            setCameraDisabled(this, App.componentName2, isDisableCamera)
            disableSensor(isDisableMicrophone, 1)
            disableSensor(isDisableCamera, 2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disable(key: String, isDisable: Boolean) {
        try {
            disableMDM(this, App.componentName2, key, isDisable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isDisable(key: String): Boolean = isDisableDMD(this, key)

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
        message.logD()
//        Looper.prepare()
        //RealtimeToast.makeText(this, message, Toast.LENGTH_SHORT).show()
//        Looper.loop()
    }
}