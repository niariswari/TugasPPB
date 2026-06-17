package com.example.thehairstory.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "members",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone"], unique = true)
    ]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val memberCode: String,
    val name: String,
    val email: String,
    val phone: String,
    val points: Int = 0,           // Available points (can be spent on rewards)
    val earnedPoints: Int = 0,     // Total points EVER earned (for tier calculation, never decremented)
    val password: String = ""
)
