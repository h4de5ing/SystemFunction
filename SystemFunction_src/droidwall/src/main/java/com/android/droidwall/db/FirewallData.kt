package com.android.droidwall.db

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list",
    indices = [Index(value = ["uid"], unique = true)]
)
data class FirewallData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "uid") var uid: Int,
    @ColumnInfo(name = "isWhite") var isWhite: Boolean,
) {
    @Ignore
    var appName = ""

    @Ignore
    var icon: Drawable? = null
}