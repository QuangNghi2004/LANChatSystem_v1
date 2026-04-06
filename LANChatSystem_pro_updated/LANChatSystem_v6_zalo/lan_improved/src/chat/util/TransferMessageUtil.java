package chat.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Helper cho message đính kèm file lưu trong chat/history. */
public final class TransferMessageUtil {

    private static final String FILE_PREFIX = "__FILE__";

    private TransferMessageUtil() {}

    public static String buildFileMessage(String transferId, String fileName, long size) {
        String encodedName = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(fileName.getBytes(StandardCharsets.UTF_8));
        return FILE_PREFIX + "|" + transferId + "|" + encodedName + "|" + size;
    }

    public static FileMessage parseFileMessage(String text) {
        if (text == null || !text.startsWith(FILE_PREFIX + "|")) return null;
        String[] parts = text.split("\\|", 4);
        if (parts.length != 4) return null;
        try {
            String fileName = new String(
                Base64.getUrlDecoder().decode(parts[2]),
                StandardCharsets.UTF_8);
            long size = Long.parseLong(parts[3]);
            return new FileMessage(parts[1], fileName, size);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public record FileMessage(String transferId, String fileName, long size) {}
}
