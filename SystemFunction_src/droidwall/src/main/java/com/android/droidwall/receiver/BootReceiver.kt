package com.android.droidwall.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.droidwall.services.DForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startService(Intent(context, DForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}