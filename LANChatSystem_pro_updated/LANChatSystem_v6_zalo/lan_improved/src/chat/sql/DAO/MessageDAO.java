package chat.sql.DAO;

import chat.model.Message;
import chat.sql.DBConnection;

import java.sql.*;
import java.util.*;

/** CRUD Messages (broadcast + PM + group) + pin + delete. */
public class MessageDAO {

    private int getLobbyRoomId() {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "SELECT id FROM rooms WHERE room_name='Lobby'");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            PreparedStatement ins = DBConnection.get().prepareStatement(
                "INSERT INTO rooms(room_name,created_by) VALUES('Lobby','admin')",
                Statement.RETURN_GENERATED_KEYS);
            ins.executeUpdate();
            ResultSet k = ins.getGeneratedKeys();
            return k.next() ? k.getInt(1) : -1;
        } catch (SQLException e) { return -1; }
    }

    public int getRoomId(String roomName, String createdBy) {
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

    public void saveBroadcast(String sender, String content) {
        saveRoom(sender, getLobbyRoomId(), content, "broadcast");
    }

    public void saveGroupMessage(String sender, String group, String content) {
        saveRoom(sender, getRoomId(group, sender), content, "group");
    }

    private void saveRoom(String sender, int roomId, String content, String type) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT INTO messages(room_id,sender,content,type) VALUES(?,?,?,?)");
            ps.setInt(1, roomId); ps.setString(2, sender);
            ps.setString(3, content); ps.setString(4, type);
            ps.execute();
        } catch (SQLException e) { System.err.println("[MessageDAO] " + e.getMessage()); }
    }

    public void savePrivateMessage(String sender, String receiver, String content) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "INSERT INTO private_messages(sender,receiver,content) VALUES(?,?,?)");
            ps.setString(1, sender); ps.setString(2, receiver); ps.setString(3, content);
            ps.execute();
        } catch (SQLException e) { System.err.println("[MessageDAO] " + e.getMessage()); }
    }

    public void markRead(String sender, String receiver) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                "UPDATE private_messages SET is_read=1 WHERE sender=? AND receiver=? AND is_read=0");
            ps.setString(1, sender); ps.setString(2, receiver); ps.execute();
        } catch (SQLException ignored) {}
    }

    public List<String[]> getPrivateHistory(String u1, String u2) {
        List<String[]> list = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement("""
                SELECT sender,receiver,content,DATE_FORMAT(created_at,'%H:%i:%s')
                FROM private_messages
                WHERE (sender=? AND receiver=? AND is_deleted_sender=0)
                   OR (sender=? AND receiver=? AND is_deleted_receiver=0)
                ORDER BY id DESC LIMIT 50""");
            ps.setString(1,u1); ps.setString(2,u2);
            ps.setString(3,u2); ps.setString(4,u1);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)});
            Collections.reverse(list);
        } catch (SQLException e) { System.err.println("[MessageDAO] " + e.getMessage()); }
        return list;
    }

    public List<String[]> getLobbyHistory() {
        List<String[]> list = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement("""
                SELECT sender,content,DATE_FORMAT(created_at,'%H:%i:%s')
                FROM messages WHERE room_id=? AND is_deleted=0
                ORDER BY id DESC LIMIT 50""");
            ps.setInt(1, getLobbyRoomId());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString(1),rs.getString(2),rs.getString(3)});
            Collections.reverse(list);
        } catch (SQLException e) { System.err.println("[MessageDAO] " + e.getMessage()); }
        return list;
    }

    /** Dùng chung từ ClientSession. */
    public List<String[]> getHistory(String user, String with) {
        if (with == null || with.equals("ALL")) return getLobbyHistory();
        return getPrivateHistory(user, with);
    }
}
