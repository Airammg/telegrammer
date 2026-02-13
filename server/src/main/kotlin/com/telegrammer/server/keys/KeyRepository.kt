package com.telegrammer.server.keys

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import com.telegrammer.server.model.OneTimePreKeyData
import com.telegrammer.server.model.PreKeyBundle
import com.telegrammer.server.model.PreKeyBundleResponse
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId

class KeyRepository {
    private val collection = Database.getDatabase().getCollection<PreKeyBundle>("prekey_bundles")

    suspend fun upsertBundle(userId: ObjectId, bundle: PreKeyBundle) {
        val existing = collection.find(Filters.eq("userId", userId)).firstOrNull()
        if (existing != null) {
            collection.replaceOne(Filters.eq("userId", userId), bundle.copy(id = existing.id))
        } else {
            collection.insertOne(bundle)
        }
    }

    suspend fun fetchAndConsumeOneTimeKey(userId: ObjectId): PreKeyBundleResponse? {
        val bundle = collection.find(Filters.eq("userId", userId)).firstOrNull()
            ?: return null

        // Atomically pop one one-time prekey
        var consumedKey: OneTimePreKeyData? = null
        if (bundle.oneTimePreKeys.isNotEmpty()) {
            consumedKey = bundle.oneTimePreKeys.first()
            collection.updateOne(
                Filters.eq("userId", userId),
                Updates.pull("oneTimePreKeys", consumedKey)
            )
        }

        return PreKeyBundleResponse(
            userId = userId.toHexString(),
            identityKey = bundle.identityKey,
            signedPreKey = bundle.signedPreKey,
            oneTimePreKey = consumedKey
        )
    }

    suspend fun addOneTimePreKeys(userId: ObjectId, keys: List<OneTimePreKeyData>) {
        collection.updateOne(
            Filters.eq("userId", userId),
            Updates.pushEach("oneTimePreKeys", keys)
        )
    }

    suspend fun getOneTimeKeyCount(userId: ObjectId): Int {
        val bundle = collection.find(Filters.eq("userId", userId)).firstOrNull()
        return bundle?.oneTimePreKeys?.size ?: 0
    }
}
