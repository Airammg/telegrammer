import shared

func callSuspend<T>(_ block: @escaping (@escaping (T?, Error?) -> Void) -> Void) async throws -> T {
    try await withCheckedThrowingContinuation { continuation in
        block { result, error in
            if let error = error {
                continuation.resume(throwing: error)
            } else if let result = result {
                continuation.resume(returning: result)
            } else {
                continuation.resume(throwing: NSError(domain: "KMP", code: 0, userInfo: [NSLocalizedDescriptionKey: "Null result"]))
            }
        }
    }
}

func callSuspendUnit(_ block: @escaping (@escaping (KotlinUnit?, Error?) -> Void) -> Void) async throws {
    let _: KotlinUnit = try await callSuspend(block)
}
