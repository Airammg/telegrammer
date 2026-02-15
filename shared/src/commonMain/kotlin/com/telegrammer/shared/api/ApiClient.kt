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
    private val apiHost: String = "192.168.1.144",
    private val apiPort: Int = 8080
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
            url {
                protocol = URLProtocol.HTTP
                host = apiHost
                port = apiPort
            }
            contentType(ContentType.Application.Json)
            tokenStore.getString(StorageKeys.ACCESS_TOKEN)?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }
}
