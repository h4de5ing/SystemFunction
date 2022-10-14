package com.android.droidwall.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.droidwall.services.ForegroundService

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startService(Intent(context, ForegroundService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}