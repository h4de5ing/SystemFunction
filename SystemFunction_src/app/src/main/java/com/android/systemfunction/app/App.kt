package com.android.systemfunction.app

import android.app.Application
import com.android.systemfunction.db.SystemDao
import com.android.systemfunction.db.SystemDatabase
import com.android.systemfunction.utils.firstUpdate

class App : Application() {
    companion object {
        lateinit var systemDao: SystemDao
        lateinit var application: Application
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        systemDao = SystemDatabase.create(this).barcodeDao()
        firstUpdate(systemDao.selectAllConfig())
    }
}