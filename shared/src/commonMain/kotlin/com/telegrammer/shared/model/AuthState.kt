package com.telegrammer.shared.model

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class OtpSent(val phoneNumber: String) : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
