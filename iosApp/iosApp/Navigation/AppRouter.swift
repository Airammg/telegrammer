import SwiftUI

enum Route: Hashable {
    case otp(phone: String)
    case conversations
    case chat(chatId: String, recipientId: String, recipientName: String)
    case contacts
    case profileEdit
}

struct AppRouter: View {
    @EnvironmentObject var deps: AppDependencies
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            if deps.authRepo.isLoggedIn() {
                ConversationListView(path: $path)
                    .navigationDestination(for: Route.self) { route in
                        destination(for: route)
                    }
            } else {
                PhoneInputView(path: $path)
                    .navigationDestination(for: Route.self) { route in
                        destination(for: route)
                    }
            }
        }
    }

    @ViewBuilder
    private func destination(for route: Route) -> some View {
        switch route {
        case .otp(let phone):
            OtpView(phone: phone, path: $path)
        case .conversations:
            ConversationListView(path: $path)
        case .chat(let chatId, let recipientId, let recipientName):
            ChatView(chatId: chatId, recipientId: recipientId, recipientName: recipientName)
        case .contacts:
            ContactsView(path: $path)
        case .profileEdit:
            ProfileEditView()
        }
    }
}
