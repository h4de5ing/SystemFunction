package com.android.droidwall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.droidwall.services.ForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startService(Intent(context, ForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}