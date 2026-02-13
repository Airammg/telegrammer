package com.telegrammer.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.telegrammer.shared.model.Message
import com.telegrammer.shared.model.MessageStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageDb(private val database: TelegrammerDatabase) {

    fun messagesForChat(chatId: String, limit: Long = 50): Flow<List<Message>> =
        database.messageQueries.selectByChatId(chatId, limit)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toMessage() } }

    fun insertMessage(message: Message) {
        database.messageQueries.insert(
            id = message.id,
            chatId = message.chatId,
            senderId = message.senderId,
            text = message.text,
            timestamp = message.timestamp,
            status = message.status.name,
            isOutgoing = if (message.isOutgoing) 1L else 0L
        )
    }

    fun updateStatus(messageId: String, status: MessageStatus) {
        database.messageQueries.updateStatus(status = status.name, id = messageId)
    }

    fun getLastForChat(chatId: String): Message? =
        database.messageQueries.getLastForChat(chatId)
            .executeAsOneOrNull()
            ?.toMessage()

    private fun MessageEntity.toMessage() = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        text = text,
        timestamp = timestamp,
        status = MessageStatus.valueOf(status),
        isOutgoing = isOutgoing != 0L
    )
}
