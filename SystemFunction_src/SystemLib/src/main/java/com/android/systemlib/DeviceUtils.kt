package com.android.systemlib

import android.content.Context

/**
 * 主要用户设备信息获取，用于判断类型
 */

/**
 * 电视？
 */
fun isTelevision(context: Context): Boolean =
    context.packageManager.hasSystemFeature("android.software.leanback")

/**
 * 表？
 */
fun isWear(context: Context): Boolean =
    context.packageManager.hasSystemFeature("android.hardware.type.watch")

/**
 * 車？
 */
fun isAuto(context: Context): Boolean =
    context.packageManager.hasSystemFeature("android.hardware.type.automotive")
