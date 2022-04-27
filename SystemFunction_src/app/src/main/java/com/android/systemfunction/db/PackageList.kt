package com.android.systemfunction.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packages", indices = [Index(
        value = ["type", "packageName"],
        unique = true
    )]
)
class PackageList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "packageName") var packageName: String,
    @ColumnInfo(name = "disable_install") var disable_install: Int,
    @ColumnInfo(name = "install") var install: Int,
    @ColumnInfo(name = "disable_uninstall") var disable_uninstall: Int,
    @ColumnInfo(name = "persistent") var persistent: Int,
    @ColumnInfo(name = "super_white") var super_white: Int,
)