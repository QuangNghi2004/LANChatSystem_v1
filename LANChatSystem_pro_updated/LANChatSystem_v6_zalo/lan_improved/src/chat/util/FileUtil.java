package chat.util;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;

/** Tiện ích xử lý file. */
public class FileUtil {

    public static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    public static boolean isImage(String ext) {
        return ext.matches("png|jpg|jpeg|gif|bmp|webp|tiff|ico");
    }

    public static boolean isText(String ext) {
        return ext.matches("txt|log|md|csv|json|xml|html|htm|java|py|c|cpp|" +
                           "js|ts|sql|yaml|yml|toml|ini|cfg|sh|bat|properties");
    }

    public static void write(File dest, byte[] data) throws IOException {
        Files.write(dest.toPath(), data);
    }
}
