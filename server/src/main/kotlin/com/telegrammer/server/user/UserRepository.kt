package com.telegrammer.server.user

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import com.telegrammer.server.model.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class UserRepository {
    private val collection = Database.getDatabase().getCollection<User>("users")

    suspend fun findOrCreateByPhone(phoneNumber: String): User {
        val existing = collection.find(Filters.eq("phoneNumber", phoneNumber)).firstOrNull()
        if (existing != null) return existing

        val user = User(phoneNumber = phoneNumber)
        collection.insertOne(user)
        return user
    }

    suspend fun findById(id: ObjectId): User? =
        collection.find(Filters.eq("_id", id)).firstOrNull()

    suspend fun findByPhoneNumbers(phoneNumbers: List<String>): List<User> =
        collection.find(Filters.`in`("phoneNumber", phoneNumbers)).toList()

    suspend fun updateProfile(id: ObjectId, displayName: String?, avatarUrl: String?): User? {
        val updates = mutableListOf<org.bson.conversions.Bson>()
        displayName?.let { updates.add(Updates.set("displayName", it)) }
        avatarUrl?.let { updates.add(Updates.set("avatarUrl", it)) }
        if (updates.isEmpty()) return findById(id)

        collection.updateOne(Filters.eq("_id", id), Updates.combine(updates))
        return findById(id)
    }

    suspend fun setOnline(id: ObjectId, online: Boolean) {
        val updates = Updates.combine(
            Updates.set("isOnline", online),
            Updates.set("lastSeenAt", System.currentTimeMillis())
        )
        collection.updateOne(Filters.eq("_id", id), updates)
    }
}
