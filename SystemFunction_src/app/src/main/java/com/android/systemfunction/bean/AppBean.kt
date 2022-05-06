package com.android.systemfunction.bean

data class AppBean(
    var name: String,
    val packageName: String,
    val mainActivity: String,
    val icon: ByteArray
) {
    override fun toString(): String {
        return "AppBean(name='$name', packageName='$packageName', mainActivity='$mainActivity')"
    }
}
