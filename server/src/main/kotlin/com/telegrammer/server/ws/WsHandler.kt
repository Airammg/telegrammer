package com.telegrammer.server.ws

import com.telegrammer.server.chat.ChatRepository
import com.telegrammer.server.chat.MessageRepository
import com.telegrammer.server.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

class WsHandler(
    private val connectionManager: ConnectionManager,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val json: Json
) {
    private val log = LoggerFactory.getLogger(WsHandler::class.java)

    suspend fun handleMessage(senderId: String, envelope: WsEnvelope) {
        when (envelope.type) {
            "message.send" -> handleSendMessage(senderId, json.decodeFromJsonElement(envelope.payload))
            "message.delivered" -> handleDelivered(senderId, json.decodeFromJsonElement(envelope.payload))
            "receipt.read" -> handleRead(senderId, json.decodeFromJsonElement(envelope.payload))
            "typing" -> handleTyping(senderId, json.decodeFromJsonElement(envelope.payload))
            else -> log.warn("Unknown message type: ${envelope.type}")
        }
    }

    private suspend fun handleSendMessage(senderId: String, msg: WsSendMessage) {
        val message = Message(
            chatId = ObjectId(msg.chatId),
            senderId = ObjectId(senderId),
            ciphertext = msg.ciphertext,
            iv = msg.iv
        )
        val stored = messageRepository.insert(message)
        chatRepository.updateLastMessage(ObjectId(msg.chatId), stored.timestamp)

        // Ack to sender
        val ack = WsEnvelope(
            type = "message.ack",
            payload = json.encodeToJsonElement(
                WsMessageAck.serializer(),
                WsMessageAck(
                    messageId = stored.id.toHexString(),
                    chatId = msg.chatId,
                    timestamp = stored.timestamp,
                    localId = msg.localId
                )
            )
        )
        connectionManager.sendTo(senderId, json.encodeToString(ack))

        // Deliver to recipient
        val newMsg = WsEnvelope(
            type = "message.new",
            payload = json.encodeToJsonElement(
                WsNewMessage.serializer(),
                WsNewMessage(
                    messageId = stored.id.toHexString(),
                    chatId = msg.chatId,
                    senderId = senderId,
                    ciphertext = msg.ciphertext,
                    iv = msg.iv,
                    timestamp = stored.timestamp
                )
            )
        )
        connectionManager.sendTo(msg.recipientId, json.encodeToString(newMsg))
    }

    private suspend fun handleDelivered(senderId: String, receipt: WsDeliveryReceipt) {
        messageRepository.updateStatus(ObjectId(receipt.messageId), "DELIVERED")

        // Find the original sender and notify them
        val message = messageRepository.findById(ObjectId(receipt.messageId)) ?: return
        val envelope = WsEnvelope(
            type = "receipt.delivered",
            payload = json.encodeToJsonElement(
                WsDeliveryReceipt.serializer(),
                receipt
            )
        )
        connectionManager.sendTo(message.senderId.toHexString(), json.encodeToString(envelope))
    }

    private suspend fun handleRead(senderId: String, receipt: WsReadReceipt) {
        messageRepository.updateStatus(ObjectId(receipt.messageId), "READ")

        val message = messageRepository.findById(ObjectId(receipt.messageId)) ?: return
        val envelope = WsEnvelope(
            type = "receipt.read",
            payload = json.encodeToJsonElement(
                WsReadReceipt.serializer(),
                receipt
            )
        )
        connectionManager.sendTo(message.senderId.toHexString(), json.encodeToString(envelope))
    }

    private suspend fun handleTyping(senderId: String, typing: WsTyping) {
        // Find the other participant in the chat and forward
        val chat = chatRepository.findById(ObjectId(typing.chatId)) ?: return
        val recipientId = chat.participants.firstOrNull { it.toHexString() != senderId } ?: return

        val envelope = WsEnvelope(
            type = "typing",
            payload = json.encodeToJsonElement(
                WsTyping.serializer(),
                WsTyping(chatId = typing.chatId, userId = senderId)
            )
        )
        connectionManager.sendTo(recipientId.toHexString(), json.encodeToString(envelope))
    }
}
