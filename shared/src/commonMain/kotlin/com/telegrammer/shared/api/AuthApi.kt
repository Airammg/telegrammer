package com.telegrammer.shared.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequest(val phoneNumber: String)

@Serializable
data class VerifyOtpRequest(val phoneNumber: String, val code: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)

class AuthApi(private val client: HttpClient) {

    suspend fun requestOtp(phoneNumber: String): Result<Unit> = runCatching {
        client.post("/auth/request-otp") {
            setBody(RequestOtpRequest(phoneNumber))
        }
        Unit
    }

    suspend fun verifyOtp(phoneNumber: String, code: String): Result<AuthTokensResponse> = runCatching {
        client.post("/auth/verify-otp") {
            setBody(VerifyOtpRequest(phoneNumber, code))
        }.body()
    }

    suspend fun refresh(refreshToken: String): Result<AuthTokensResponse> = runCatching {
        client.post("/auth/refresh") {
            setBody(RefreshRequest(refreshToken))
        }.body()
    }
}
