import SwiftUI
import shared

class AuthViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var error: String?

    private let authRepo: AuthRepository

    init(authRepo: AuthRepository) {
        self.authRepo = authRepo
    }

    func requestOtp(phone: String) async -> Bool {
        await MainActor.run { isLoading = true; error = nil }
        do {
            let state: AuthState = try await callSuspend { completion in
                self.authRepo.requestOtp(phoneNumber: phone) { result, error in
                    completion(result, error)
                }
            }
            await MainActor.run { isLoading = false }
            if state is AuthStateError {
                let err = state as! AuthStateError
                await MainActor.run { error = err.message }
                return false
            }
            return true
        } catch {
            await MainActor.run { self.isLoading = false; self.error = error.localizedDescription }
            return false
        }
    }

    func verifyOtp(phone: String, code: String) async -> Bool {
        await MainActor.run { isLoading = true; error = nil }
        do {
            let state: AuthState = try await callSuspend { completion in
                self.authRepo.verifyOtp(phoneNumber: phone, code: code) { result, error in
                    completion(result, error)
                }
            }
            await MainActor.run { isLoading = false }
            if state is AuthStateAuthenticated {
                return true
            }
            if state is AuthStateError {
                let err = state as! AuthStateError
                await MainActor.run { error = err.message }
            }
            return false
        } catch {
            await MainActor.run { self.isLoading = false; self.error = error.localizedDescription }
            return false
        }
    }
}
