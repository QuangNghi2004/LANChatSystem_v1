package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.server.ClientManager;

/** Xử lý: PIN_MSG, UNPIN_MSG. */
public class PinHandler {
    private final ClientManager clients;
    public PinHandler(ClientManager clients) { this.clients = clients; }

    public void handlePin(String[] p, ClientConnection cc)   { /* TODO */ }
    public void handleUnpin(String[] p, ClientConnection cc) { /* TODO */ }
}
