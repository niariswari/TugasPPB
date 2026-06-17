package com.example.thehairstory.data.repository

import com.example.thehairstory.data.local.dao.MemberDao
import com.example.thehairstory.data.local.dao.RewardDao
import com.example.thehairstory.data.local.dao.ServiceDao
import com.example.thehairstory.data.local.dao.TransactionDao
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.util.Locale

class MembershipRepositoryImpl(
    private val memberDao: MemberDao,
    private val transactionDao: TransactionDao,
    private val serviceDao: ServiceDao,
    private val rewardDao: RewardDao
) : MembershipRepository {

    // ─── Members ────────────────────────────────────────────────────────────

    override fun getAllMembers(): Flow<List<MemberEntity>> = memberDao.getAllMembers()

    override fun getMemberById(memberId: Int): Flow<MemberEntity?> = memberDao.getMemberById(memberId)

    override fun getMemberCount(): Flow<Int> = memberDao.getMemberCount()

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + String.format("%02x", it) }
    }

    override suspend fun registerMember(name: String, email: String, phone: String, password: String): Long {
        val cleanEmail = email.trim().lowercase(Locale.US)
        val cleanPhone = phone.trim()

        val existingEmail = memberDao.getMemberByEmail(cleanEmail)
        if (existingEmail != null) throw Exception("Email sudah terdaftar!")

        val existingPhone = memberDao.getMemberByPhone(cleanPhone)
        if (existingPhone != null) throw Exception("Nomor telepon sudah terdaftar!")

        val hashedPassword = hashPassword(password)

        // Insert with temp code, then update with MB + auto-ID
        val tempMember = MemberEntity(
            memberCode = "",
            name = name,
            email = cleanEmail,
            phone = cleanPhone,
            points = 0,
            earnedPoints = 0,
            password = hashedPassword
        )
        val memberId = memberDao.insertMember(tempMember)

        // MB0001, MB0002, ...
        val memberCode = "MB" + String.format(Locale.US, "%04d", memberId)
        val updatedMember = tempMember.copy(id = memberId.toInt(), memberCode = memberCode)
        memberDao.updateMember(updatedMember)

        return memberId
    }

    override suspend fun authenticateMember(input: String, password: String): MemberEntity? {
        val cleanInput = input.trim()
        val member = memberDao.getMemberByEmailOrPhone(cleanInput) ?: return null
        val hashedPassword = hashPassword(password)
        return if (member.password == hashedPassword) member else null
    }

    override suspend fun getMemberByCode(memberCode: String): MemberEntity? {
        val code = memberCode.trim()
        return memberDao.getMemberByCode(code)
            ?: memberDao.getMemberByCode(code.lowercase(Locale.US))
            ?: memberDao.getMemberByCode(code.uppercase(Locale.US))
    }

    override suspend fun getMemberByEmailOrPhone(input: String): MemberEntity? =
        memberDao.getMemberByEmailOrPhone(input.trim())

    // ─── Transactions ────────────────────────────────────────────────────────

    override fun getTransactionsForMember(memberId: Int): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForMember(memberId)

    override suspend fun addTransaction(
        memberId: Int,
        amount: Double,
        description: String,
        date: String,
        serviceCodes: String,
        rewardCodes: String
    ): Boolean {
        val member = memberDao.getMemberById(memberId).firstOrNull() ?: return false
        val pointsEarned = (amount / 10000).toInt()

        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                transactionCode = "",
                memberId = memberId,
                memberCode = member.memberCode,
                amount = amount,
                pointsEarned = pointsEarned,
                serviceCodes = serviceCodes,
                rewardCodes = rewardCodes,
                description = description,
                date = date
            )
        )
        // Update TX code after insert
        transactionDao.updateTransactionCode(txId.toInt(), "TX" + String.format(Locale.US, "%04d", txId))

        memberDao.updatePoints(memberId, member.points + pointsEarned)
        memberDao.updateEarnedPoints(memberId, member.earnedPoints + pointsEarned)
        return true
    }

    override suspend fun redeemReward(
        memberId: Int,
        pointsCost: Int,
        rewardDescription: String,
        date: String,
        rewardCodes: String
    ): Boolean {
        val member = memberDao.getMemberById(memberId).firstOrNull() ?: return false
        if (member.points < pointsCost) return false

        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                transactionCode = "",
                memberId = memberId,
                memberCode = member.memberCode,
                amount = 0.0,
                pointsEarned = -pointsCost,
                serviceCodes = "",
                rewardCodes = rewardCodes,
                description = "Redeem: $rewardDescription",
                date = date
            )
        )
        transactionDao.updateTransactionCode(txId.toInt(), "TX" + String.format(Locale.US, "%04d", txId))

        memberDao.updatePoints(memberId, member.points - pointsCost)
        return true
    }

    override suspend fun addTransactionWithRedeem(
        memberId: Int,
        amount: Double,
        description: String,
        pointsChange: Int,
        date: String,
        serviceCodes: String,
        rewardCodes: String
    ): Boolean {
        val member = memberDao.getMemberById(memberId).firstOrNull() ?: return false

        val txId = transactionDao.insertTransaction(
            TransactionEntity(
                transactionCode = "",
                memberId = memberId,
                memberCode = member.memberCode,
                amount = amount,
                pointsEarned = pointsChange,
                serviceCodes = serviceCodes,
                rewardCodes = rewardCodes,
                description = description,
                date = date
            )
        )
        transactionDao.updateTransactionCode(txId.toInt(), "TX" + String.format(Locale.US, "%04d", txId))

        // Available points (spendable, can decrease when redeemed)
        val newPoints = (member.points + pointsChange).coerceAtLeast(0)
        memberDao.updatePoints(memberId, newPoints)

        // Earned points (tier tracking — only increases from purchases)
        val earnedFromPurchase = (amount / 10000).toInt()
        if (earnedFromPurchase > 0) {
            memberDao.updateEarnedPoints(memberId, member.earnedPoints + earnedFromPurchase)
        }
        return true
    }

    // ─── Services ────────────────────────────────────────────────────────────

    override fun getAllServices(): Flow<List<ServiceEntity>> = serviceDao.getAllActiveServices()

    override suspend fun ensureServicesSeeded() {
        if (serviceDao.getServiceCount() > 0) return

        val catalog = listOf(
            ServiceEntity(name = "Haircut (Junior Stylist)",             price = 100000.0),
            ServiceEntity(name = "Haircut (Senior Stylist / Owner)",     price = 150000.0),
            ServiceEntity(name = "Hair Wash & Blow",                     price = 50000.0),
            ServiceEntity(name = "Hair Styling",                         price = 120000.0),
            ServiceEntity(name = "Creambath",                            price = 100000.0),
            ServiceEntity(name = "Hair Spa",                             price = 150000.0),
            ServiceEntity(name = "Hair Coloring",                        price = 500000.0),
            ServiceEntity(name = "Keratin Treatment",                    price = 700000.0),
            ServiceEntity(name = "Hair Smoothing",                       price = 800000.0)
        )
        catalog.forEach { service ->
            val id = serviceDao.insertService(service)
            serviceDao.updateService(
                service.copy(id = id.toInt(), serviceCode = "SV" + String.format(Locale.US, "%04d", id))
            )
        }
    }

    // ─── Rewards ─────────────────────────────────────────────────────────────

    override fun getAllRewards(): Flow<List<RewardEntity>> = rewardDao.getAllActiveRewards()

    override suspend fun ensureRewardsSeeded() {
        if (rewardDao.getRewardCount() > 0) return

        val catalog = listOf(
            // Standard rewards (cost points)
            RewardEntity(name = "Potongan Rp25.000", pointsCost = 100, discountValue = 25000.0, minTier = "BRONZE"),
            RewardEntity(name = "Gratis Hair Wash & Blow", pointsCost = 250, discountValue = 50000.0, minTier = "BRONZE"),
            RewardEntity(name = "Gratis Creambath", pointsCost = 500, discountValue = 100000.0, minTier = "BRONZE"),
            RewardEntity(name = "Gratis Hair Spa", pointsCost = 1000, discountValue = 150000.0, minTier = "BRONZE"),

            // Tier benefits (cost 0 points, but require specific tier)
            RewardEntity(name = "Voucher Ulang Tahun Rp25.000 (Benefit Bronze)", pointsCost = 0, discountValue = 25000.0, minTier = "BRONZE"),
            RewardEntity(name = "Voucher Ulang Tahun Rp50.000 (Benefit Silver)", pointsCost = 0, discountValue = 50000.0, minTier = "SILVER"),
            RewardEntity(name = "Voucher Ulang Tahun Rp75.000 (Benefit Gold)", pointsCost = 0, discountValue = 75000.0, minTier = "GOLD"),
            RewardEntity(name = "Gratis Minuman di Setiap Kunjungan (Benefit Gold)", pointsCost = 0, discountValue = 0.0, minTier = "GOLD"),
            RewardEntity(name = "Voucher Ulang Tahun Rp100.000 (Benefit Platinum)", pointsCost = 0, discountValue = 100000.0, minTier = "PLATINUM"),
            RewardEntity(name = "Gratis Hair Wash & Blow 1x per Tahun (Benefit Platinum)", pointsCost = 0, discountValue = 50000.0, minTier = "PLATINUM")
        )
        catalog.forEach { reward ->
            val id = rewardDao.insertReward(reward)
            rewardDao.updateReward(
                reward.copy(id = id.toInt(), rewardCode = "RW" + String.format(Locale.US, "%04d", id))
            )
        }
    }
}
