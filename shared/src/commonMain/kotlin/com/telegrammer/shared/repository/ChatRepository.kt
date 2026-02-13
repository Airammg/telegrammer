package com.telegrammer.shared.repository

import com.telegrammer.shared.crypto.CryptoSession
import com.telegrammer.shared.db.ConversationDb
import com.telegrammer.shared.db.MessageDb
import com.telegrammer.shared.model.Conversation
import com.telegrammer.shared.model.Message
import com.telegrammer.shared.model.MessageStatus
import com.telegrammer.shared.ws.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatRepository(
    private val chatSocket: ChatSocket,
    private val cryptoSession: CryptoSession,
    private val messageDb: MessageDb,
    private val conversationDb: ConversationDb,
    private val currentUserId: () -> String?,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _typingEvents = MutableSharedFlow<WsTyping>(extraBufferCapacity = 16)
    val typingEvents = _typingEvents.asSharedFlow()

    private val _presenceEvents = MutableSharedFlow<WsPresence>(extraBufferCapacity = 16)
    val presenceEvents = _presenceEvents.asSharedFlow()

    init {
        scope.launch { observeIncoming() }
    }

    private suspend fun observeIncoming() {
        chatSocket.incoming.collect { envelope ->
            when (envelope.type) {
                "message.new" -> handleNewMessage(json.decodeFromJsonElement(envelope.payload))
                "message.ack" -> handleAck(json.decodeFromJsonElement(envelope.payload))
                "receipt.delivered" -> handleDelivered(json.decodeFromJsonElement(envelope.payload))
                "receipt.read" -> handleRead(json.decodeFromJsonElement(envelope.payload))
                "typing" -> _typingEvents.emit(json.decodeFromJsonElement(envelope.payload))
                "presence" -> _presenceEvents.emit(json.decodeFromJsonElement(envelope.payload))
            }
        }
    }

    private suspend fun handleNewMessage(msg: WsNewMessage) {
        val plaintext = try {
            cryptoSession.decrypt(msg.senderId, msg.ciphertext, msg.iv)
        } catch (_: Exception) {
            "[Decryption failed]"
        }

        val message = Message(
            id = msg.messageId,
            chatId = msg.chatId,
            senderId = msg.senderId,
            text = plaintext,
            timestamp = msg.timestamp,
            status = MessageStatus.DELIVERED,
            isOutgoing = false
        )
        messageDb.insertMessage(message)
        conversationDb.updateLastMessage(msg.chatId, msg.timestamp, plaintext.take(100))
        conversationDb.incrementUnread(msg.chatId)

        _incomingMessages.emit(message)

        // Send delivery receipt
        chatSocket.sendDelivered(msg.messageId, msg.chatId)
    }

    private fun handleAck(ack: WsMessageAck) {
        messageDb.updateStatus(ack.messageId, MessageStatus.SENT)
    }

    private fun handleDelivered(receipt: WsDeliveryReceipt) {
        messageDb.updateStatus(receipt.messageId, MessageStatus.DELIVERED)
    }

    private fun handleRead(receipt: WsReadReceipt) {
        messageDb.updateStatus(receipt.messageId, MessageStatus.READ)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun sendMessage(chatId: String, recipientId: String, text: String): Message {
        val localId = Uuid.random().toString()

        // Encrypt
        val (ciphertext, iv) = cryptoSession.encrypt(recipientId, text)

        // Store locally with SENDING status
        val message = Message(
            id = localId,
            chatId = chatId,
            senderId = currentUserId() ?: "",
            text = text,
            timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
            status = MessageStatus.SENDING,
            isOutgoing = true
        )
        messageDb.insertMessage(message)
        conversationDb.updateLastMessage(chatId, message.timestamp, text.take(100))

        // Send via WebSocket
        chatSocket.sendMessage(chatId, recipientId, ciphertext, iv)

        return message
    }

    fun getMessages(chatId: String): Flow<List<Message>> =
        messageDb.messagesForChat(chatId)

    fun getConversations(): Flow<List<Conversation>> =
        conversationDb.allConversations()

    suspend fun sendTyping(chatId: String) {
        val userId = currentUserId() ?: return
        chatSocket.sendTyping(chatId, userId)
    }

    suspend fun markRead(messageId: String, chatId: String) {
        chatSocket.sendRead(messageId, chatId)
        conversationDb.clearUnread(chatId)
    }

    fun connect() = chatSocket.connect()
    fun disconnect() = chatSocket.disconnect()
}
