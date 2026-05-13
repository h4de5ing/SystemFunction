package com.android.systemlib

import android.os.ServiceManager
import android.view.IWindowManager

/**
 * 这个工具类主要用来处理屏幕相关适配
 * 多屏，分屏等应用场景
 */

fun setWindowManager2() {
    IWindowManager.Stub.asInterface(ServiceManager.getService("window"))
}