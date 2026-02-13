package com.telegrammer.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.model.AuthState
import com.telegrammer.shared.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun requestOtp(phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _state.value = authRepo.requestOtp(phoneNumber)
            _isLoading.value = false
        }
    }

    fun verifyOtp(phoneNumber: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _state.value = authRepo.verifyOtp(phoneNumber, code)
            _isLoading.value = false
        }
    }
}
