package com.android.systemfunction.bean

data class AppBean(
    var name: String,
    val packageName: String,
    val mainActivity: String,
    val icon: ByteArray
)
