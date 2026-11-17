import SwiftUI
import Shared

@main
struct BuildingMyFutureApp: App {
    init() {
        // Antes de pintar nada: datos del bundle, plan activo, tema e idioma.
        MainViewControllerKt.iniciarApp()
    }

    var body: some Scene {
        WindowGroup {
            // Compose pinta hasta los bordes y se aparta solo de la isla, la barra y el teclado.
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
