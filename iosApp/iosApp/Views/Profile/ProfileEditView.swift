import SwiftUI
import shared

struct ProfileEditView: View {
    @EnvironmentObject var deps: AppDependencies
    @Environment(\.dismiss) var dismiss
    @State private var displayName = ""
    @State private var phoneNumber = ""
    @State private var isLoading = true
    @State private var isSaving = false
    @State private var error: String?

    var body: some View {
        VStack(spacing: 24) {
            if isLoading {
                Spacer()
                ProgressView()
                Spacer()
            } else {
                Spacer().frame(height: 20)

                InitialsAvatar(name: displayName, size: 80)

                Text(phoneNumber)
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                TextField("Display Name", text: $displayName)
                    .textFieldStyle(.plain)
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
                    Task { await save() }
                } label: {
                    if isSaving {
                        ProgressView()
                            .tint(.white)
                    } else {
                        Text("Save")
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Theme.primary)
                .foregroundColor(.white)
                .cornerRadius(12)
                .padding(.horizontal, 32)
                .disabled(isSaving)

                Spacer()
            }
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadProfile()
        }
    }

    private func loadProfile() async {
        isLoading = true
        do {
            let user: User = try await callSuspend { completion in
                self.deps.userApi.getMe(completionHandler: completion)
            }
            displayName = user.displayName
            phoneNumber = user.phoneNumber
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func save() async {
        isSaving = true
        error = nil
        do {
            let _: User = try await callSuspend { completion in
                self.deps.userApi.updateProfile(displayName: self.displayName, avatarUrl: nil, completionHandler: completion)
            }
            isSaving = false
            dismiss()
        } catch {
            isSaving = false
            self.error = error.localizedDescription
        }
    }
}
