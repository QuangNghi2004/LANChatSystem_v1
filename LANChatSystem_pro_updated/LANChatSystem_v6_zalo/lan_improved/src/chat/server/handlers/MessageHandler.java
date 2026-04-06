package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;
import chat.sql.DAO.MessageDAO;
import chat.sql.DAO.UserDAO;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/** Xử lý: MSG, PM, GROUP_MSG, DELETE_MSG, EDIT_MSG. */
public class MessageHandler {

    private final ClientManager clients;
    private final MessageDAO    msgDAO;
    private final UserDAO       userDAO;

    public MessageHandler(ClientManager clients, MessageDAO msgDAO, UserDAO userDAO) {
        this.clients = clients; this.msgDAO = msgDAO; this.userDAO = userDAO;
    }

    public void handleBroadcast(String[] p, ClientConnection cc) {
        if (!auth(cc) || p.length < 2) return;
        String msg = p[1], ts = ts();
        clients.broadcast(Protocol.build(Protocol.MSG, cc.getUsername(), msg, ts), null);
        msgDAO.saveBroadcast(cc.getUsername(), msg);
    }

    public void handlePM(String[] p, ClientConnection cc) {
        if (!auth(cc) || p.length < 3) return;
        String to = p[1], msg = p[2], ts = ts();
        ClientConnection target = clients.getSession(to);
        if (target != null) {
            target.send(Protocol.build(Protocol.PM, cc.getUsername(), msg, ts));
            cc.send(Protocol.build(Protocol.DELIVERED, to, msg, ts));
        } else {
            // Người nhận offline — lưu thông báo
        }
        msgDAO.savePrivateMessage(cc.getUsername(), to, msg);
    }

    public void handleGroupMsg(String[] p, ClientConnection cc) {
        if (!auth(cc) || p.length < 3) return;
        String group = p[1], msg = p[2], ts = ts();
        clients.broadcastGroup(group,
            Protocol.build(Protocol.GROUP_MSG, group, cc.getUsername(), msg, ts),
            cc.getUsername());
        msgDAO.saveGroupMessage(cc.getUsername(), group, msg);
    }

    public void handleRead(String[] p, ClientConnection cc) {
        if (!auth(cc) || p.length < 2) return;
        msgDAO.markRead(p[1], cc.getUsername());
        ClientConnection s = clients.getSession(p[1]);
        if (s != null) s.send(Protocol.build(Protocol.READ, cc.getUsername()));
    }

    public void handleHistory(String[] p, ClientConnection cc) {
        if (!auth(cc) || p.length < 2) return;
        String with = p[1];
        if (!with.equals("ALL") && !with.startsWith("#"))
            msgDAO.markRead(with, cc.getUsername());
        var hist = msgDAO.getHistory(cc.getUsername(), with);
        StringBuilder sb = new StringBuilder();
        for (String[] m : hist) {
            String encodedContent = Base64.getEncoder()
                .encodeToString(m[2].getBytes(StandardCharsets.UTF_8));
            sb.append(m[0]).append(":").append(encodedContent).append(":").append(m[3]).append(";");
        }
        cc.send(Protocol.build(Protocol.HISTORY_RESP, with, sb.toString()));
    }

    private boolean auth(ClientConnection cc) { return cc.getUsername() != null; }
    private String  ts() { return new SimpleDateFormat("HH:mm:ss").format(new Date()); }
}
