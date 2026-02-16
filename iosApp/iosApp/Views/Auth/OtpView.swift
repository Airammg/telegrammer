import SwiftUI
import shared

struct OtpView: View {
    @EnvironmentObject var deps: AppDependencies
    let phone: String
    @Binding var path: NavigationPath
    @State private var code = ""
    @State private var isLoading = false
    @State private var error: String?

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Text("Verification Code")
                .font(.title2.bold())

            Text("Enter the code sent to \(phone)")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            TextField("000000", text: $code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .multilineTextAlignment(.center)
                .font(.title.monospacedDigit())
                .padding()
                .background(Theme.surfaceVariant)
                .cornerRadius(12)
                .padding(.horizontal, 64)

            if let error = error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button {
                Task { await verifyOtp() }
            } label: {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Verify")
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(code.count < 4 ? Color.gray : Theme.primary)
            .foregroundColor(.white)
            .cornerRadius(12)
            .padding(.horizontal, 32)
            .disabled(code.count < 4 || isLoading)

            Spacer()
        }
        .navigationBarBackButtonHidden(false)
        .navigationTitle("")
    }

    private func verifyOtp() async {
        isLoading = true
        error = nil
        do {
            let state: AuthState = try await callSuspend { completion in
                self.deps.authRepo.verifyOtp(phoneNumber: self.phone, code: self.code, completionHandler: completion)
            }
            isLoading = false
            if state is AuthStateAuthenticated {
                deps.isLoggedIn = true
                path = NavigationPath()
            } else if let err = state as? AuthStateError {
                error = err.message
            }
        } catch {
            isLoading = false
            self.error = error.localizedDescription
        }
    }
}
