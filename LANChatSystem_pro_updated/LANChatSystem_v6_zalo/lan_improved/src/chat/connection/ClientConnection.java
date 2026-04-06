package chat.connection;

import chat.server.ServerMain;
import chat.server.handlers.*;
import chat.util.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * Đại diện cho một client kết nối tới server.
 * Nhận dòng raw → đẩy sang ServerMain.dispatch() để xử lý.
 */
public class ClientConnection extends ConnectionHandler implements Runnable {

    private final ServerMain server;
    private       String     username;
    private       String     role;

    public ClientConnection(Socket socket, ServerMain server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            initStreams();
            String line;
            while (running && (line = in.readLine()) != null) {
                server.dispatch(line.trim(), this);
            }
        } catch (IOException ignored) {
        } finally {
            server.onDisconnect(this);
            closeSocket();
        }
    }

    // ── Getters / Setters ──────────────────────────────────────────────────
    public String  getUsername()          { return username; }
    public void    setUsername(String v)  { username = v; }
    public String  getRole()              { return role; }
    public void    setRole(String v)      { role = v; }
    public boolean isAdmin()              { return "admin".equals(role); }
    public String  getIp()                { return socket.getInetAddress().getHostAddress(); }
}
