package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;
import chat.sql.DAO.MessageDAO;
import chat.util.TransferMessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Xử lý: FILE_OFFER, FILE_ACCEPT, FILE_REJECT, FILE_DATA, FILE_DONE. */
public class FileHandler {

    private final ClientManager clients;
    private final MessageDAO    msgDAO;
    private final Map<String, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();

    public FileHandler(ClientManager clients, MessageDAO msgDAO) {
        this.clients = clients;
        this.msgDAO = msgDAO;
    }

    public void handleOffer(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 5) return;
        // FILE_OFFER || to || filename || size || transferId
        ClientConnection target = clients.getSession(p[1]);
        if (target == null) return;

        pendingTransfers.put(key(cc.getUsername(), p[1], p[4]),
            new PendingTransfer(p[2], parseSize(p[3]), p[4]));

        target.send(Protocol.build(Protocol.FILE_OFFER, cc.getUsername(), p[2], p[3], p[4]));

        // Server tự ack để sender gửi ngay, không cần người nhận bấm chấp nhận.
        cc.send(Protocol.build(Protocol.FILE_ACCEPT, p[1], p[1], p[2], p[4]));
    }

    /** Relay lệnh file: chèn sender vào trước các tham số. */
    public void relay(String[] p, String cmd, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 2) return;
        ClientConnection target = clients.getSession(p[1]);
        if (target == null) return;
        String[] relay = new String[p.length + 1];
        relay[0] = cmd; relay[1] = cc.getUsername();
        System.arraycopy(p, 1, relay, 2, p.length - 1);
        target.send(Protocol.build(relay));

        if (Protocol.FILE_DONE.equals(cmd) && p.length >= 4) {
            PendingTransfer transfer = pendingTransfers.remove(key(cc.getUsername(), p[1], p[3]));
            if (transfer != null) {
                msgDAO.savePrivateMessage(cc.getUsername(), p[1],
                    TransferMessageUtil.buildFileMessage(
                        transfer.transferId(), transfer.fileName(), transfer.size()));
            }
        } else if (Protocol.FILE_REJECT.equals(cmd) && p.length >= 4) {
            pendingTransfers.remove(key(cc.getUsername(), p[1], p[3]));
        }
    }

    private String key(String from, String to, String transferId) {
        return from + "|" + to + "|" + transferId;
    }

    private long parseSize(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private record PendingTransfer(String fileName, long size, String transferId) {}
}
