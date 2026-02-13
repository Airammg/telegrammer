package com.telegrammer.server.chat

import com.telegrammer.server.model.ChatResponse
import com.telegrammer.server.model.CreateChatRequest
import com.telegrammer.server.model.MessageResponse
import com.telegrammer.server.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

fun Route.chatRoutes(
    chatRepository: ChatRepository,
    messageRepository: MessageRepository
) {
    authenticate {
        route("/chats") {
            get {
                val userId = ObjectId(call.userId())
                val chats = chatRepository.findByUser(userId)
                call.respond(chats.map { ChatResponse.from(it) })
            }

            post {
                val body = call.receive<CreateChatRequest>()
                val currentUserId = ObjectId(call.userId())
                val participantId = ObjectId(body.participantId)

                if (currentUserId == participantId) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot chat with yourself"))
                    return@post
                }

                val chat = chatRepository.findOrCreate(currentUserId, participantId)
                call.respond(HttpStatusCode.Created, ChatResponse.from(chat))
            }

            get("/{id}/messages") {
                val chatId = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing chat id"))
                    return@get
                }
                val before = call.request.queryParameters["before"]?.toLongOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

                val chat = chatRepository.findById(ObjectId(chatId))
                if (chat == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Chat not found"))
                    return@get
                }

                // Verify user is participant
                val userId = ObjectId(call.userId())
                if (userId !in chat.participants) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not a participant"))
                    return@get
                }

                val messages = messageRepository.findByChatId(
                    chatId = ObjectId(chatId),
                    before = before,
                    limit = limit.coerceIn(1, 100)
                )
                call.respond(messages.map { MessageResponse.from(it) })
            }
        }
    }
}
