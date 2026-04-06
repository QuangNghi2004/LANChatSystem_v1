package chat.sql.DAO;

import chat.sql.DBConnection;
import java.sql.*;

/** CRUD Group rooms + members. */
public class GroupDAO {

    public int ensureRoom(String roomName, String createdBy) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT id FROM rooms WHERE room_name=?");
            ps.setString(1, roomName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            PreparedStatement ins = DBConnection.get().prepareStatement(
                "INSERT INTO rooms(room_name,created_by) VALUES(?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ins.setString(1, roomName); ins.setString(2, createdBy);
            ins.executeUpdate();
            ResultSet k = ins.getGeneratedKeys();
            return k.next() ? k.getInt(1) : -1;
        } catch (SQLException e) { return -1; }
    }

    public void addMember(int roomId, String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT IGNORE INTO room_members(room_id,username) VALUES(?,?)");
            ps.setInt(1, roomId); ps.setString(2, username); ps.execute();
        } catch (SQLException ignored) {}
    }

    public void removeMember(int roomId, String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "DELETE FROM room_members WHERE room_id=? AND username=?");
            ps.setInt(1, roomId); ps.setString(2, username); ps.execute();
        } catch (SQLException ignored) {}
    }
}
