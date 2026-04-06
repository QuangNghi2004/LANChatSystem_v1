package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.server.ClientManager;

/** Xử lý: REACT, UNREACT — broadcast REACT_UPDATE tới room. */
public class ReactionHandler {
    private final ClientManager clients;
    public ReactionHandler(ClientManager clients) { this.clients = clients; }

    public void handleReact(String[] p, ClientConnection cc)   { /* TODO */ }
    public void handleUnreact(String[] p, ClientConnection cc) { /* TODO */ }
}
