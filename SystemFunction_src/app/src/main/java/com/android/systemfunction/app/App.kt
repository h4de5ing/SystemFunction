package com.android.systemfunction.app

import android.app.Application
import android.content.ComponentName
import com.android.systemfunction.AdminReceiver
import com.android.systemfunction.BuildConfig
import com.android.systemfunction.db.SystemDao
import com.android.systemfunction.db.SystemDatabase
import com.android.systemfunction.utils.CrashHandler
import com.android.systemfunction.utils.firstUpdate
import com.android.systemfunction.utils.firstUpdatePackage
import com.android.systemfunction.utils.isDebug
import com.github.h4de5ing.baseui.initLog

class App : Application() {
    companion object {
        lateinit var db: SystemDatabase //App.db.runInTransaction {  }
        lateinit var systemDao: SystemDao
        lateinit var application: Application
        lateinit var componentName2: ComponentName
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        db = SystemDatabase.create(this)
        systemDao = db.barcodeDao()
        firstUpdate(systemDao.selectAllConfig())
        firstUpdatePackage(systemDao.selectAllPackages())
        componentName2 = ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)
        initLog(isDebug(), "gh0st")
        CrashHandler.getInstance().init(this)
    }
}