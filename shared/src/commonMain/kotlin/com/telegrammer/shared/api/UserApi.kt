package com.telegrammer.shared.api

import com.telegrammer.shared.model.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class UserApi(private val client: HttpClient) {

    suspend fun getUser(userId: String): Result<User> = runCatching {
        client.get("/users/$userId").body()
    }

    suspend fun getMe(): Result<User> = runCatching {
        client.get("/users/me").body()
    }
}
