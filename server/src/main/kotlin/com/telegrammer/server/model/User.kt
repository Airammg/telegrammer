package com.telegrammer.server.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class User(
    @Contextual @SerialName("_id") val id: ObjectId = ObjectId(),
    val phoneNumber: String,
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserResponse(
    val id: String,
    val phoneNumber: String,
    val displayName: String,
    val avatarUrl: String?,
    val isOnline: Boolean,
    val lastSeenAt: Long
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id.toHexString(),
            phoneNumber = user.phoneNumber,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            isOnline = user.isOnline,
            lastSeenAt = user.lastSeenAt
        )
    }
}

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null
)
