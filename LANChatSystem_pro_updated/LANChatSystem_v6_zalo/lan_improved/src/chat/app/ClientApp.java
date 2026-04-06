package chat.app;

import chat.ui.client.ClientWindow;
import javax.swing.SwingUtilities;

/** 🚀 Entry point — chạy Client. */
public class ClientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientWindow().showLogin());
    }
}
