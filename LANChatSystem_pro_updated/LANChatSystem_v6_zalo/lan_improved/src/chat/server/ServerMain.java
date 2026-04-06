package chat.server;

import chat.connection.ClientConnection;
import chat.protocol.Protocol;
import chat.server.handlers.*;
import chat.server.managers.*;
import chat.sql.DAO.*;
import chat.util.Logger;

/**
 * Server controller trung tâm.
 * Nhận raw lệnh từ ClientConnection → route sang đúng Handler/Manager.
 * Không chứa code UI hay socket — chỉ điều phối.
 */
public class ServerMain {

    // ── Tầng quản lý ─────────────────────────────────────────────────────
    private final ClientManager   clientManager = new ClientManager();

    // ── DAO ───────────────────────────────────────────────────────────────
    private final UserDAO    userDAO    = new UserDAO();
    private final MessageDAO msgDAO     = new MessageDAO();
    private final GroupDAO   groupDAO   = new GroupDAO();

    // ── Handlers ─────────────────────────────────────────────────────────
    private final AuthHandler     authHandler;
    private final MessageHandler  msgHandler;
    private final FileHandler     fileHandler;
    private final ImageHandler    imgHandler;
    private final AdminHandler    adminHandler;
    private final PollHandler     pollHandler;
    private final ReminderHandler reminderHandler;
    private final PinHandler      pinHandler;
    private final ReactionHandler reactionHandler;

    // ── Managers ─────────────────────────────────────────────────────────
    private final GroupManager    groupManager;
    private final ActivityTracker activityTracker;

    public ServerMain() {
        authHandler     = new AuthHandler(clientManager, userDAO);
        msgHandler      = new MessageHandler(clientManager, msgDAO, userDAO);
        fileHandler     = new FileHandler(clientManager, msgDAO);
        imgHandler      = new ImageHandler(clientManager);
        adminHandler    = new AdminHandler(clientManager, userDAO);
        pollHandler     = new PollHandler(clientManager);
        reminderHandler = new ReminderHandler(clientManager);
        pinHandler      = new PinHandler(clientManager);
        reactionHandler = new ReactionHandler(clientManager);
        groupManager    = new GroupManager(clientManager, groupDAO);
        activityTracker = new ActivityTracker(userDAO);

        userDAO.seedAdmin();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ── Dispatch ──────────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════
    public void dispatch(String raw, ClientConnection cc) {
        String[] p = Protocol.parse(raw);
        if (p.length == 0) return;

        switch (p[0]) {
            // Auth
            case Protocol.LOGIN         -> authHandler.handleLogin(p, cc);
            case Protocol.REGISTER      -> authHandler.handleRegister(p, cc);
            case Protocol.LOGOUT        -> onDisconnect(cc);
            case Protocol.CHANGE_PASS        -> authHandler.handleChangePass(p, cc);
            case Protocol.ADMIN_CHANGE_PASS  -> authHandler.handleAdminChangePass(p, cc);
            // Messages
            case Protocol.MSG           -> msgHandler.handleBroadcast(p, cc);
            case Protocol.PM            -> msgHandler.handlePM(p, cc);
            case Protocol.GROUP_MSG     -> msgHandler.handleGroupMsg(p, cc);
            case Protocol.READ          -> msgHandler.handleRead(p, cc);
            case Protocol.HISTORY_REQ   -> msgHandler.handleHistory(p, cc);
            // Groups
            case Protocol.CREATE_GROUP  -> groupManager.createGroup(p, cc);
            case Protocol.JOIN_GROUP    -> groupManager.joinGroup(p, cc);
            case Protocol.LEAVE_GROUP   -> groupManager.leaveGroup(p, cc);
            case Protocol.INVITE_GROUP  -> groupManager.inviteToGroup(p, cc);
            case Protocol.ADD_MEMBER    -> groupManager.addMember(p, cc);
            // Files
            case Protocol.FILE_OFFER    -> fileHandler.handleOffer(p, cc);
            case Protocol.FILE_ACCEPT   -> fileHandler.relay(p, Protocol.FILE_ACCEPT, cc);
            case Protocol.FILE_REJECT   -> fileHandler.relay(p, Protocol.FILE_REJECT, cc);
            case Protocol.FILE_DATA     -> fileHandler.relay(p, Protocol.FILE_DATA, cc);
            case Protocol.FILE_DONE     -> fileHandler.relay(p, Protocol.FILE_DONE, cc);
            // Images
            case Protocol.IMG_SEND      -> imgHandler.handleSend(p, cc);
            case Protocol.IMG_DATA      -> imgHandler.relay(p, Protocol.IMG_DATA, cc);
            case Protocol.IMG_DONE      -> imgHandler.relay(p, Protocol.IMG_DONE, cc);
            // Admin
            case Protocol.KICK          -> adminHandler.handleKick(p, cc);
            case Protocol.BAN           -> adminHandler.handleBan(p, cc);
            case Protocol.UNBAN         -> adminHandler.handleUnban(p, cc);
            case Protocol.BROADCAST     -> adminHandler.handleBroadcast(p, cc);
            case Protocol.CREATE_USER   -> adminHandler.handleCreateUser(p, cc);
            case Protocol.ASSIGN_ROLE        -> adminHandler.handleAssignRole(p, cc);
            // Conv Info
            case Protocol.CONV_INFO_REQ      -> handleConvInfoReq(p, cc);
            // Poll
            case Protocol.POLL_CREATE   -> pollHandler.handleCreate(p, cc);
            case Protocol.POLL_VOTE     -> pollHandler.handleVote(p, cc);
            case Protocol.POLL_CLOSE    -> pollHandler.handleClose(p, cc);
            // Reminder
            case Protocol.REMINDER_SET  -> reminderHandler.handleSet(p, cc);
            // Pin / Reaction
            case Protocol.PIN_MSG       -> pinHandler.handlePin(p, cc);
            case Protocol.UNPIN_MSG     -> pinHandler.handleUnpin(p, cc);
            case Protocol.REACT         -> reactionHandler.handleReact(p, cc);
            case Protocol.UNREACT       -> reactionHandler.handleUnreact(p, cc);
            default -> {}
        }

        // Heartbeat sau mỗi lệnh
        if (cc.getUsername() != null) activityTracker.heartbeat(cc.getUsername());
    }

    // ── Sự kiện ngắt kết nối ─────────────────────────────────────────────
    public void onDisconnect(ClientConnection cc) {
        if (cc.getUsername() != null) {
            activityTracker.setOffline(cc.getUsername());
            clientManager.unregister(cc);
            clientManager.broadcast(Protocol.build(Protocol.USER_LEAVE, cc.getUsername()), null);
            Logger.info(cc.getUsername() + " ngắt kết nối");
        }
    }

    // ── Xu ly yeu cau thong tin cuoc tro chuyen ───────────────────────────
    // Format: CONV_INFO_REQ || target || isGroup
    private void handleConvInfoReq(String[] p, ClientConnection cc) {
        if (p.length < 3 || cc.getUsername() == null) return;
        String  target  = p[1];
        boolean isGroup = "true".equalsIgnoreCase(p[2]);

        if (isGroup) {
            // Nhom chat: lay danh sach thanh vien + so file/anh
            java.util.List<String> members = userDAO.getGroupMembers(target);
            int[] media = userDAO.getGroupMediaCount(target);
            String memberStr = String.join(",", members);
            cc.send(Protocol.build(Protocol.CONV_INFO_RESP,
                target, "true",
                String.valueOf(media[0]),
                String.valueOf(media[1]),
                memberStr));
        } else {
            // Chat don: lay thong tin doi phuong + so file/anh
            String[] info = userDAO.getUserInfo(target);
            int[] media   = userDAO.getConvMediaCount(cc.getUsername(), target);
            // extra = "fullname|role|status"
            String extra = info[1] + "|" + info[2] + "|" + info[3];
            cc.send(Protocol.build(Protocol.CONV_INFO_RESP,
                target, "false",
                String.valueOf(media[0]),
                String.valueOf(media[1]),
                extra));
        }
    }

    // ── Getters cho UI ───────────────────────────────────────────────────
    public ClientManager getClientManager() { return clientManager; }
    public UserDAO       getUserDAO()        { return userDAO; }
}
