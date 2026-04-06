package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;
import chat.sql.DAO.UserDAO;
import chat.util.Logger;

/** Xu ly: LOGIN, REGISTER, LOGOUT, CHANGE_PASS, ADMIN_CHANGE_PASS. */
public class AuthHandler {

    private final ClientManager clients;
    private final UserDAO       userDAO;

    public AuthHandler(ClientManager clients, UserDAO userDAO) {
        this.clients = clients; this.userDAO = userDAO;
    }

    public void handleLogin(String[] p, ClientConnection cc) {
        if (p.length < 3) { cc.send(Protocol.build(Protocol.AUTH_FAIL, "Sai dinh dang")); return; }
        String user = p[1], pass = p[2];
        if (userDAO.isBanned(user))           { cc.send(Protocol.build(Protocol.AUTH_FAIL, "Tai khoan bi cam!")); return; }
        if (!userDAO.login(user, userDAO.sha256(pass)))
                                               { cc.send(Protocol.build(Protocol.AUTH_FAIL, "Sai ten dang nhap hoac mat khau")); return; }
        if (clients.isOnline(user))            { cc.send(Protocol.build(Protocol.AUTH_FAIL, "Tai khoan dang dang nhap tu may khac")); return; }

        cc.setUsername(user);
        cc.setRole(userDAO.getRole(user));
        userDAO.setOnline(user, cc.getIp());
        clients.register(cc);

        cc.send(Protocol.build(Protocol.AUTH_OK, user, cc.getRole()));
        cc.send(Protocol.build(Protocol.USER_LIST, String.join(",", clients.getOnlineUsers())));
        cc.send(Protocol.build(Protocol.GROUP_LIST, clients.getGroupListString()));
        clients.broadcast(Protocol.build(Protocol.USER_JOIN, user), user);
        Logger.info(user + " dang nhap tu " + cc.getIp());
    }

    public void handleRegister(String[] p, ClientConnection cc) {
        if (p.length < 3) { cc.send(Protocol.build(Protocol.AUTH_FAIL, "Sai dinh dang")); return; }
        String user = p[1], pass = p[2];
        if (userDAO.register(user, userDAO.sha256(pass))) {
            cc.send(Protocol.build(Protocol.AUTH_OK, user, "user"));
            Logger.info(user + " dang ky tai khoan moi");
        } else {
            cc.send(Protocol.build(Protocol.AUTH_FAIL, "Ten dang nhap da ton tai"));
        }
    }

    /**
     * Doi mat khau - user thuong phai nhap mat khau cu.
     * Protocol: CHANGE_PASS || oldPassword || newPassword
     */
    public void handleChangePass(String[] p, ClientConnection cc) {
        if (p.length < 3 || cc.getUsername() == null) {
            cc.send(Protocol.build(Protocol.AUTH_FAIL, "Thieu thong tin"));
            return;
        }
        String oldPass = p[1];
        String newPass = p[2];

        // Xac minh mat khau cu
        boolean oldOk = userDAO.login(cc.getUsername(), userDAO.sha256(oldPass));
        if (!oldOk) {
            cc.send(Protocol.build(Protocol.AUTH_FAIL, "Mat khau cu khong chinh xac"));
            return;
        }

        boolean ok = userDAO.changePassword(cc.getUsername(), userDAO.sha256(newPass));
        if (ok) {
            cc.send(Protocol.build(Protocol.OP_OK, "Doi mat khau thanh cong"));
            Logger.info(cc.getUsername() + " doi mat khau thanh cong");
        } else {
            cc.send(Protocol.build(Protocol.AUTH_FAIL, "Doi mat khau that bai"));
        }
    }

    /**
     * Admin doi mat khau user bat ky - khong can mat khau cu.
     * Protocol: ADMIN_CHANGE_PASS || targetUsername || newPassword
     */
    public void handleAdminChangePass(String[] p, ClientConnection cc) {
        if (!cc.isAdmin()) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Chi Admin moi co quyen doi mat khau nguoi dung khac"));
            return;
        }
        if (p.length < 3) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Thieu thong tin"));
            return;
        }
        String targetUser = p[1];
        String newPass    = p[2];

        boolean ok = userDAO.changePassword(targetUser, userDAO.sha256(newPass));
        if (ok) {
            cc.send(Protocol.build(Protocol.OP_OK, "Da doi mat khau cua " + targetUser + " thanh cong"));
            Logger.info("Admin " + cc.getUsername() + " doi mat khau cua " + targetUser);
        } else {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Khong tim thay tai khoan: " + targetUser));
        }
    }
}
