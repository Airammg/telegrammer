package com.telegrammer.shared.model

import kotlinx.serialization.Serializable

enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val isOutgoing: Boolean = false
)
