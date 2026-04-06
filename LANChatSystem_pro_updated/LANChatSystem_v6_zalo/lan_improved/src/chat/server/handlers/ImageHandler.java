package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;

/** Xử lý: IMG_SEND, IMG_DATA, IMG_DONE — tương tự FileHandler nhưng cho ảnh inline. */
public class ImageHandler {

    private final ClientManager clients;

    public ImageHandler(ClientManager clients) { this.clients = clients; }

    public void handleSend(String[] p, ClientConnection cc) {
        // IMG_SEND || to || filename || size
        if (cc.getUsername() == null || p.length < 4) return;
        ClientConnection target = clients.getSession(p[1]);
        if (target != null)
            target.send(Protocol.build(Protocol.IMG_SEND, cc.getUsername(), p[2], p[3]));
    }

    public void relay(String[] p, String cmd, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 2) return;
        ClientConnection target = clients.getSession(p[1]);
        if (target == null) return;
        String[] r = new String[p.length + 1];
        r[0] = cmd; r[1] = cc.getUsername();
        System.arraycopy(p, 1, r, 2, p.length - 1);
        target.send(Protocol.build(r));
    }
}
