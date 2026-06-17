package com.example.thehairstory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thehairstory.data.local.entity.RewardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReward(reward: RewardEntity): Long

    @Update
    suspend fun updateReward(reward: RewardEntity)

    @Query("SELECT * FROM rewards WHERE isActive = 1 ORDER BY pointsCost ASC")
    fun getAllActiveRewards(): Flow<List<RewardEntity>>

    @Query("SELECT COUNT(*) FROM rewards")
    suspend fun getRewardCount(): Int
}
