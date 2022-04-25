package com.android.systemfunction.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SystemDao {
    //配置文件
    @Query("SELECT * FROM config order by id")
    fun selectAllConfig(): MutableList<Config>

    @Query("SELECT * FROM config order by id")
    fun observerConfigChange(): LiveData<MutableList<Config>>

    @Query("SELECT * FROM config where `key`=(:key)")
    fun selectConfigFromKey(key: String): MutableList<Config>

    @Insert
    fun insertConfig(vararg config: Config)

    @Update
    fun updateConfig(config: Config)

    @Query("DELETE FROM config")
    fun deleteAllConfig()
}