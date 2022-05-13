package com.android.systemfunction.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.systemfunction.ForegroundService
import com.android.systemfunction.app.App
import com.github.h4de5ing.baseui.logD

class BootReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        "MDM BootReceiver ${intent.action}".logD()
        context.startService(Intent(context, ForegroundService::class.java))
    }
}