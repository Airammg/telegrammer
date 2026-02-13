package com.telegrammer.server.contact

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class ContactRepository {
    private val collection = Database.getDatabase().getCollection<Document>("contacts")

    suspend fun addContact(userId: ObjectId, contactUserId: ObjectId) {
        val existing = collection.find(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("contactUserId", contactUserId)
            )
        ).firstOrNull()

        if (existing == null) {
            collection.insertOne(
                Document()
                    .append("userId", userId)
                    .append("contactUserId", contactUserId)
                    .append("createdAt", System.currentTimeMillis())
            )
        }
    }

    suspend fun getContacts(userId: ObjectId): List<ObjectId> =
        collection.find(Filters.eq("userId", userId))
            .toList()
            .mapNotNull { it.getObjectId("contactUserId") }
}
