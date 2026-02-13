package com.telegrammer.shared.api

import com.telegrammer.shared.platform.SecureStorage
import com.telegrammer.shared.platform.StorageKeys
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(
    private val tokenStore: SecureStorage,
    baseUrl: String = "http://10.0.2.2:8080" // Android emulator -> host
) {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val http = HttpClient {
        install(ContentNegotiation) {
            json(this@ApiClient.json)
        }
        install(WebSockets)

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            tokenStore.getString(StorageKeys.ACCESS_TOKEN)?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }
}
