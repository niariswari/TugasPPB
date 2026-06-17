package com.example.thehairstory.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thehairstory.data.repository.MembershipRepository
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.TransactionEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MemberDetailViewModel(
    private val repository: MembershipRepository
) : ViewModel() {

    val servicesCatalog: StateFlow<List<ServiceEntity>> = repository.getAllServices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewardsCatalog: StateFlow<List<RewardEntity>> = repository.getAllRewards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _memberId = MutableStateFlow<Int?>(null)

    val member: StateFlow<MemberEntity?> = _memberId.flatMapLatest { id ->
        if (id != null) {
            repository.getMemberById(id)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val transactions: StateFlow<List<TransactionEntity>> = _memberId.flatMapLatest { id ->
        if (id != null) {
            repository.getTransactionsForMember(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMemberId(id: Int) {
        _memberId.value = id
    }
}
