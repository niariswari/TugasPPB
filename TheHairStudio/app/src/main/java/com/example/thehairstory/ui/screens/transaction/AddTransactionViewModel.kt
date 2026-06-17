package com.example.thehairstory.ui.screens.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionViewModel(private val repository: MembershipRepository) : ViewModel() {

    // Services and rewards loaded from DB
    val servicesCatalog: StateFlow<List<ServiceEntity>> = repository.getAllServices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dbRewards = repository.getAllRewards()
    private val _member = MutableStateFlow<MemberEntity?>(null)
    val member: StateFlow<MemberEntity?> = _member.asStateFlow()

    private val _memberTransactions = MutableStateFlow<List<com.example.thehairstory.data.local.entity.TransactionEntity>>(emptyList())
    val memberTransactions: StateFlow<List<com.example.thehairstory.data.local.entity.TransactionEntity>> = _memberTransactions.asStateFlow()

    val rewardsCatalog: StateFlow<List<RewardEntity>> = kotlinx.coroutines.flow.combine(
        _dbRewards,
        _member,
        _memberTransactions
    ) { dbRewards, currentMember, txList ->
        if (currentMember == null) {
            dbRewards
        } else {
            val earned = currentMember.earnedPoints
            val memberTier = when {
                earned >= 800 -> "PLATINUM"
                earned >= 300 -> "GOLD"
                earned >= 100 -> "SILVER"
                else -> "BRONZE"
            }
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val oneYearAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.time

            dbRewards.filter { reward ->
                val pointsCost = reward.pointsCost
                val isTierBenefit = pointsCost == 0

                if (isTierBenefit) {
                    // Tier benefit (pointsCost == 0)
                    val isVoucher = reward.name.contains("Voucher", ignoreCase = true)
                    if (isVoucher) {
                        reward.minTier.uppercase() == memberTier
                    } else {
                        getTierLevel(memberTier) >= getTierLevel(reward.minTier)
                    }
                } else {
                    // Standard reward: available to everyone
                    true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getTierLevel(tier: String): Int = when(tier.uppercase()) {
        "PLATINUM" -> 3
        "GOLD" -> 2
        "SILVER" -> 1
        else -> 0
    }

    private val _selectedServices = MutableStateFlow<Map<ServiceEntity, Int>>(emptyMap())
    val selectedServices: StateFlow<Map<ServiceEntity, Int>> = _selectedServices.asStateFlow()

    private val _selectedRewards = MutableStateFlow<List<RewardEntity>>(emptyList())
    val selectedRewards: StateFlow<List<RewardEntity>> = _selectedRewards.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    fun loadMember(memberId: Int) {
        viewModelScope.launch {
            repository.getMemberById(memberId).collect {
                _member.value = it
                validateSelectedRewards()
            }
        }
        viewModelScope.launch {
            repository.getTransactionsForMember(memberId).collect {
                _memberTransactions.value = it
            }
        }
    }

    fun toggleService(service: ServiceEntity) {
        val currentMap = _selectedServices.value.toMutableMap()
        if (currentMap.containsKey(service)) {
            currentMap.remove(service)
        } else {
            currentMap[service] = 1
        }
        _selectedServices.value = currentMap
    }

    fun incrementService(service: ServiceEntity) {
        val currentMap = _selectedServices.value.toMutableMap()
        currentMap[service] = (currentMap[service] ?: 0) + 1
        _selectedServices.value = currentMap
    }

    fun decrementService(service: ServiceEntity) {
        val currentMap = _selectedServices.value.toMutableMap()
        val count = currentMap[service] ?: 0
        if (count <= 1) currentMap.remove(service) else currentMap[service] = count - 1
        _selectedServices.value = currentMap
    }

    fun toggleReward(reward: RewardEntity) {
        val memberVal = _member.value ?: return
        val currentList = _selectedRewards.value.toMutableList()
        if (currentList.contains(reward)) {
            currentList.remove(reward)
        } else {
            val totalCost = currentList.sumOf { it.pointsCost } + reward.pointsCost
            if (memberVal.points >= totalCost) {
                currentList.add(reward)
            }
        }
        _selectedRewards.value = currentList
    }

    private fun validateSelectedRewards() {
        val memberVal = _member.value ?: return
        val currentSelected = _selectedRewards.value.toMutableList()
        while (currentSelected.sumOf { it.pointsCost } > memberVal.points) {
            if (currentSelected.isNotEmpty()) currentSelected.removeAt(currentSelected.lastIndex)
            else break
        }
        _selectedRewards.value = currentSelected
    }

    fun submitTransaction(memberId: Int) {
        val servicesMap = _selectedServices.value
        if (servicesMap.isEmpty()) {
            _transactionState.value = TransactionState.Error("Pilih minimal satu jenis perawatan!")
            return
        }

        val subtotal = servicesMap.entries.sumOf { (svc, qty) -> svc.price * qty }
        val selectedRewardsList = _selectedRewards.value

        // 1x per year validation upon transaction submission
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val oneYearAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.time
        val txList = _memberTransactions.value

        for (reward in selectedRewardsList) {
            val isTierBenefit = reward.pointsCost == 0
            val isFreeDrinks = reward.name.contains("Minuman", ignoreCase = true) || reward.name.contains("Drink", ignoreCase = true)
            if (isTierBenefit && !isFreeDrinks) {
                val isClaimedLastYear = txList.any { tx ->
                    val isWithinYear = try {
                        val txDateOnly = tx.date.substring(0, minOf(10, tx.date.length))
                        val parsedDate = sdf.parse(txDateOnly)
                        parsedDate != null && parsedDate.after(oneYearAgo)
                    } catch (e: Exception) {
                        false
                    }
                    if (isWithinYear) {
                        val hasCode = tx.rewardCodes.split(",").map { it.trim() }.contains(reward.rewardCode)
                        val hasName = tx.description.contains(reward.name, ignoreCase = true)
                        hasCode || hasName
                    } else {
                        false
                    }
                }
                if (isClaimedLastYear) {
                    _transactionState.value = TransactionState.Error("Reward ${reward.name} can only be claimed once a year.")
                    return
                }
            }
        }

        val discount = selectedRewardsList.sumOf { it.discountValue }
        val grandTotal = maxOf(0.0, subtotal - discount)

        val pointsEarned = (grandTotal / 10000).toInt()
        val pointsCost = selectedRewardsList.sumOf { it.pointsCost }
        val netPointsChange = pointsEarned - pointsCost

        // Build description with service codes
        val serviceDesc = servicesMap.entries.joinToString("\n") { (svc, qty) ->
            if (qty > 1) "${svc.serviceCode} ${svc.name} x$qty"
            else "${svc.serviceCode} ${svc.name}"
        }
        val rewardDesc = if (selectedRewardsList.isNotEmpty()) {
            "\n[Redeem: " + selectedRewardsList.joinToString(", ") { "${it.rewardCode} ${it.name}" } + "]"
        } else ""
        val description = serviceDesc + rewardDesc

        // Collect codes for storage
        val serviceCodes = servicesMap.keys.joinToString(",") { it.serviceCode }
        val rewardCodes = selectedRewardsList.joinToString(",") { it.rewardCode }

        _transactionState.value = TransactionState.Loading

        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val currentDateStr = dateFormat.format(Date())

                val success = repository.addTransactionWithRedeem(
                    memberId = memberId,
                    amount = grandTotal,
                    description = description,
                    pointsChange = netPointsChange,
                    date = currentDateStr,
                    serviceCodes = serviceCodes,
                    rewardCodes = rewardCodes
                )
                if (success) {
                    _transactionState.value = TransactionState.Success
                } else {
                    _transactionState.value = TransactionState.Error("Gagal menambahkan transaksi. Member tidak ditemukan.")
                }
            } catch (e: Exception) {
                _transactionState.value = TransactionState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun resetState() {
        _selectedServices.value = emptyMap()
        _selectedRewards.value = emptyList()
        _transactionState.value = TransactionState.Idle
    }
}

sealed interface TransactionState {
    object Idle : TransactionState
    object Loading : TransactionState
    object Success : TransactionState
    data class Error(val message: String) : TransactionState
}
