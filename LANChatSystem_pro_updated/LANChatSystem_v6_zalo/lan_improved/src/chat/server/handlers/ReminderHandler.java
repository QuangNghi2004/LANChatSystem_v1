package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.server.ClientManager;

/** Xử lý: REMINDER_SET, fire reminder — placeholder. */
public class ReminderHandler {
    private final ClientManager clients;
    public ReminderHandler(ClientManager clients) { this.clients = clients; }

    public void handleSet(String[] p, ClientConnection cc)  { /* TODO */ }
}
