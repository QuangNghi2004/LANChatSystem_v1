package chat.client.listeners;

/** Callback nhận tin nhắn đến từ server — implement ở ClientMain. */
public interface MessageListener {
    void onMessage(String raw);
    void onDisconnected();
}
