import SwiftUI
import shared

struct ProfileState {
    var displayName: String = ""
    var phoneNumber: String = ""
    var isLoading = false
    var isSaving = false
    var error: String?
}

class ProfileEditViewModel: ObservableObject {
    @Published var state = ProfileState()

    private let userApi: UserApi

    init(userApi: UserApi) {
        self.userApi = userApi
        Task { await loadProfile() }
    }

    func loadProfile() async {
        await MainActor.run { state.isLoading = true }
        do {
            let user: User = try await callSuspend { completion in
                self.userApi.getMe { result, error in
                    completion(result, error)
                }
            }
            await MainActor.run {
                state.displayName = user.displayName
                state.phoneNumber = user.phoneNumber
                state.isLoading = false
            }
        } catch {
            await MainActor.run { state.isLoading = false; state.error = error.localizedDescription }
        }
    }

    func save() async -> Bool {
        await MainActor.run { state.isSaving = true; state.error = nil }
        do {
            let _: User = try await callSuspend { completion in
                self.userApi.updateProfile(displayName: self.state.displayName, avatarUrl: nil) { result, error in
                    completion(result, error)
                }
            }
            await MainActor.run { state.isSaving = false }
            return true
        } catch {
            await MainActor.run { state.isSaving = false; state.error = error.localizedDescription }
            return false
        }
    }
}
