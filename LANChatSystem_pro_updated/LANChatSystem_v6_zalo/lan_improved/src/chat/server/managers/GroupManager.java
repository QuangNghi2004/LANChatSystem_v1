package chat.server.managers;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.ClientManager;
import chat.sql.DAO.GroupDAO;
import chat.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/** CRUD nhóm chat + phân quyền thành viên. Chỉ Admin mới tạo nhóm và thêm thành viên. */
public class GroupManager {

    private final ClientManager clients;
    private final GroupDAO      groupDAO;

    public GroupManager(ClientManager clients, GroupDAO groupDAO) {
        this.clients = clients; this.groupDAO = groupDAO;
    }

    /**
     * Tạo nhóm mới — CHỈ ADMIN.
     * Protocol: CREATE_GROUP || groupName
     */
    public void createGroup(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 2) return;

        // Kiểm tra quyền admin
        if (!cc.isAdmin()) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Chỉ Admin mới có quyền tạo nhóm"));
            Logger.info(cc.getUsername() + " cố tạo nhóm nhưng không có quyền");
            return;
        }

        String group = p[1].trim();
        if (group.isEmpty()) return;

        clients.createGroup(group, cc.getUsername());
        int roomId = groupDAO.ensureRoom(group, cc.getUsername());
        groupDAO.addMember(roomId, cc.getUsername());
        clients.broadcastAll(Protocol.build(Protocol.GROUP_CREATED, group, cc.getUsername()));
        Logger.info(cc.getUsername() + " (Admin) tạo nhóm: " + group);
    }

    public void joinGroup(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 2) return;
        String group = p[1];
        clients.joinGroup(group, cc.getUsername());
        groupDAO.addMember(groupDAO.ensureRoom(group, cc.getUsername()), cc.getUsername());
        clients.broadcastGroup(group,
            Protocol.build(Protocol.GROUP_MSG, group, "System",
                cc.getUsername() + " đã tham gia nhóm", ts()), null);
    }

    public void leaveGroup(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 2) return;
        String group = p[1];
        clients.leaveGroup(group, cc.getUsername());
        groupDAO.removeMember(groupDAO.ensureRoom(group, cc.getUsername()), cc.getUsername());
        clients.broadcastGroup(group,
            Protocol.build(Protocol.GROUP_MSG, group, "System",
                cc.getUsername() + " đã rời nhóm", ts()), null);
    }

    public void inviteToGroup(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 3) return;
        ClientConnection target = clients.getSession(p[2]);
        if (target != null)
            target.send(Protocol.build(Protocol.INVITE_GROUP, p[1], cc.getUsername()));
    }

    /**
     * Thêm thành viên vào nhóm — CHỈ ADMIN.
     * Protocol: ADD_MEMBER || groupName || targetUsername
     */
    public void addMember(String[] p, ClientConnection cc) {
        if (cc.getUsername() == null || p.length < 3) return;

        // Kiểm tra quyền admin
        if (!cc.isAdmin()) {
            cc.send(Protocol.build(Protocol.OP_FAIL, "Chỉ Admin mới có quyền thêm thành viên"));
            Logger.info(cc.getUsername() + " cố thêm thành viên nhưng không có quyền");
            return;
        }

        String group      = p[1].trim();
        String newMember  = p[2].trim();

        // Thêm vào DB và memory
        int roomId = groupDAO.ensureRoom(group, cc.getUsername());
        groupDAO.addMember(roomId, newMember);
        clients.joinGroup(group, newMember);

        // Gửi thông báo cho thành viên mới nếu đang online
        ClientConnection targetConn = clients.getSession(newMember);
        if (targetConn != null) {
            targetConn.send(Protocol.build(Protocol.GROUP_CREATED, group, cc.getUsername()));
        }

        // Thông báo trong nhóm
        clients.broadcastGroup(group,
            Protocol.build(Protocol.GROUP_MSG, group, "System",
                newMember + " đã được Admin thêm vào nhóm", ts()), null);

        // Phản hồi cho admin
        cc.send(Protocol.build(Protocol.GROUP_MSG, group, "System",
            "Đã thêm " + newMember + " vào nhóm " + group, ts()));

        Logger.info("Admin " + cc.getUsername() + " thêm " + newMember + " vào nhóm: " + group);
    }

    private String ts() { return new SimpleDateFormat("HH:mm:ss").format(new Date()); }
}
