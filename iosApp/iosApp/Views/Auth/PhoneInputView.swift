import SwiftUI
import shared

struct PhoneInputView: View {
    @EnvironmentObject var deps: AppDependencies
    @Binding var path: NavigationPath
    @State private var phone = ""
    @State private var isLoading = false
    @State private var error: String?

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Text("Telegrammer")
                .font(.largeTitle.bold())
                .foregroundColor(Theme.primary)

            Text("Enter your phone number")
                .font(.subheadline)
                .foregroundColor(.secondary)

            TextField("+1 234 567 8900", text: $phone)
                .keyboardType(.phonePad)
                .textContentType(.telephoneNumber)
                .padding()
                .background(Theme.surfaceVariant)
                .cornerRadius(12)
                .padding(.horizontal, 32)

            if let error = error {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button {
                Task { await requestOtp() }
            } label: {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Continue")
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(phone.isEmpty ? Color.gray : Theme.primary)
            .foregroundColor(.white)
            .cornerRadius(12)
            .padding(.horizontal, 32)
            .disabled(phone.isEmpty || isLoading)

            Spacer()
        }
        .navigationBarHidden(true)
    }

    private func requestOtp() async {
        isLoading = true
        error = nil
        do {
            let state: AuthState = try await callSuspend { completion in
                self.deps.authRepo.requestOtp(phoneNumber: self.phone, completionHandler: completion)
            }
            isLoading = false
            if let err = state as? AuthStateError {
                error = err.message
            } else {
                path.append(Route.otp(phone: phone))
            }
        } catch {
            isLoading = false
            self.error = error.localizedDescription
        }
    }
}
