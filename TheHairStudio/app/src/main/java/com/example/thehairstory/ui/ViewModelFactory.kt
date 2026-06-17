package com.example.thehairstory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thehairstory.data.repository.MembershipRepository
import com.example.thehairstory.ui.screens.home.HomeViewModel
import com.example.thehairstory.ui.screens.register.RegisterViewModel
import com.example.thehairstory.ui.screens.detail.MemberDetailViewModel
import com.example.thehairstory.ui.screens.transaction.AddTransactionViewModel
import com.example.thehairstory.ui.screens.login.LoginViewModel

class ViewModelFactory(
    private val repository: MembershipRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(repository) as T
            }
            modelClass.isAssignableFrom(MemberDetailViewModel::class.java) -> {
                MemberDetailViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AddTransactionViewModel::class.java) -> {
                AddTransactionViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
