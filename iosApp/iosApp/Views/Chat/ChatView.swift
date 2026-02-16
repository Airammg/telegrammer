import SwiftUI
import shared

struct ChatView: View {
    @EnvironmentObject var deps: AppDependencies
    let chatId: String
    let recipientId: String
    let recipientName: String

    @State private var messageText = ""
    @State private var messages: [Message] = []
    @State private var isSending = false
    @State private var closeable: Closeable?

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 4) {
                        ForEach(messages, id: \.id) { message in
                            MessageBubble(message: message)
                                .id(message.id)
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 8)
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last {
                        withAnimation {
                            proxy.scrollTo(last.id, anchor: .bottom)
                        }
                    }
                }
                .onAppear {
                    if let last = messages.last {
                        proxy.scrollTo(last.id, anchor: .bottom)
                    }
                }
            }

            Divider()

            HStack(spacing: 8) {
                TextField("Message", text: $messageText)
                    .textFieldStyle(.plain)
                    .padding(10)
                    .background(Theme.surfaceVariant)
                    .cornerRadius(20)

                Button {
                    let text = messageText
                    messageText = ""
                    Task { await sendMessage(text: text) }
                } label: {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(messageText.isEmpty ? .gray : Theme.primary)
                }
                .disabled(messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isSending)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .navigationTitle(recipientName.isEmpty ? "Chat" : recipientName)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            observeMessages()
            markIncomingAsRead()
        }
        .onChange(of: messages) { _ in
            markIncomingAsRead()
        }
        .onDisappear {
            closeable?.close()
        }
    }

    private func observeMessages() {
        let wrapper = deps.chatRepo.getMessagesWrapped(chatId: chatId)
        closeable = wrapper.watch { value in
            guard let list = value as? [Message] else { return }
            DispatchQueue.main.async {
                self.messages = list
            }
        }
    }

    private func sendMessage(text: String) async {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        isSending = true
        do {
            let _: Message = try await callSuspend { completion in
                self.deps.chatRepo.sendMessage(chatId: self.chatId, recipientId: self.recipientId, text: text, completionHandler: completion)
            }
        } catch {
            print("Send failed: \(error)")
        }
        isSending = false
    }

    private func markIncomingAsRead() {
        for msg in messages where !msg.isOutgoing && msg.status != MessageStatus.read {
            Task {
                do {
                    try await callSuspendUnit { completion in
                        self.deps.chatRepo.markRead(messageId: msg.id, chatId: self.chatId, completionHandler: completion)
                    }
                } catch {}
            }
        }
    }
}
