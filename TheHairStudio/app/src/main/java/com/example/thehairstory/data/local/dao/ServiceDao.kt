package com.example.thehairstory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thehairstory.data.local.entity.ServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertService(service: ServiceEntity): Long

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Query("SELECT * FROM services WHERE isActive = 1 ORDER BY id ASC")
    fun getAllActiveServices(): Flow<List<ServiceEntity>>

    @Query("SELECT COUNT(*) FROM services")
    suspend fun getServiceCount(): Int
}
