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
import com.zhangyf.library.utils.SPUtils
import com.zhangyf.library.utils.TotpUtil

class App : Application() {
    companion object {
        lateinit var systemDao: SystemDao
        lateinit var application: Application
        lateinit var componentName2: ComponentName
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        systemDao = SystemDatabase.create(this).barcodeDao()
        firstUpdate(systemDao.selectAllConfig())
        firstUpdatePackage(systemDao.selectAllPackages())
        //google二次验证
        SPUtils.init(this)
        TotpUtil.init("FZ6S5VB64HVSYLJN")// 初始化SEED
        //google二次验证
        componentName2 = ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)
        initLog(isDebug(), "gh0st")
        CrashHandler.getInstance().init(this)
    }
}