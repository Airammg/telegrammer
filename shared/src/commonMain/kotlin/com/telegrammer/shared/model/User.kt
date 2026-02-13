package com.telegrammer.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val phoneNumber: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeenAt: Long = 0
)
