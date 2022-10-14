package com.android.droidwall.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FWDao {
    @Query("SELECT * FROM list order by id")
    fun selectAllConfig(): MutableList<FirewallData>

    @Query("SELECT * FROM list order by id")
    fun observerConfigChange(): LiveData<MutableList<FirewallData>>

    @Insert
    fun insert(vararg config: FirewallData)

    @Update
    fun update(config: FirewallData)

    @Query("DELETE FROM list")
    fun deleteAllConfig()
}