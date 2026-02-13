package com.telegrammer.server

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.telegrammer.server.model.*
import org.bson.Document

object Database {
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase

    fun init(config: MongoConfig) {
        client = MongoClient.create(config.connectionString)
        database = client.getDatabase(config.database)
    }

    fun getDatabase(): MongoDatabase = database

    suspend fun createIndexes() {
        // Users: unique phone number
        database.getCollection<User>("users")
            .createIndex(Document("phoneNumber", 1), com.mongodb.client.model.IndexOptions().unique(true))

        // Messages: by chat + timestamp for pagination
        database.getCollection<Message>("messages")
            .createIndex(Document("chatId", 1).append("timestamp", -1))

        // Chats: by participants for lookup
        database.getCollection<Chat>("chats")
            .createIndex(Document("participants", 1))

        // OTP codes: TTL expiry + phone lookup
        database.getCollection<OtpCode>("otp_codes")
            .createIndex(
                Document("expiresAt", 1),
                com.mongodb.client.model.IndexOptions().expireAfter(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            )
        database.getCollection<OtpCode>("otp_codes")
            .createIndex(Document("phoneNumber", 1))

        // PreKey bundles: one per user
        database.getCollection<PreKeyBundle>("prekey_bundles")
            .createIndex(Document("userId", 1), com.mongodb.client.model.IndexOptions().unique(true))
    }
}
