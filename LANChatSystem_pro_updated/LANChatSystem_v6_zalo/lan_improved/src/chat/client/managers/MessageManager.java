package chat.client.managers;

import chat.client.ConnectionManager;
import chat.protocol.Protocol;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Gửi các loại tin nhắn tới server. */
public class MessageManager {

    private final ConnectionManager conn;

    public MessageManager(ConnectionManager conn) { this.conn = conn; }

    public void sendBroadcast(String text) {
        conn.send(Protocol.build(Protocol.MSG, text));
    }

    public void sendPM(String to, String text) {
        conn.send(Protocol.build(Protocol.PM, to, text));
    }

    public void sendGroupMsg(String group, String text) {
        conn.send(Protocol.build(Protocol.GROUP_MSG, group, text));
    }

    public void markRead(String from) {
        conn.send(Protocol.build(Protocol.READ, from));
    }

    public void requestHistory(String with) {
        conn.send(Protocol.build(Protocol.HISTORY_REQ, with));
    }

    public String nowTs() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}
