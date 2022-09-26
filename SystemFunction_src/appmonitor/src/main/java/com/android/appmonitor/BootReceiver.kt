package com.android.settingc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startService(Intent(context, ForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}