package com.android.systemfunction.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.github.h4de5ing.gsoncommon.JsonUtils
import com.github.h4de5ing.gsoncommon.fromJson
import com.github.h4de5ing.gsoncommon.toJson

@Entity(
    tableName = "packages", indices = [Index(
        value = ["type"],
        unique = true
    )]
)
class PackageList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "packages") var packages: String,//存储包名列表
) {
    //获取包名列表
    fun getPackageList(): List<String> {
        return try {
            JsonUtils.getJsonParser().fromJson(packages)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setPackageList(list: List<String>): PackageList {
        this.packages = list.toJson()
        return this
    }
}