package com.telegrammer.shared.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class SignedPreKeyData(
    val keyId: Int,
    val publicKey: String,
    val signature: String
)

@Serializable
data class OneTimePreKeyData(
    val keyId: Int,
    val publicKey: String
)

@Serializable
data class UploadBundleRequest(
    val identityKey: String,
    val signedPreKey: SignedPreKeyData,
    val oneTimePreKeys: List<OneTimePreKeyData>
)

@Serializable
data class PreKeyBundleResponse(
    val userId: String,
    val identityKey: String,
    val signedPreKey: SignedPreKeyData,
    val oneTimePreKey: OneTimePreKeyData?
)

@Serializable
data class ReplenishRequest(
    val oneTimePreKeys: List<OneTimePreKeyData>
)

class KeyApi(private val client: HttpClient) {

    suspend fun uploadBundle(request: UploadBundleRequest): Result<Unit> = runCatching {
        client.post("/keys/bundle") {
            setBody(request)
        }
        Unit
    }

    suspend fun fetchBundle(userId: String): Result<PreKeyBundleResponse> = runCatching {
        client.get("/keys/bundle/$userId").body()
    }

    suspend fun replenishPreKeys(keys: List<OneTimePreKeyData>): Result<Unit> = runCatching {
        client.post("/keys/replenish") {
            setBody(ReplenishRequest(keys))
        }
        Unit
    }
}
