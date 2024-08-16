package com.android.droidwall.utils

import com.android.droidwall.App.Companion.fwDao
import com.android.droidwall.db.FirewallData


var configs = mutableListOf<FirewallData>()
fun updateKT(uid: Int, isWhite: Boolean) {
    configs.firstOrNull { it.uid == uid }?.apply {
        this.isWhite = isWhite
        this.update()
    } ?: FirewallData(0, uid, isWhite).insert()
    configs = fwDao.selectAllConfig()
}

fun insert2DB(uid: Int, isWhite: Boolean) {
    try {
        fwDao.insert(FirewallData(0, uid, isWhite))
    } catch (_: Exception) {
    }
}

private fun FirewallData.update() = fwDao.update(this)
private fun FirewallData.insert() = fwDao.insert(this)