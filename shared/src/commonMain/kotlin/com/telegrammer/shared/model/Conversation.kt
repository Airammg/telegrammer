package com.telegrammer.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    val participantIds: List<String>,
    val lastMessageAt: Long? = null,
    val createdAt: Long = 0,
    // Populated locally
    val otherUser: User? = null,
    val lastMessagePreview: String? = null,
    val unreadCount: Int = 0
)
