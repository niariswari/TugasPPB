package com.example.thehairstory.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thehairstory.data.repository.MembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: MembershipRepository
) : ViewModel() {

    // Login States
    private val _loginInput = MutableStateFlow("")
    val loginInput: StateFlow<String> = _loginInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    // Registration States
    private val _registerName = MutableStateFlow("")
    val registerName: StateFlow<String> = _registerName.asStateFlow()

    private val _registerEmail = MutableStateFlow("")
    val registerEmail: StateFlow<String> = _registerEmail.asStateFlow()

    private val _registerPhone = MutableStateFlow("")
    val registerPhone: StateFlow<String> = _registerPhone.asStateFlow()

    private val _registerPassword = MutableStateFlow("")
    val registerPassword: StateFlow<String> = _registerPassword.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering.asStateFlow()

    fun onInputChange(value: String) {
        _loginInput.value = value
        _errorMessage.value = null // Clear error when typing
    }

    fun onPasswordChange(value: String) {
        _loginPassword.value = value
        _errorMessage.value = null
    }

    fun onRegisterNameChange(value: String) {
        _registerName.value = value
        _registerError.value = null
    }

    fun onRegisterEmailChange(value: String) {
        _registerEmail.value = value
        _registerError.value = null
    }

    fun onRegisterPhoneChange(value: String) {
        _registerPhone.value = value
        _registerError.value = null
    }

    fun onRegisterPasswordChange(value: String) {
        _registerPassword.value = value
        _registerError.value = null
    }

    fun clearErrors() {
        _errorMessage.value = null
        _registerError.value = null
    }

    fun checkUsername(onResult: (exists: Boolean) -> Unit) {
        val input = _loginInput.value.trim()
        if (input.isEmpty()) {
            _errorMessage.value = "Input tidak boleh kosong!"
            onResult(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            if (input.lowercase() == "admin") {
                _isLoading.value = false
                onResult(true)
            } else {
                val member = repository.getMemberByEmailOrPhone(input)
                _isLoading.value = false
                if (member != null) {
                    onResult(true)
                } else {
                    _errorMessage.value = "Username/Member tidak ditemukan!"
                    onResult(false)
                }
            }
        }
    }

    fun performLogin(onSuccess: (isStaff: Boolean, memberId: Int?) -> Unit) {
        val input = _loginInput.value.trim()
        if (input.isEmpty()) {
            _errorMessage.value = "Input tidak boleh kosong!"
            return
        }

        val password = _loginPassword.value
        if (password.isEmpty()) {
            _errorMessage.value = "Password tidak boleh kosong!"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            if (input.lowercase() == "admin") {
                _isLoading.value = false
                if (password == "admin123") {
                    onSuccess(true, null)
                } else {
                    _errorMessage.value = "Password admin salah!"
                }
            } else {
                val member = repository.authenticateMember(input, password)
                _isLoading.value = false
                if (member != null) {
                    onSuccess(false, member.id)
                } else {
                    _errorMessage.value = "Username atau Password salah!"
                }
            }
        }
    }

    fun performRegister(onSuccess: (memberId: Int) -> Unit) {
        val name = _registerName.value.trim()
        val email = _registerEmail.value.trim()
        val phone = _registerPhone.value.trim()
        val password = _registerPassword.value.trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            _registerError.value = "Semua kolom pendaftaran wajib diisi!"
            return
        }

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        if (!email.matches(emailRegex.toRegex())) {
            _registerError.value = "Format email tidak valid!"
            return
        }

        viewModelScope.launch {
            _isRegistering.value = true
            _registerError.value = null
            try {
                val memberId = repository.registerMember(name, email, phone, password)
                _isRegistering.value = false
                // Reset fields
                _registerName.value = ""
                _registerEmail.value = ""
                _registerPhone.value = ""
                _registerPassword.value = ""
                onSuccess(memberId.toInt())
            } catch (e: Exception) {
                _isRegistering.value = false
                _registerError.value = e.message ?: "Terjadi kesalahan pendaftaran!"
            }
        }
    }
}
