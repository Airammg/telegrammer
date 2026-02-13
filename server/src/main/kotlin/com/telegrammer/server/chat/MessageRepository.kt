package com.telegrammer.server.chat

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import com.telegrammer.server.model.Message
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class MessageRepository {
    private val collection = Database.getDatabase().getCollection<Message>("messages")

    suspend fun insert(message: Message): Message {
        collection.insertOne(message)
        return message
    }

    suspend fun findByChatId(
        chatId: ObjectId,
        before: Long? = null,
        limit: Int = 50
    ): List<Message> {
        val filter = if (before != null) {
            Filters.and(
                Filters.eq("chatId", chatId),
                Filters.lt("timestamp", before)
            )
        } else {
            Filters.eq("chatId", chatId)
        }
        return collection.find(filter)
            .sort(Sorts.descending("timestamp"))
            .limit(limit)
            .toList()
    }

    suspend fun updateStatus(messageId: ObjectId, status: String) {
        collection.updateOne(
            Filters.eq("_id", messageId),
            Updates.set("status", status)
        )
    }

    suspend fun findById(id: ObjectId): Message? =
        collection.find(Filters.eq("_id", id)).firstOrNull()
}
