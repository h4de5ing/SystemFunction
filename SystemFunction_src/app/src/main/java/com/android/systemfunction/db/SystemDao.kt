package com.android.systemfunction.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SystemDao {
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

    @Query("SELECT * FROM packages order by id")
    fun observerPackagesList(): LiveData<MutableList<PackageList>>

    @Query("SELECT * FROM packages order by id")
    fun selectAllPackages(): MutableList<PackageList>

    @Query("SELECT * FROM packages where type=(:type) order by id")
    fun selectAllPackagesList(type: Int): MutableList<PackageList>

    @Insert
    fun insertPackages(vararg list: PackageList)

    @Update
    fun updatePackages(list: PackageList)

    @Query("DELETE FROM packages where type=(:type)")
    fun deletePackages(type: Int)
}