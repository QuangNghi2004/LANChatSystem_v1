package chat.server.managers;

/**
 * Cloud storage logic — placeholder.
 * Mở rộng: lưu file lên cloud (local folder / S3), trả URL cho client.
 */
public class CloudManager {
    public String upload(String filename, byte[] data) {
        // TODO: lưu vào thư mục cloud/ trên server hoặc S3
        return null;
    }

    public byte[] download(String fileId) {
        // TODO
        return null;
    }
}
