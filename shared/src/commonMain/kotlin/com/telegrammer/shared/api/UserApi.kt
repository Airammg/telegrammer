package com.telegrammer.shared.api

import com.telegrammer.shared.model.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null
)

class UserApi(private val client: HttpClient) {

    suspend fun getUser(userId: String): Result<User> = runCatching {
        client.get("/users/$userId").body()
    }

    suspend fun getMe(): Result<User> = runCatching {
        client.get("/users/me").body()
    }

    suspend fun updateProfile(displayName: String? = null, avatarUrl: String? = null): Result<User> = runCatching {
        client.put("/users/me") {
            setBody(UpdateProfileRequest(displayName, avatarUrl))
        }.body()
    }
}
