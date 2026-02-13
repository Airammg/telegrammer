package com.telegrammer.server.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class OtpCode(
    @Contextual @SerialName("_id") val id: ObjectId = ObjectId(),
    val phoneNumber: String,
    val codeHash: String,
    val attempts: Int = 0,
    val expiresAt: Long = System.currentTimeMillis() + 300_000
)
