import SwiftUI
import shared

class ChatViewModel: ObservableObject {
    @Published var messages: [Message] = []
    @Published var isSending = false
    @Published var error: String?

    private var closeable: Closeable?
    private let chatRepo: ChatRepository
    private let chatId: String
    private let recipientId: String

    init(chatRepo: ChatRepository, chatId: String, recipientId: String) {
        self.chatRepo = chatRepo
        self.chatId = chatId
        self.recipientId = recipientId
        observeMessages()
    }

    private func observeMessages() {
        let wrapper = chatRepo.getMessagesWrapped(chatId: chatId)
        closeable = wrapper.watch { [weak self] value in
            guard let list = value as? [Message] else { return }
            DispatchQueue.main.async {
                self?.messages = list
            }
        }
    }

    func sendMessage(text: String) async {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        await MainActor.run { isSending = true; error = nil }
        do {
            let _: Message = try await callSuspend { completion in
                self.chatRepo.sendMessage(chatId: self.chatId, recipientId: self.recipientId, text: text) { result, error in
                    completion(result, error)
                }
            }
            await MainActor.run { isSending = false }
        } catch {
            await MainActor.run { self.isSending = false; self.error = error.localizedDescription }
        }
    }

    func sendTyping() async {
        do {
            try await callSuspendUnit { completion in
                self.chatRepo.sendTyping(chatId: self.chatId) { error in
                    completion(error)
                }
            }
        } catch {}
    }

    func markRead(messageId: String) async {
        do {
            try await callSuspendUnit { completion in
                self.chatRepo.markRead(messageId: messageId, chatId: self.chatId) { error in
                    completion(error)
                }
            }
        } catch {}
    }

    deinit {
        closeable?.close()
    }
}
