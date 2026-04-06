package chat.client.managers;

import chat.client.ConnectionManager;
import chat.protocol.Protocol;
import chat.util.Config;
import chat.util.TransferMessageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Upload/download file giữa các client qua server relay. */
public class FileManager {

    private final ConnectionManager          conn;
    private final Consumer<ReceivedFile>     onReceived;
    private final Consumer<SentFile>         onSent;
    private final BiConsumer<String, String> onProgress;

    private final Map<String, ByteArrayOutputStream> incoming = new ConcurrentHashMap<>();
    private final Map<String, IncomingTransfer> incomingMeta = new ConcurrentHashMap<>();
    private final Map<String, OutgoingTransfer> outgoing = new ConcurrentHashMap<>();

    public FileManager(ConnectionManager conn,
                       Consumer<ReceivedFile> onReceived,
                       Consumer<SentFile> onSent,
                       BiConsumer<String, String> onProgress) {
        this.conn = conn;
        this.onReceived = onReceived;
        this.onSent = onSent;
        this.onProgress = onProgress;
    }

    public void offerFile(File file, String receiver) {
        if (file.length() > Config.MAX_FILE_SIZE) {
            onProgress.accept(receiver, "File quá lớn (tối đa 10MB)");
            return;
        }

        String transferId = UUID.randomUUID().toString();
        outgoing.put(key(receiver, transferId), new OutgoingTransfer(file, receiver, transferId));
        conn.send(Protocol.build(
            Protocol.FILE_OFFER,
            receiver,
            file.getName(),
            String.valueOf(file.length()),
            transferId));
    }

    public void onAccepted(String receiver, String fileName, String transferId) {
        new Thread(() -> sendChunks(receiver, fileName, transferId), "file-send").start();
    }

    private void sendChunks(String receiver, String fileName, String transferId) {
        OutgoingTransfer transfer = outgoing.get(key(receiver, transferId));
        if (transfer == null) {
            onProgress.accept(receiver, "Không tìm thấy file để gửi");
            return;
        }

        try {
            byte[] data = Files.readAllBytes(transfer.file().toPath());
            int offset = 0;
            while (offset < data.length) {
                int len = Math.min(Config.FILE_CHUNK, data.length - offset);
                byte[] chunk = Arrays.copyOfRange(data, offset, offset + len);
                conn.send(Protocol.build(
                    Protocol.FILE_DATA,
                    receiver,
                    fileName,
                    Base64.getEncoder().encodeToString(chunk),
                    transferId));
                offset += len;
                Thread.sleep(5);
            }

            conn.send(Protocol.build(Protocol.FILE_DONE, receiver, fileName, transferId));
            File cachedFile = cacheFile(transferId, fileName, data);
            outgoing.remove(key(receiver, transferId));

            if (onSent != null) {
                onSent.accept(new SentFile(
                    receiver,
                    TransferMessageUtil.buildFileMessage(transferId, fileName, transfer.file().length()),
                    cachedFile));
            }
        } catch (Exception e) {
            onProgress.accept(receiver, "Lỗi gửi file: " + e.getMessage());
        }
    }

    public void prepareIncoming(String from, String fileName, long size, String transferId) {
        incoming.put(key(from, transferId), new ByteArrayOutputStream());
        incomingMeta.put(key(from, transferId), new IncomingTransfer(from, fileName, size, transferId));
    }

    public void onChunk(String from, String fileName, String b64, String transferId) {
        ByteArrayOutputStream buf =
            incoming.computeIfAbsent(key(from, transferId), k -> new ByteArrayOutputStream());
        try {
            buf.write(Base64.getDecoder().decode(b64));
        } catch (Exception ignored) {
        }
    }

    public void onDone(String from, String fileName, String transferId) {
        ByteArrayOutputStream buf = incoming.remove(key(from, transferId));
        IncomingTransfer meta = incomingMeta.remove(key(from, transferId));
        if (buf == null) return;

        byte[] data = buf.toByteArray();
        try {
            File cachedFile = cacheFile(transferId, fileName, data);
            long size = meta != null ? meta.size() : data.length;
            if (onReceived != null) {
                onReceived.accept(new ReceivedFile(
                    from,
                    TransferMessageUtil.buildFileMessage(transferId, fileName, size),
                    cachedFile,
                    data));
            }
        } catch (IOException e) {
            onProgress.accept(from, "Không thể lưu file nhận: " + e.getMessage());
        }
    }

    public File resolveCachedFile(String transferId, String fileName) {
        Path path = cacheDir().resolve(transferId + "_" + sanitize(fileName));
        return Files.exists(path) ? path.toFile() : null;
    }

    private File cacheFile(String transferId, String fileName, byte[] data) throws IOException {
        Path dir = cacheDir();
        Files.createDirectories(dir);
        Path path = dir.resolve(transferId + "_" + sanitize(fileName));
        Files.write(path, data);
        return path.toFile();
    }

    private Path cacheDir() {
        return Path.of(System.getProperty("user.home"), ".lanchat-cache");
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String key(String peer, String transferId) {
        return peer + "_" + transferId;
    }

    public record ReceivedFile(String from, String messageText, File cachedFile, byte[] data) {}
    public record SentFile(String to, String messageText, File cachedFile) {}
    private record IncomingTransfer(String from, String fileName, long size, String transferId) {}
    private record OutgoingTransfer(File file, String receiver, String transferId) {}
}
