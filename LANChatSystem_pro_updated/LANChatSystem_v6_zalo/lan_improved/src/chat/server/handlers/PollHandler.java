package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.server.ClientManager;

/** Xử lý: POLL_CREATE, POLL_VOTE, POLL_CLOSE — placeholder mở rộng sau. */
public class PollHandler {
    private final ClientManager clients;
    public PollHandler(ClientManager clients) { this.clients = clients; }

    public void handleCreate(String[] p, ClientConnection cc) { /* TODO */ }
    public void handleVote(String[] p, ClientConnection cc)   { /* TODO */ }
    public void handleClose(String[] p, ClientConnection cc)  { /* TODO */ }
}
