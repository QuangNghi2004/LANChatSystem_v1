package chat.server.handlers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;
import chat.sql.DAO.UserDAO;
import chat.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Xử lý: KICK, BAN, UNBAN, BROADCAST (admin), CREATE_USER, ASSIGN_ROLE, ADD_MEMBER. */
public class AdminHandler {

    private final ClientManager clients;
    private final UserDAO       userDAO;

    public AdminHandler(ClientManager clients, UserDAO userDAO) {
        this.clients = clients; this.userDAO = userDAO;
    }

    public void handleKick(String[] p, ClientConnection cc) {
        if (!cc.isAdmin() || p.length < 2) return;
        ClientConnection s = clients.getSession(p[1]);
        if (s != null) {
            s.send(Protocol.build(Protocol.KICKED, "Bạn đã bị kick bởi Admin"));
            Logger.info("Admin kick: " + p[1]);
        }
    }

    public void handleBan(String[] p, ClientConnection cc) {
        if (!cc.isAdmin() || p.length < 2) return;
        String reason = p.length >= 3 ? p[2] : "Vi phạm quy tắc";
        userDAO.ban(p[1], cc.getUsername(), reason);
        ClientConnection s = clients.getSession(p[1]);
        if (s != null) {
            s.send(Protocol.build(Protocol.BANNED, "Tài khoản của bạn đã bị cấm"));
        }
        Logger.info("Admin ban: " + p[1] + " — " + reason);
    }

    public void handleUnban(String[] p, ClientConnection cc) {
        if (!cc.isAdmin() || p.length < 2) return;
        userDAO.unban(p[1]);
        Logger.info("Admin unban: " + p[1]);
    }

    public void handleBroadcast(String[] p, ClientConnection cc) {
        if (!cc.isAdmin() || p.length < 2) return;
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        clients.broadcastAll(Protocol.build(Protocol.BROADCAST, p[1], ts));
        Logger.info("Admin broadcast: " + p[1]);
    }

    /**
     * Tạo tài khoản mới (chỉ admin).
     * Protocol: CREATE_USER || username || password || role || fullname
     * roles hợp lệ: truong_phong | pho_phong | nhan_vien | user
     */
    public void handleCreateUser(String[] p, ClientConnection cc) {
        if (!cc.isAdmin()) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Chỉ Admin mới có quyền tạo tài khoản"));
            return;
        }
        if (p.length < 4) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Thiếu thông tin tạo tài khoản"));
            return;
        }
        String username = p[1];
        String passHash = userDAO.sha256(p[2]);
        String role     = sanitizeRole(p[3]);
        String fullname = p.length >= 5 ? p[4] : username;

        boolean ok = userDAO.createUserFull(username, passHash, role, fullname);
        if (ok) {
            cc.send(Protocol.build(Protocol.USER_CREATED,
                username, role, userDAO.getRoleDisplay(role)));
            Logger.info("Admin tạo user: " + username + " [" + role + "]");
        } else {
            cc.send(Protocol.build(Protocol.OP_FAIL,
                "Tên đăng nhập \"" + username + "\" đã tồn tại"));
        }
    }

    /**
     * Phân quyền tài khoản (chỉ admin).
     * Protocol: ASSIGN_ROLE || username || newRole
     */
    public void handleAssignRole(String[] p, ClientConnection cc) {
        if (!cc.isAdmin()) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Chỉ Admin mới có quyền phân quyền"));
            return;
        }
        if (p.length < 3) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Thiếu thông tin phân quyền"));
            return;
        }
        String targetUser = p[1];
        String newRole    = sanitizeRole(p[2]);
        boolean ok = userDAO.updateRole(targetUser, newRole);
        if (ok) {
            cc.send(Protocol.build(Protocol.ROLE_ASSIGNED,
                targetUser, newRole, userDAO.getRoleDisplay(newRole)));
            // Thông báo cho user được phân quyền nếu đang online
            ClientConnection target = clients.getSession(targetUser);
            if (target != null) {
                target.setRole(newRole);
                target.send(Protocol.build(Protocol.AUTH_OK, targetUser, newRole));
            }
            Logger.info("Admin phân quyền: " + targetUser + " → " + newRole);
        } else {
            cc.send(Protocol.build(Protocol.OP_FAIL,
                "Không tìm thấy tài khoản \"" + targetUser + "\""));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private String sanitizeRole(String role) {
        if (role == null) return "nhan_vien";
        return switch (role.toLowerCase().trim()) {
            case "admin"        -> "admin";
            case "truong_phong" -> "truong_phong";
            case "pho_phong"    -> "pho_phong";
            case "nhan_vien"    -> "nhan_vien";
            default             -> "nhan_vien";
        };
    }
}
