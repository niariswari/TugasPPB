package com.example.thehairstory.ui.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thehairstory.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: MembershipRepository) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun onNameChange(newValue: String) {
        _name.value = newValue
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onPhoneChange(newValue: String) {
        _phone.value = newValue
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
    }

    fun register() {
        val currentName = _name.value.trim()
        val currentEmail = _email.value.trim()
        val currentPhone = _phone.value.trim()
        val currentPassword = _password.value.trim()

        if (currentName.isEmpty() || currentEmail.isEmpty() || currentPhone.isEmpty() || currentPassword.isEmpty()) {
            _registrationState.value = RegistrationState.Error("Semua kolom pendaftaran wajib diisi!")
            return
        }

        if (!isValidEmail(currentEmail)) {
            _registrationState.value = RegistrationState.Error("Format email tidak valid!")
            return
        }

        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                repository.registerMember(currentName, currentEmail, currentPhone, currentPassword)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun resetState() {
        _name.value = ""
        _email.value = ""
        _phone.value = ""
        _password.value = ""
        _registrationState.value = RegistrationState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return email.matches(emailRegex.toRegex())
    }
}

sealed interface RegistrationState {
    object Idle : RegistrationState
    object Loading : RegistrationState
    object Success : RegistrationState
    data class Error(val message: String) : RegistrationState
}
