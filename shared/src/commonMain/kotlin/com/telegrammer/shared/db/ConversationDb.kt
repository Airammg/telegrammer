package com.telegrammer.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.telegrammer.shared.model.Conversation
import com.telegrammer.shared.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationDb(private val database: TelegrammerDatabase) {

    fun allConversations(): Flow<List<Conversation>> =
        database.conversationQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toConversation() } }

    fun getById(id: String): Conversation? =
        database.conversationQueries.selectById(id)
            .executeAsOneOrNull()
            ?.toConversation()

    fun upsert(conversation: Conversation) {
        database.conversationQueries.upsert(
            id = conversation.id,
            otherUserId = conversation.otherUser?.id ?: "",
            otherUserName = conversation.otherUser?.displayName ?: "",
            otherUserPhone = conversation.otherUser?.phoneNumber ?: "",
            otherUserAvatarUrl = conversation.otherUser?.avatarUrl,
            lastMessageAt = conversation.lastMessageAt,
            lastMessagePreview = conversation.lastMessagePreview,
            unreadCount = conversation.unreadCount.toLong(),
            createdAt = conversation.createdAt
        )
    }

    fun updateLastMessage(chatId: String, timestamp: Long, preview: String) {
        database.conversationQueries.updateLastMessage(
            lastMessageAt = timestamp,
            lastMessagePreview = preview,
            id = chatId
        )
    }

    fun incrementUnread(chatId: String) {
        database.conversationQueries.incrementUnread(chatId)
    }

    fun clearUnread(chatId: String) {
        database.conversationQueries.clearUnread(chatId)
    }

    private fun ConversationEntity.toConversation() = Conversation(
        id = id,
        participantIds = listOf(otherUserId),
        lastMessageAt = lastMessageAt,
        createdAt = createdAt,
        otherUser = User(
            id = otherUserId,
            phoneNumber = otherUserPhone,
            displayName = otherUserName,
            avatarUrl = otherUserAvatarUrl
        ),
        lastMessagePreview = lastMessagePreview,
        unreadCount = unreadCount.toInt()
    )
}
