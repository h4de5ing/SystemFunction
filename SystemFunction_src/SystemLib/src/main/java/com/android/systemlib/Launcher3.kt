package com.android.systemlib

import android.content.Context
import android.content.pm.LauncherApps

object Launcher3 {
    fun cleanDefaultLauncher(context: Context) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
    }
}