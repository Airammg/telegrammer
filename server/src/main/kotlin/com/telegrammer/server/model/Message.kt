package com.telegrammer.server.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Message(
    @Contextual @SerialName("_id") val id: ObjectId = ObjectId(),
    @Contextual val chatId: ObjectId,
    @Contextual val senderId: ObjectId,
    val ciphertext: String,
    val iv: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT"
)

@Serializable
data class MessageResponse(
    val id: String,
    val chatId: String,
    val senderId: String,
    val ciphertext: String,
    val iv: String,
    val timestamp: Long,
    val status: String
) {
    companion object {
        fun from(msg: Message) = MessageResponse(
            id = msg.id.toHexString(),
            chatId = msg.chatId.toHexString(),
            senderId = msg.senderId.toHexString(),
            ciphertext = msg.ciphertext,
            iv = msg.iv,
            timestamp = msg.timestamp,
            status = msg.status
        )
    }
}
