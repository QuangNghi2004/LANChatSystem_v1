package chat.client;

import chat.client.listeners.MessageListener;
import chat.client.managers.*;
import chat.protocol.Protocol;

import java.util.function.Consumer;

/**
 * Client controller.
 *
 * FIX file transfer index:
 *   FileHandler.relay() format: CMD || sender || originalTo || fname [|| b64]
 *   So for receiver:
 *     FILE_ACCEPT: p[1]=from, p[3]=fname
 *     FILE_REJECT: p[1]=from, p[3]=fname
 *     FILE_DATA:   p[1]=from, p[3]=fname, p[4]=b64
 *     FILE_DONE:   p[1]=from, p[3]=fname
 */
public class ClientMain implements MessageListener {

    public final ConnectionManager conn;
    public final SessionManager    session = SessionManager.getInstance();
    public final MessageManager    msgMgr;
    public final FileManager       fileMgr;
    public final CloudManager      cloudMgr = new CloudManager();

    private Consumer<String[]> onDispatch;
    private Runnable           onReconnect;

    public ClientMain(Consumer<String> unused,
                      Consumer<FileManager.ReceivedFile> onFileReceived,
                      Consumer<FileManager.SentFile> onFileSent,
                      java.util.function.BiConsumer<String,String> onFileProgress) {
        conn    = new ConnectionManager();
        msgMgr  = new MessageManager(conn);
        fileMgr = new FileManager(conn, onFileReceived, onFileSent, onFileProgress);
        conn.setListener(this);
    }

    public void setOnDispatch(Consumer<String[]> cb) { this.onDispatch  = cb; }
    public void setOnReconnect(Runnable cb)          { this.onReconnect = cb; }

    @Override
    public void onMessage(String raw) {
        String[] p = Protocol.parse(raw);
        if (p.length == 0) return;

        switch (p[0]) {
            case Protocol.FILE_ACCEPT -> {
                // server auto-ack: FILE_ACCEPT || receiver || originalTo || fname || transferId
                if (p.length >= 5) fileMgr.onAccepted(p[1], p[3], p[4]);
                return;
            }
            case Protocol.FILE_DATA -> {
                // relay: FILE_DATA || from || to || fname || b64 || transferId
                if (p.length >= 6) fileMgr.onChunk(p[1], p[3], p[4], p[5]);
                return;
            }
            case Protocol.FILE_DONE -> {
                // relay: FILE_DONE || from || to || fname || transferId
                if (p.length >= 5) fileMgr.onDone(p[1], p[3], p[4]);
                return;
            }
        }

        if (onDispatch != null) onDispatch.accept(p);
    }

    @Override
    public void onDisconnected() {
        if (onReconnect != null) onReconnect.run();
    }
}
