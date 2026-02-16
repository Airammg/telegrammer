import SwiftUI

struct InitialsAvatar: View {
    let name: String
    var size: CGFloat = 48

    private var initial: String {
        String(name.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased()
    }

    var body: some View {
        ZStack {
            Circle()
                .fill(Theme.primary.opacity(0.2))
                .frame(width: size, height: size)
            Text(initial.isEmpty ? "?" : initial)
                .font(.system(size: size * 0.4, weight: .semibold))
                .foregroundColor(Theme.primary)
        }
    }
}
