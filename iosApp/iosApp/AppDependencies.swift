import shared

class AppDependencies: ObservableObject {
    // Platform
    let secureStorage = SecureStorage()
    let database: TelegrammerDatabase

    // Network
    let apiClient: ApiClient
    let authApi: AuthApi
    let contactApi: ContactApi
    let keyApi: KeyApi
    let chatApi: ChatApi
    let userApi: UserApi

    // Crypto
    let keyManager: KeyManager
    let cryptoSession: CryptoSession

    // WebSocket
    let chatSocket: ChatSocket

    // DB
    let messageDb: MessageDb
    let conversationDb: ConversationDb

    // Repositories
    let authRepo: AuthRepository
    let chatRepo: ChatRepository
    let contactRepo: ContactRepository

    init() {
        let sqlDriver = DriverFactory().create()
        database = TelegrammerDatabase(driver: sqlDriver)

        apiClient = ApiClient(tokenStore: secureStorage, apiHost: "192.168.1.129", apiPort: 8080)
        authApi = AuthApi(client: apiClient.http)
        contactApi = ContactApi(client: apiClient.http)
        keyApi = KeyApi(client: apiClient.http)
        chatApi = ChatApi(client: apiClient.http)
        userApi = UserApi(client: apiClient.http)

        keyManager = KeyManager(storage: secureStorage)
        cryptoSession = CryptoSession(keyManager: keyManager, keyApi: keyApi, storage: secureStorage)

        chatSocket = ChatSocket(client: apiClient.http, tokenStore: secureStorage, json: apiClient.json, wsHost: "192.168.1.129", wsPort: 8080)

        messageDb = MessageDb(database: database)
        conversationDb = ConversationDb(database: database)

        authRepo = AuthRepository(authApi: authApi, storage: secureStorage, keyManager: keyManager, keyApi: keyApi)
        chatRepo = ChatRepository(
            chatSocket: chatSocket,
            cryptoSession: cryptoSession,
            messageDb: messageDb,
            conversationDb: conversationDb,
            currentUserId: { [weak self] in self?.authRepo.getUserId() },
            json: apiClient.json,
            chatApi: chatApi,
            userApi: userApi
        )
        contactRepo = ContactRepository(contactApi: contactApi)
    }

    func initializeCrypto() async {
        do {
            try await callSuspendUnit { completion in
                self.keyManager.initialize(completionHandler: completion)
            }
            if !self.keyManager.hasIdentityKey() {
                let bundle = self.keyManager.generateUploadBundle(oneTimeKeyCount: 100)
                try await callSuspendUnit { completion in
                    self.keyApi.uploadBundle(request: bundle, completionHandler: completion)
                }
            }
        } catch {
            print("Crypto init failed: \(error)")
        }
    }
}
