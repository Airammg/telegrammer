import SwiftUI
import shared

struct ConversationRow: View {
    let conversation: Conversation

    private var name: String {
        let n = conversation.otherUser?.displayName ?? ""
        return n.isEmpty ? (conversation.otherUser?.phoneNumber ?? "Unknown") : n
    }

    private var hasUnread: Bool {
        conversation.unreadCount > 0
    }

    var body: some View {
        HStack(spacing: 12) {
            InitialsAvatar(name: name, size: 48)

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(name)
                        .font(hasUnread ? .body.bold() : .body)
                        .lineLimit(1)
                    Spacer()
                    if let ts = conversation.lastMessageAt?.int64Value {
                        Text(formatTime(ts))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                HStack {
                    Text(conversation.lastMessagePreview ?? "")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                    Spacer()
                    if hasUnread {
                        Text("\(conversation.unreadCount)")
                            .font(.caption2.bold())
                            .foregroundColor(.white)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Theme.primary)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func formatTime(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000.0)
        let calendar = Calendar.current
        let formatter = DateFormatter()
        if calendar.isDateInToday(date) {
            formatter.dateFormat = "HH:mm"
        } else {
            formatter.dateFormat = "MMM d"
        }
        return formatter.string(from: date)
    }
}
