import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255.0
        let g = Double((int >> 8) & 0xFF) / 255.0
        let b = Double(int & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

enum Theme {
    static let primary = Color(hex: "0088CC")
    static let primaryContainer = Color(hex: "D4E9F7")
    static let surfaceVariant = Color(hex: "F0F0F0")
    static let readBlue = Color(hex: "34B7F1")

    enum Dark {
        static let primary = Color(hex: "64B5F6")
        static let primaryContainer = Color(hex: "1A3A5C")
        static let background = Color(hex: "121212")
    }
}
