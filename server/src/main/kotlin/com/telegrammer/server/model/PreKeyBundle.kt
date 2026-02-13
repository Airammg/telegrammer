package com.telegrammer.server.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

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
data class PreKeyBundle(
    @Contextual @SerialName("_id") val id: ObjectId = ObjectId(),
    @Contextual val userId: ObjectId,
    val identityKey: String,
    val signedPreKey: SignedPreKeyData,
    val oneTimePreKeys: List<OneTimePreKeyData>
)

@Serializable
data class PreKeyBundleRequest(
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
data class ReplenishPreKeysRequest(
    val oneTimePreKeys: List<OneTimePreKeyData>
)
