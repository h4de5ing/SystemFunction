package com.android.systemfunction.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.UserManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.systemfunction.R
import com.android.systemlib.isDisableDMD

class OemConfigService : TileService() {

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onClick() {
        super.onClick()
        val open = isDisableDMD(this, UserManager.DISALLOW_INSTALL_APPS)
        qsTile.state = if (open) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
        qsTile.icon = Icon.createWithResource(this, R.drawable.ic_baseline_apps_24)
        qsTile.label = "APP"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = "This is a TileService"
        }
    }
}