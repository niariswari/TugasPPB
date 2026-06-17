package com.example.thehairstory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceCode: String = "",  // SV0001, SV0002, ...
    val name: String,
    val price: Double,
    val isActive: Boolean = true
)
