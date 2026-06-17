package com.example.thehairstory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val rewardCode: String = "",  // RW0001, RW0002, ...
    val name: String,
    val pointsCost: Int,
    val discountValue: Double,
    val minTier: String = "BRONZE",
    val isActive: Boolean = true
)
