package com.telegrammer.shared.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateChatRequest(val participantId: String)

@Serializable
data class ChatResponse(
    val id: String,
    val participants: List<String>,
    val lastMessageAt: Long?,
    val createdAt: Long
)

class ChatApi(private val client: HttpClient) {

    suspend fun createChat(participantId: String): Result<ChatResponse> = runCatching {
        client.post("/chats") {
            setBody(CreateChatRequest(participantId))
        }.body()
    }

    suspend fun getChats(): Result<List<ChatResponse>> = runCatching {
        client.get("/chats").body()
    }
}
