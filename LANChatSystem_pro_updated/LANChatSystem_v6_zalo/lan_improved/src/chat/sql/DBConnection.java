package chat.sql;

import chat.util.Config;
import chat.util.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton giữ một Connection tới MySQL.
 * Tất cả DAO đều lấy Connection từ đây.
 */
public class DBConnection {

    private static final String URL =
        "jdbc:mysql://" + Config.DB_HOST + ":" + Config.DB_PORT + "/" + Config.DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true"
        + "&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";

    private static Connection instance;

    public static Connection get() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, Config.DB_USER, Config.DB_PASS);
                instance.setAutoCommit(true);
                Logger.info("✅ Kết nối MySQL: " + Config.DB_HOST + "/" + Config.DB_NAME);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "❌ Không tìm thấy MySQL Connector!\n" +
                "→ Tải mysql-connector-j-*.jar và thêm vào Build Path.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                "❌ Không kết nối được MySQL!\n" +
                "→ Kiểm tra XAMPP đã bật MySQL chưa.\n" +
                "→ Đã import lanchat_mysql.sql chưa?", e);
        }
        return instance;
    }

    public static void close() {
        try { if (instance != null && !instance.isClosed()) instance.close(); }
        catch (SQLException ignored) {}
    }
}
