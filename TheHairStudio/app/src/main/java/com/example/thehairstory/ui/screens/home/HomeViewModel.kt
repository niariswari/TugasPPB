package com.example.thehairstory.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thehairstory.data.repository.MembershipRepository
import com.example.thehairstory.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(private val repository: MembershipRepository) : ViewModel() {

    val allMembers: StateFlow<List<MemberEntity>> = repository.getAllMembers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val memberCount: StateFlow<Int> = repository.getMemberCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    suspend fun getMemberByCode(memberCode: String): MemberEntity? {
        return repository.getMemberByCode(memberCode)
    }
}
