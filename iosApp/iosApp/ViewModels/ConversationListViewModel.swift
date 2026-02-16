import SwiftUI
import shared

class ConversationListViewModel: ObservableObject {
    @Published var conversations: [Conversation] = []
    @Published var isLoading = false

    private var closeable: Closeable?
    private let chatRepo: ChatRepository

    init(chatRepo: ChatRepository) {
        self.chatRepo = chatRepo
        observeConversations()
        chatRepo.connect()
        Task { await syncConversations() }
    }

    private func observeConversations() {
        let wrapper = chatRepo.getConversationsWrapped()
        closeable = wrapper.watch { [weak self] value in
            guard let list = value as? [Conversation] else { return }
            DispatchQueue.main.async {
                self?.conversations = list.sorted { ($0.lastMessageAt?.int64Value ?? 0) > ($1.lastMessageAt?.int64Value ?? 0) }
            }
        }
    }

    private func syncConversations() async {
        await MainActor.run { isLoading = true }
        do {
            try await callSuspendUnit { completion in
                self.chatRepo.syncConversations { error in
                    completion(error)
                }
            }
        } catch {
            print("Sync failed: \(error)")
        }
        await MainActor.run { isLoading = false }
    }

    deinit {
        closeable?.close()
        chatRepo.disconnect()
    }
}
