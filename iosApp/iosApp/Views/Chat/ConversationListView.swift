import SwiftUI
import shared

struct ConversationListView: View {
    @EnvironmentObject var deps: AppDependencies
    @Binding var path: NavigationPath
    @StateObject private var viewModel = ConversationListVM()
    @State private var didInit = false

    var body: some View {
        ZStack {
            if viewModel.conversations.isEmpty && !viewModel.isLoading {
                VStack(spacing: 12) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    Text("No conversations yet")
                        .foregroundColor(.secondary)
                    Text("Tap + to start a new chat")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            } else {
                List(viewModel.conversations, id: \.id) { conversation in
                    ConversationRow(conversation: conversation)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            let recipientName = conversation.otherUser?.displayName ?? ""
                            let recipientId = conversation.otherUser?.id ?? ""
                            path.append(Route.chat(chatId: conversation.id, recipientId: recipientId, recipientName: recipientName))
                        }
                }
                .listStyle(.plain)
            }

            // FAB
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button {
                        path.append(Route.contacts)
                    } label: {
                        Image(systemName: "plus")
                            .font(.title2.bold())
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Theme.primary)
                            .clipShape(Circle())
                            .shadow(radius: 4)
                    }
                    .padding(.trailing, 20)
                    .padding(.bottom, 20)
                }
            }
        }
        .navigationTitle("Telegrammer")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    path.append(Route.profileEdit)
                } label: {
                    Image(systemName: "person.circle")
                        .font(.title3)
                }
            }
        }
        .task {
            guard !didInit else { return }
            didInit = true
            viewModel.setup(chatRepo: deps.chatRepo)
        }
    }
}

private class ConversationListVM: ObservableObject {
    @Published var conversations: [Conversation] = []
    @Published var isLoading = false

    private var closeable: Closeable?
    private var chatRepo: ChatRepository?

    func setup(chatRepo: ChatRepository) {
        guard self.chatRepo == nil else { return }
        self.chatRepo = chatRepo
        chatRepo.connect()
        let wrapper = chatRepo.getConversationsWrapped()
        closeable = wrapper.watch { [weak self] value in
            guard let list = value as? [Conversation] else { return }
            DispatchQueue.main.async {
                self?.conversations = list.sorted { ($0.lastMessageAt?.int64Value ?? 0) > ($1.lastMessageAt?.int64Value ?? 0) }
            }
        }
        Task { @MainActor [weak self] in
            self?.isLoading = true
            do {
                try await callSuspendUnit { completion in
                    chatRepo.syncConversations(completionHandler: completion)
                }
            } catch {}
            self?.isLoading = false
        }
    }

    deinit {
        closeable?.close()
        chatRepo?.disconnect()
    }
}
