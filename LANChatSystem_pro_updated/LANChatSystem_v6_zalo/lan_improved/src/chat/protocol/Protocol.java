package chat.protocol;

/**
 * Toan bo lenh giao thuc Client <-> Server.
 * Phan cach bang "||". Them lenh moi chi can them hang so tai day.
 */
public class Protocol {

    // ── Auth ──────────────────────────────────────────────────────────────────
    public static final String LOGIN            = "LOGIN";
    public static final String REGISTER         = "REGISTER";
    public static final String AUTH_OK          = "AUTH_OK";
    public static final String AUTH_FAIL        = "AUTH_FAIL";
    public static final String LOGOUT           = "LOGOUT";
    public static final String CHANGE_PASS      = "CHANGE_PASS";
    public static final String ADMIN_CHANGE_PASS= "ADMIN_CHANGE_PASS"; // Admin doi pass user khac

    // ── Trang thai nguoi dung ─────────────────────────────────────────────────
    public static final String USER_LIST   = "USER_LIST";
    public static final String USER_JOIN   = "USER_JOIN";
    public static final String USER_LEAVE  = "USER_LEAVE";
    public static final String STATUS      = "STATUS";

    // ── Tin nhan ─────────────────────────────────────────────────────────────
    public static final String MSG         = "MSG";
    public static final String PM          = "PM";
    public static final String GROUP_MSG   = "GROUP_MSG";
    public static final String BROADCAST   = "BROADCAST";
    public static final String DELIVERED   = "DELIVERED";
    public static final String READ        = "READ";
    public static final String DELETE_MSG  = "DELETE_MSG";
    public static final String EDIT_MSG    = "EDIT_MSG";
    public static final String PIN_MSG     = "PIN_MSG";
    public static final String UNPIN_MSG   = "UNPIN_MSG";

    // ── Reaction ─────────────────────────────────────────────────────────────
    public static final String REACT        = "REACT";
    public static final String UNREACT      = "UNREACT";
    public static final String REACT_UPDATE = "REACT_UPDATE";

    // ── Nhom chat ────────────────────────────────────────────────────────────
    public static final String CREATE_GROUP  = "CREATE_GROUP";
    public static final String JOIN_GROUP    = "JOIN_GROUP";
    public static final String LEAVE_GROUP   = "LEAVE_GROUP";
    public static final String INVITE_GROUP  = "INVITE_GROUP";
    public static final String GROUP_LIST    = "GROUP_LIST";
    public static final String GROUP_CREATED = "GROUP_CREATED";
    public static final String GROUP_INFO    = "GROUP_INFO";
    public static final String ADD_MEMBER    = "ADD_MEMBER";
    public static final String REMOVE_MEMBER = "REMOVE_MEMBER";

    // ── File ─────────────────────────────────────────────────────────────────
    public static final String FILE_OFFER   = "FILE_OFFER";
    public static final String FILE_ACCEPT  = "FILE_ACCEPT";
    public static final String FILE_REJECT  = "FILE_REJECT";
    public static final String FILE_DATA    = "FILE_DATA";
    public static final String FILE_DONE    = "FILE_DONE";

    // ── Anh ──────────────────────────────────────────────────────────────────
    public static final String IMG_SEND     = "IMG_SEND";
    public static final String IMG_DATA     = "IMG_DATA";
    public static final String IMG_DONE     = "IMG_DONE";

    // ── Poll ─────────────────────────────────────────────────────────────────
    public static final String POLL_CREATE  = "POLL_CREATE";
    public static final String POLL_VOTE    = "POLL_VOTE";
    public static final String POLL_CLOSE   = "POLL_CLOSE";
    public static final String POLL_RESULT  = "POLL_RESULT";

    // ── Reminder ─────────────────────────────────────────────────────────────
    public static final String REMINDER_SET = "REMINDER_SET";
    public static final String REMINDER_FIRE= "REMINDER_FIRE";

    // ── Admin ─────────────────────────────────────────────────────────────────
    public static final String KICK          = "KICK";
    public static final String BAN           = "BAN";
    public static final String UNBAN         = "UNBAN";
    public static final String KICKED        = "KICKED";
    public static final String BANNED        = "BANNED";
    public static final String CREATE_USER   = "CREATE_USER";
    public static final String ASSIGN_ROLE   = "ASSIGN_ROLE";
    public static final String USER_CREATED  = "USER_CREATED";
    public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
    public static final String OP_FAIL       = "OP_FAIL";
    public static final String OP_OK         = "OP_OK";

    // ── Lich su ──────────────────────────────────────────────────────────────
    public static final String HISTORY_REQ  = "HISTORY_REQ";
    public static final String HISTORY_RESP = "HISTORY_RESP";

    // ── Thong tin cuoc tro chuyen ─────────────────────────────────────────────
    public static final String CONV_INFO_REQ  = "CONV_INFO_REQ";  // client yeu cau
    public static final String CONV_INFO_RESP = "CONV_INFO_RESP"; // server tra loi
    // Format CONV_INFO_REQ: CONV_INFO_REQ || target || isGroup(true/false)
    // Format CONV_INFO_RESP: CONV_INFO_RESP || target || isGroup || fileCount || imageCount || extra
    // extra: neu la group: "member1,member2,..." | neu la dm: "fullname|role|status"

    // ── Phan cach ────────────────────────────────────────────────────────────
    public static final String SEP       = "||";
    public static final String SEP_REGEX = "\\|\\|";

    public static String build(String... parts) {
        return String.join(SEP, parts);
    }

    public static String[] parse(String raw) {
        if (raw == null || raw.isBlank()) return new String[0];
        return raw.split(SEP_REGEX, -1);
    }
}
