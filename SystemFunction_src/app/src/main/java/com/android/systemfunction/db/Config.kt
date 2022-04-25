package com.android.systemfunction.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "config", indices = [Index(
        value = ["id", "key"],
        unique = true
    )]
)
class Config(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "key") var key: String,
    @ColumnInfo(name = "value") var value: String,
) {
    override fun toString(): String {
        return "(id=$id, $key='$value')"
    }
}