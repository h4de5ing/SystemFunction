package com.android.systemlib

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

fun getStatusBarHeight(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= 30) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets = windowMetrics.windowInsets
        val insets =
            windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
        insets.top
    } else {//TODO 以前怎么获取的高度
        0
    }
}