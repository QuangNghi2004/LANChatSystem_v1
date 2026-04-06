package chat.app;

import chat.ui.server.ServerDashboard;
import javax.swing.SwingUtilities;

/** 🚀 Entry point — chạy Server Dashboard. */
public class ServerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerDashboard().setVisible(true));
    }
}
