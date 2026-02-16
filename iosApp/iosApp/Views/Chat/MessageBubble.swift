import SwiftUI
import shared

struct MessageBubble: View {
    let message: Message

    private var isOutgoing: Bool { message.isOutgoing }

    var body: some View {
        HStack {
            if isOutgoing { Spacer(minLength: 60) }

            VStack(alignment: isOutgoing ? .trailing : .leading, spacing: 2) {
                Text(message.text)
                    .font(.body)
                    .foregroundColor(isOutgoing ? .white : .primary)

                HStack(spacing: 4) {
                    Text(formatTime(message.timestamp))
                        .font(.caption2)
                        .foregroundColor(isOutgoing ? .white.opacity(0.7) : .secondary)

                    if isOutgoing {
                        statusIcon
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isOutgoing ? Theme.primary : Theme.surfaceVariant)
            .cornerRadius(16, corners: isOutgoing ? [.topLeading, .topTrailing, .bottomLeading] : [.topLeading, .topTrailing, .bottomTrailing])

            if !isOutgoing { Spacer(minLength: 60) }
        }
    }

    @ViewBuilder
    private var statusIcon: some View {
        let status = message.status
        if status == MessageStatus.sending {
            Image(systemName: "clock")
                .font(.caption2)
                .foregroundColor(.white.opacity(0.7))
        } else if status == MessageStatus.sent {
            Image(systemName: "checkmark")
                .font(.caption2)
                .foregroundColor(.white.opacity(0.7))
        } else if status == MessageStatus.delivered {
            doubleCheck(color: .white.opacity(0.7))
        } else if status == MessageStatus.read {
            doubleCheck(color: Theme.readBlue)
        } else if status == MessageStatus.failed {
            Image(systemName: "exclamationmark.circle")
                .font(.caption2)
                .foregroundColor(.red)
        }
    }

    private func doubleCheck(color: Color) -> some View {
        HStack(spacing: -4) {
            Image(systemName: "checkmark")
                .font(.caption2)
                .foregroundColor(color)
            Image(systemName: "checkmark")
                .font(.caption2)
                .foregroundColor(color)
        }
    }

    private func formatTime(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMs) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
