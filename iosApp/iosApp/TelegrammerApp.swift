import SwiftUI

@main
struct TelegrammerApp: App {
    @StateObject private var deps = AppDependencies()

    var body: some Scene {
        WindowGroup {
            AppRouter()
                .environmentObject(deps)
                .task {
                    await deps.initializeCrypto()
                }
        }
    }
}
