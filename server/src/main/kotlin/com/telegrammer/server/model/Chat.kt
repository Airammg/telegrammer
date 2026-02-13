package com.telegrammer.server.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Chat(
    @Contextual @SerialName("_id") val id: ObjectId = ObjectId(),
    val participants: List<@Contextual ObjectId>,
    val lastMessageAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ChatResponse(
    val id: String,
    val participants: List<String>,
    val lastMessageAt: Long?,
    val createdAt: Long
) {
    companion object {
        fun from(chat: Chat) = ChatResponse(
            id = chat.id.toHexString(),
            participants = chat.participants.map { it.toHexString() },
            lastMessageAt = chat.lastMessageAt,
            createdAt = chat.createdAt
        )
    }
}

@Serializable
data class CreateChatRequest(
    val participantId: String
)
