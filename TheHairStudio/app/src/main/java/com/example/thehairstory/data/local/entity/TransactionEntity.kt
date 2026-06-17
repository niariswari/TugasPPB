package com.example.thehairstory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionCode: String = "",  // TX0001, TX0002, ...
    val memberId: Int,
    val memberCode: String = "",       // MB0001 — for easy reading
    val amount: Double,
    val pointsEarned: Int,
    val serviceCodes: String = "",     // "SV0001,SV0003" — comma-separated service codes
    val rewardCodes: String = "",      // "RW0002" — comma-separated reward codes
    val description: String,           // Human-readable summary
    val date: String
)
