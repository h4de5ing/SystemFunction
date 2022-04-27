package com.android.systemfunction.app

import android.app.Application
import android.content.ComponentName
import com.android.systemfunction.AdminReceiver
import com.android.systemfunction.BuildConfig
import com.android.systemfunction.bean.AppBean
import com.android.systemfunction.db.SystemDao
import com.android.systemfunction.db.SystemDatabase
import com.android.systemfunction.utils.firstUpdate
import com.android.systemfunction.utils.getAllApp
import com.zhangyf.library.utils.SPUtils
import com.zhangyf.library.utils.TotpUtil

class App : Application() {
    companion object {
        lateinit var systemDao: SystemDao
        lateinit var application: Application
        lateinit var list: List<AppBean>//TODO 暂时放这里
        lateinit var componentName2: ComponentName
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        systemDao = SystemDatabase.create(this).barcodeDao()
        firstUpdate(systemDao.selectAllConfig())
        list = getAllApp()
        SPUtils.init(this)
        TotpUtil.init("FZ6S5VB64HVSYLJN")// 初始化SEED
        componentName2 = ComponentName(BuildConfig.APPLICATION_ID, AdminReceiver::class.java.name)
    }
}