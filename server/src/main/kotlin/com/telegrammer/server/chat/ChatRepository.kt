package com.telegrammer.server.chat

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import com.telegrammer.server.model.Chat
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class ChatRepository {
    private val collection = Database.getDatabase().getCollection<Chat>("chats")

    suspend fun findOrCreate(participantA: ObjectId, participantB: ObjectId): Chat {
        // Check if chat already exists between these two users
        val existing = collection.find(
            Filters.all("participants", participantA, participantB)
        ).firstOrNull()

        if (existing != null) return existing

        val chat = Chat(participants = listOf(participantA, participantB))
        collection.insertOne(chat)
        return chat
    }

    suspend fun findById(id: ObjectId): Chat? =
        collection.find(Filters.eq("_id", id)).firstOrNull()

    suspend fun findByUser(userId: ObjectId): List<Chat> =
        collection.find(Filters.eq("participants", userId))
            .sort(Sorts.descending("lastMessageAt"))
            .toList()

    suspend fun updateLastMessage(chatId: ObjectId, timestamp: Long) {
        collection.updateOne(
            Filters.eq("_id", chatId),
            Updates.set("lastMessageAt", timestamp)
        )
    }
}
