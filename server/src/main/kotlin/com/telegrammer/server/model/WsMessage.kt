package com.telegrammer.server.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WsEnvelope(
    val type: String,
    val payload: JsonElement
)

@Serializable
data class WsSendMessage(
    val chatId: String,
    val recipientId: String,
    val ciphertext: String,
    val iv: String
)

@Serializable
data class WsMessageAck(
    val messageId: String,
    val chatId: String,
    val timestamp: Long
)

@Serializable
data class WsNewMessage(
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val ciphertext: String,
    val iv: String,
    val timestamp: Long
)

@Serializable
data class WsDeliveryReceipt(
    val messageId: String,
    val chatId: String
)

@Serializable
data class WsReadReceipt(
    val messageId: String,
    val chatId: String
)

@Serializable
data class WsTyping(
    val chatId: String,
    val userId: String
)

@Serializable
data class WsPresence(
    val userId: String,
    val isOnline: Boolean
)
