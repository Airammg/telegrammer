package com.telegrammer.shared.ws

import com.telegrammer.shared.platform.SecureStorage
import com.telegrammer.shared.platform.StorageKeys
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatSocket(
    private val client: HttpClient,
    private val tokenStore: SecureStorage,
    private val json: Json,
    private val wsHost: String = "192.168.1.129",
    private val wsPort: Int = 8080
) {
    private var session: WebSocketSession? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _incoming = MutableSharedFlow<WsEnvelope>(extraBufferCapacity = 64)
    val incoming: SharedFlow<WsEnvelope> = _incoming.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    fun connect() {
        if (connectionJob?.isActive == true) return
        connectionJob = scope.launch {
            while (isActive) {
                try {
                    connectOnce()
                } catch (_: Exception) {
                    _connectionState.value = false
                }
                // Reconnect delay
                delay(3000)
            }
        }
    }

    private suspend fun connectOnce() {
        val token = tokenStore.getString(StorageKeys.ACCESS_TOKEN) ?: return

        client.webSocket(
            host = wsHost,
            port = wsPort,
            path = "/ws?token=$token"
        ) {
            session = this
            _connectionState.value = true

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val envelope = json.decodeFromString<WsEnvelope>(text)
                            _incoming.emit(envelope)
                        } catch (_: Exception) {
                            // Malformed message
                        }
                    }
                }
            } finally {
                session = null
                _connectionState.value = false
            }
        }
    }

    suspend fun send(envelope: WsEnvelope) {
        session?.send(Frame.Text(json.encodeToString(envelope)))
    }

    suspend fun sendMessage(chatId: String, recipientId: String, ciphertext: String, iv: String, localId: String = "") {
        val payload = WsSendMessage(chatId, recipientId, ciphertext, iv, localId)
        val envelope = WsEnvelope(
            type = "message.send",
            payload = json.encodeToJsonElement(WsSendMessage.serializer(), payload)
        )
        send(envelope)
    }

    suspend fun sendDelivered(messageId: String, chatId: String) {
        val payload = WsDeliveryReceipt(messageId, chatId)
        val envelope = WsEnvelope(
            type = "message.delivered",
            payload = json.encodeToJsonElement(WsDeliveryReceipt.serializer(), payload)
        )
        send(envelope)
    }

    suspend fun sendRead(messageId: String, chatId: String) {
        val payload = WsReadReceipt(messageId, chatId)
        val envelope = WsEnvelope(
            type = "receipt.read",
            payload = json.encodeToJsonElement(WsReadReceipt.serializer(), payload)
        )
        send(envelope)
    }

    suspend fun sendTyping(chatId: String, userId: String) {
        val payload = WsTyping(chatId, userId)
        val envelope = WsEnvelope(
            type = "typing",
            payload = json.encodeToJsonElement(WsTyping.serializer(), payload)
        )
        send(envelope)
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        scope.launch {
            session?.close()
            session = null
            _connectionState.value = false
        }
    }
}
