import SwiftUI
import GameBigBox

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> some UIViewController {
        MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {}
}
