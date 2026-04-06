package chat.connection;

import java.io.*;
import java.net.Socket;

/** Base class: đọc/ghi dòng UTF-8 qua socket. */
public abstract class ConnectionHandler {

    protected Socket         socket;
    protected PrintWriter    out;
    protected BufferedReader in;
    protected volatile boolean running = true;

    protected void initStreams() throws IOException {
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
    }

    public void send(String msg) {
        if (out != null) out.println(msg);
    }

    protected void closeSocket() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && running;
    }
}
