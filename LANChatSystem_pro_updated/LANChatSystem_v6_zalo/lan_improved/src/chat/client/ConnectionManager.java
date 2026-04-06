package chat.client;

import chat.client.listeners.MessageListener;
import chat.util.Config;
import chat.util.Logger;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Quản lý kết nối TCP từ client tới server.
 * Gọi MessageListener khi nhận dữ liệu hoặc mất kết nối.
 */
public class ConnectionManager {

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private volatile boolean connected  = false;
    private volatile boolean autoRecon  = true;
    private String         host;

    private MessageListener listener;

    public void setListener(MessageListener l) { this.listener = l; }

    public void connect(String host, String firstMsg, Consumer<String> onError) {
        this.host    = host;
        this.autoRecon = true;
        new Thread(() -> {
            try {
                socket = new Socket(host, Config.SERVER_PORT);
                out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
                in     = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
                connected = true;
                if (firstMsg != null) send(firstMsg);
                new Thread(this::readLoop, "reader").start();
            } catch (IOException e) {
                if (onError != null) onError.accept(e.getMessage());
            }
        }, "connector").start();
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                final String raw = line;
                if (listener != null) listener.onMessage(raw);
            }
        } catch (IOException e) {
            if (connected && autoRecon && listener != null) {
                connected = false;
                listener.onDisconnected();
                scheduleReconnect();
            }
        }
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try { Thread.sleep(Config.RECONNECT_DELAY); } catch (InterruptedException ignored) {}
            if (autoRecon && listener != null) listener.onDisconnected();
        }).start();
    }

    public void send(String msg) { if (out != null) out.println(msg); }

    public void disconnect() {
        autoRecon = false; connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    /** Đóng socket không gửi LOGOUT (sau đăng ký). */
    public void closeQuiet() { autoRecon = false; connected = false; disconnect(); }

    public boolean isConnected() { return connected; }
    public String  getHost()     { return host; }
}
