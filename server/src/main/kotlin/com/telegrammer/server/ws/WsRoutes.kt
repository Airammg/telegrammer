package com.telegrammer.server.ws

import com.telegrammer.server.auth.JwtService
import com.telegrammer.server.model.WsEnvelope
import com.telegrammer.server.model.WsPresence
import com.telegrammer.server.user.UserRepository
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("WsRoutes")

fun Route.wsRoutes(
    jwtService: JwtService,
    connectionManager: ConnectionManager,
    wsHandler: WsHandler,
    userRepository: UserRepository,
    json: Json
) {
    webSocket("/ws") {
        // Authenticate via query param
        val token = call.request.queryParameters["token"]
        if (token == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
            return@webSocket
        }

        val decoded = try {
            jwtService.verifier.verify(token)
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        val userId = decoded.subject
        connectionManager.addConnection(userId, this)
        userRepository.setOnline(ObjectId(userId), true)
        log.info("User $userId connected via WebSocket")

        // Broadcast presence
        val presenceOnline = WsEnvelope(
            type = "presence",
            payload = json.encodeToJsonElement(
                WsPresence.serializer(),
                WsPresence(userId = userId, isOnline = true)
            )
        )
        broadcastPresence(connectionManager, userId, json.encodeToString(presenceOnline))

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    try {
                        val envelope = json.decodeFromString<WsEnvelope>(text)
                        wsHandler.handleMessage(userId, envelope)
                    } catch (e: Exception) {
                        log.error("Error handling WS message from $userId: ${e.message}")
                    }
                }
            }
        } finally {
            connectionManager.removeConnection(userId, this)
            userRepository.setOnline(ObjectId(userId), false)
            log.info("User $userId disconnected from WebSocket")

            val presenceOffline = WsEnvelope(
                type = "presence",
                payload = json.encodeToJsonElement(
                    WsPresence.serializer(),
                    WsPresence(userId = userId, isOnline = false)
                )
            )
            broadcastPresence(connectionManager, userId, json.encodeToString(presenceOffline))
        }
    }
}

private suspend fun broadcastPresence(
    connectionManager: ConnectionManager,
    userId: String,
    message: String
) {
    connectionManager.getOnlineUserIds()
        .filter { it != userId }
        .forEach { connectionManager.sendTo(it, message) }
}
