package com.example.thehairstory.data.repository

import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface MembershipRepository {
    // Members
    fun getAllMembers(): Flow<List<MemberEntity>>
    fun getMemberById(memberId: Int): Flow<MemberEntity?>
    fun getMemberCount(): Flow<Int>
    suspend fun registerMember(name: String, email: String, phone: String, password: String): Long
    suspend fun getMemberByEmailOrPhone(input: String): MemberEntity?
    suspend fun getMemberByCode(memberCode: String): MemberEntity?
    suspend fun authenticateMember(input: String, password: String): MemberEntity?

    // Transactions
    fun getTransactionsForMember(memberId: Int): Flow<List<TransactionEntity>>
    suspend fun addTransaction(
        memberId: Int,
        amount: Double,
        description: String,
        date: String,
        serviceCodes: String = "",
        rewardCodes: String = ""
    ): Boolean
    suspend fun redeemReward(
        memberId: Int,
        pointsCost: Int,
        rewardDescription: String,
        date: String,
        rewardCodes: String = ""
    ): Boolean
    suspend fun addTransactionWithRedeem(
        memberId: Int,
        amount: Double,
        description: String,
        pointsChange: Int,
        date: String,
        serviceCodes: String = "",
        rewardCodes: String = ""
    ): Boolean

    // Services
    fun getAllServices(): Flow<List<ServiceEntity>>
    suspend fun ensureServicesSeeded()

    // Rewards
    fun getAllRewards(): Flow<List<RewardEntity>>
    suspend fun ensureRewardsSeeded()
}
