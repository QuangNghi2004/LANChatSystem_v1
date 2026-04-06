package chat.util;

/** Kiểm tra đầu vào người dùng. */
public class Validator {

    public static boolean isValidUsername(String u) {
        return u != null && u.matches("[a-zA-Z0-9_]{3,30}");
    }

    public static boolean isValidPassword(String p) {
        return p != null && p.length() >= 4 && p.length() <= 64;
    }

    public static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank();
    }

    public static boolean isValidGroupName(String g) {
        return g != null && g.matches("[\\wÀ-ỹ ]{2,30}");
    }

    /** Cắt ngắn chuỗi nếu quá dài. */
    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
