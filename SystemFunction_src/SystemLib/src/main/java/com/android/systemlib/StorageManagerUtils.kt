package com.android.systemlib

import android.app.usage.IStorageStatsManager
import android.os.ServiceManager
import android.os.storage.IStorageManager

fun getStorage() {
    val iStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("storage"))
    iStorageManager.mount("")
}

fun getStorageStats() {
    val iStorageStatsManager =
        IStorageStatsManager.Stub.asInterface(ServiceManager.getService("storagestats"))
}

fun getStorageMount() {
    val iStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"))
}