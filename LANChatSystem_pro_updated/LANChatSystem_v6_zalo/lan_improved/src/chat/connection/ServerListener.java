package chat.connection;

import chat.server.ServerMain;
import chat.util.Config;
import chat.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Lắng nghe kết nối TCP đến — tạo ClientConnection cho mỗi client.
 * Hoàn toàn độc lập với UI.
 */
public class ServerListener {

    private ServerSocket       serverSocket;
    private boolean            running = false;
    private final ExecutorService pool  = Executors.newCachedThreadPool();
    private final ServerMain   server;

    public ServerListener(ServerMain server) { this.server = server; }

    public void start() throws IOException {
        serverSocket = new ServerSocket(Config.SERVER_PORT);
        running = true;
        pool.execute(this::acceptLoop);
        Logger.info("✅ Lắng nghe trên port " + Config.SERVER_PORT);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        Logger.info("⏹ ServerListener dừng.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket sock = serverSocket.accept();
                ClientConnection cc = new ClientConnection(sock, server);
                pool.execute(cc);
                Logger.info("🔌 Kết nối mới: " + sock.getInetAddress().getHostAddress());
            } catch (IOException e) {
                if (running) Logger.warn("Lỗi accept: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() { return running; }
}
