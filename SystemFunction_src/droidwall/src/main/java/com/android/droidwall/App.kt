package com.android.droidwall

import android.app.Application
import android.os.INetworkManagementService
import android.os.ServiceManager
import com.android.droidwall.db.FWDao
import com.android.droidwall.db.FWDatabase
import com.android.droidwall.utils.configs

class App : Application() {
    companion object {
        lateinit var fwDao: FWDao
        var iNetD: INetworkManagementService? = null
    }

    override fun onCreate() {
        super.onCreate()
        val db = FWDatabase.create(this)
        fwDao = db.fwDao()
        configs = fwDao.selectAllConfig()
        iNetD =
            INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"))
    }
}