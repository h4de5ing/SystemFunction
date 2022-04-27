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
class PackageManagerList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "packageName") var packageName: String,
)