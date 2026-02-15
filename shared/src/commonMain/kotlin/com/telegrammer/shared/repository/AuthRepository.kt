package com.telegrammer.shared.repository

import com.telegrammer.shared.api.AuthApi
import com.telegrammer.shared.api.KeyApi
import com.telegrammer.shared.crypto.KeyManager
import com.telegrammer.shared.model.AuthState
import com.telegrammer.shared.platform.SecureStorage
import com.telegrammer.shared.platform.StorageKeys

class AuthRepository(
    private val authApi: AuthApi,
    private val storage: SecureStorage,
    private val keyManager: KeyManager,
    private val keyApi: KeyApi
) {
    fun isLoggedIn(): Boolean =
        storage.getString(StorageKeys.ACCESS_TOKEN) != null

    fun getUserId(): String? =
        storage.getString(StorageKeys.USER_ID)

    fun getAccessToken(): String? =
        storage.getString(StorageKeys.ACCESS_TOKEN)

    suspend fun requestOtp(phoneNumber: String): AuthState {
        val result = authApi.requestOtp(phoneNumber)
        return if (result.isSuccess) {
            AuthState.OtpSent(phoneNumber)
        } else {
            AuthState.Error(result.exceptionOrNull()?.message ?: "Failed to send OTP")
        }
    }

    suspend fun verifyOtp(phoneNumber: String, code: String): AuthState {
        val result = authApi.verifyOtp(phoneNumber, code)
        return result.fold(
            onSuccess = { tokens ->
                storage.putString(StorageKeys.ACCESS_TOKEN, tokens.accessToken)
                storage.putString(StorageKeys.REFRESH_TOKEN, tokens.refreshToken)
                storage.putString(StorageKeys.USER_ID, tokens.userId)

                // Generate E2E keys and upload bundle after login
                try {
                    keyManager.initialize()
                    val bundle = keyManager.generateUploadBundle()
                    keyApi.uploadBundle(bundle)
                } catch (_: Exception) {
                    // Key upload failed — will retry on next app launch
                }

                AuthState.Authenticated(tokens.userId)
            },
            onFailure = { e ->
                AuthState.Error(e.message ?: "Verification failed")
            }
        )
    }

    suspend fun refreshToken(): Boolean {
        val refreshToken = storage.getString(StorageKeys.REFRESH_TOKEN) ?: return false
        val result = authApi.refresh(refreshToken)
        return result.fold(
            onSuccess = { tokens ->
                storage.putString(StorageKeys.ACCESS_TOKEN, tokens.accessToken)
                storage.putString(StorageKeys.REFRESH_TOKEN, tokens.refreshToken)
                true
            },
            onFailure = { false }
        )
    }

    fun logout() {
        storage.clear()
    }
}
