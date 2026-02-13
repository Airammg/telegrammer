package com.telegrammer.server.ws

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager {
    private val connections = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun addConnection(userId: String, session: WebSocketSession) {
        connections.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun removeConnection(userId: String, session: WebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
    }

    fun isOnline(userId: String): Boolean = connections.containsKey(userId)

    suspend fun sendTo(userId: String, message: String) {
        connections[userId]?.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (_: Exception) {
                // Session might be closing
            }
        }
    }

    fun getOnlineUserIds(): Set<String> = connections.keys.toSet()
}
