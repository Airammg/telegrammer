package com.telegrammer.android

import android.content.Context
import com.telegrammer.shared.api.ApiClient
import com.telegrammer.shared.api.AuthApi
import com.telegrammer.shared.api.ChatApi
import com.telegrammer.shared.api.ContactApi
import com.telegrammer.shared.api.KeyApi
import com.telegrammer.shared.api.UserApi
import com.telegrammer.shared.crypto.CryptoSession
import com.telegrammer.shared.crypto.KeyManager
import com.telegrammer.shared.db.ConversationDb
import com.telegrammer.shared.db.MessageDb
import com.telegrammer.shared.db.TelegrammerDatabase
import com.telegrammer.shared.platform.DriverFactory
import com.telegrammer.shared.platform.SecureStorage
import com.telegrammer.shared.repository.AuthRepository
import com.telegrammer.shared.repository.ChatRepository
import com.telegrammer.shared.repository.ContactRepository
import com.telegrammer.shared.ws.ChatSocket

class AppDependencies(context: Context) {
    // Platform
    val secureStorage = SecureStorage(context)
    private val sqlDriver = DriverFactory(context).create()
    val database = TelegrammerDatabase(sqlDriver)

    // Network
    val apiClient = ApiClient(tokenStore = secureStorage)
    val authApi = AuthApi(apiClient.http)
    val contactApi = ContactApi(apiClient.http)
    val keyApi = KeyApi(apiClient.http)
    val chatApi = ChatApi(apiClient.http)
    val userApi = UserApi(apiClient.http)

    // Crypto
    val keyManager = KeyManager(secureStorage)
    val cryptoSession = CryptoSession(keyManager, keyApi, secureStorage)

    // WebSocket
    val chatSocket = ChatSocket(apiClient.http, secureStorage, apiClient.json)

    // DB
    val messageDb = MessageDb(database)
    val conversationDb = ConversationDb(database)

    // Repositories
    val authRepo = AuthRepository(authApi, secureStorage, keyManager, keyApi)
    val chatRepo = ChatRepository(
        chatSocket = chatSocket,
        cryptoSession = cryptoSession,
        messageDb = messageDb,
        conversationDb = conversationDb,
        currentUserId = { authRepo.getUserId() },
        json = apiClient.json,
        chatApi = chatApi,
        userApi = userApi
    )
    val contactRepo = ContactRepository(contactApi)
}
