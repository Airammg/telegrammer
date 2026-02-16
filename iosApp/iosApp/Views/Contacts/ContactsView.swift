import SwiftUI
import shared

struct ContactsView: View {
    @EnvironmentObject var deps: AppDependencies
    @Binding var path: NavigationPath
    @State private var searchText = ""
    @State private var contacts: [User] = []
    @State private var isSearching = false
    @State private var closeable: Closeable?

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                TextField("Phone number (e.g. +14155551234)", text: $searchText)
                    .textFieldStyle(.plain)
                    .keyboardType(.phonePad)
                    .padding(10)
                    .background(Theme.surfaceVariant)
                    .cornerRadius(10)

                Button {
                    Task { await search() }
                } label: {
                    if isSearching {
                        ProgressView()
                    } else {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(Theme.primary)
                    }
                }
                .disabled(searchText.isEmpty || isSearching)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            if contacts.isEmpty {
                Spacer()
                Text("Search for contacts by phone number")
                    .foregroundColor(.secondary)
                Spacer()
            } else {
                List(contacts, id: \.id) { user in
                    HStack(spacing: 12) {
                        InitialsAvatar(name: user.displayName, size: 40)
                        VStack(alignment: .leading) {
                            Text(user.displayName.isEmpty ? user.phoneNumber : user.displayName)
                                .font(.body)
                            Text(user.phoneNumber)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        Task { await startChat(with: user) }
                    }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Contacts")
        .task {
            observeContacts()
            await refreshContacts()
        }
        .onDisappear {
            closeable?.close()
        }
    }

    private func observeContacts() {
        let wrapper = deps.contactRepo.contactsWrapped()
        closeable = wrapper.watch { value in
            guard let list = value as? [User] else { return }
            DispatchQueue.main.async {
                self.contacts = list
            }
        }
    }

    private func search() async {
        let phones = searchText.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
        guard !phones.isEmpty else { return }
        isSearching = true
        do {
            let _: NSArray = try await callSuspend { completion in
                self.deps.contactRepo.resolveContacts(phoneNumbers: phones, completionHandler: completion)
            }
        } catch {
            print("Search failed: \(error)")
        }
        isSearching = false
    }

    private func refreshContacts() async {
        do {
            let _: NSArray = try await callSuspend { completion in
                self.deps.contactRepo.refreshContacts(completionHandler: completion)
            }
        } catch {}
    }

    private func startChat(with user: User) async {
        do {
            let response: ChatResponse = try await callSuspend { completion in
                self.deps.chatApi.createChat(participantId: user.id, completionHandler: completion)
            }
            path.append(Route.chat(chatId: response.id, recipientId: user.id, recipientName: user.displayName))
        } catch {
            print("Create chat failed: \(error)")
        }
    }
}
