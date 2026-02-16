import shared

class FlowObserver<T>: ObservableObject {
    @Published var value: T
    private var closeable: Closeable?

    init(wrapper: FlowWrapper<AnyObject>, initial: T, map: @escaping (Any?) -> T) {
        self.value = initial
        self.closeable = wrapper.watch { [weak self] newValue in
            DispatchQueue.main.async {
                self?.value = map(newValue)
            }
        }
    }

    deinit {
        closeable?.close()
    }
}
