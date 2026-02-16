import SwiftUI
import shared

class ContactsViewModel: ObservableObject {
    @Published var contacts: [User] = []
    @Published var isSearching = false

    private var closeable: Closeable?
    private let contactRepo: ContactRepository

    init(contactRepo: ContactRepository) {
        self.contactRepo = contactRepo
        observeContacts()
        Task { await refreshContacts() }
    }

    private func observeContacts() {
        let wrapper = contactRepo.contactsWrapped()
        closeable = wrapper.watch { [weak self] value in
            guard let list = value as? [User] else { return }
            DispatchQueue.main.async {
                self?.contacts = list
            }
        }
    }

    func searchByPhone(text: String) async {
        let phones = text.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
        guard !phones.isEmpty else { return }
        await MainActor.run { isSearching = true }
        do {
            let _: NSArray = try await callSuspend { completion in
                self.contactRepo.resolveContacts(phoneNumbers: phones) { result, error in
                    completion(result as? NSArray, error)
                }
            }
        } catch {
            print("Search failed: \(error)")
        }
        await MainActor.run { isSearching = false }
    }

    private func refreshContacts() async {
        do {
            let _: NSArray = try await callSuspend { completion in
                self.contactRepo.refreshContacts { result, error in
                    completion(result as? NSArray, error)
                }
            }
        } catch {}
    }

    deinit {
        closeable?.close()
    }
}
