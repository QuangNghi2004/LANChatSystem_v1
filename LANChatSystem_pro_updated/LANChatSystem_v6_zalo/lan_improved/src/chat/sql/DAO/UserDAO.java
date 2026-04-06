package chat.sql.DAO;

import chat.model.User;
import chat.sql.DBConnection;
import chat.util.Config;

import java.sql.*;
import java.util.*;

/** CRUD Users + ban/unban + role + session tracking. */
public class UserDAO {

    // ── Auth ──────────────────────────────────────────────────────────────
    public boolean register(String username, String passHash) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT IGNORE INTO users(username,password,role) VALUES(?,?,?)");
            ps.setString(1, username);
            ps.setString(2, passHash);
            ps.setString(3, "user");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean login(String username, String passHash) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT password FROM users WHERE username=? AND banned=0");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getString(1).equals(passHash);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean changePassword(String username, String newHash) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE users SET password=? WHERE username=?");
            ps.setString(1, newHash);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // ── Online / Offline ──────────────────────────────────────────────────
    public void setOnline(String username, String ip) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE users SET status='online', last_seen=NOW() WHERE username=?");
            ps.setString(1, username);
            ps.execute();

            DBConnection.get().prepareStatement(
                    "DELETE FROM sessions WHERE username=?")
                    .execute();
            PreparedStatement ins = DBConnection.get().prepareStatement(
                    "INSERT INTO sessions(session_id,username,ip_address,is_online) VALUES(?,?,?,1)");
            ins.setString(1, UUID.randomUUID().toString());
            ins.setString(2, username);
            ins.setString(3, ip != null ? ip : "unknown");
            ins.execute();
        } catch (SQLException ignored) {
        }
    }

    public void setOffline(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE users SET status='offline', last_seen=NOW() WHERE username=?");
            ps.setString(1, username);
            ps.execute();
            PreparedStatement ps2 = DBConnection.get().prepareStatement(
                    "UPDATE sessions SET is_online=0 WHERE username=?");
            ps2.setString(1, username);
            ps2.execute();
        } catch (SQLException ignored) {
        }
    }

    public void heartbeat(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE sessions SET last_activity=NOW() WHERE username=? AND is_online=1");
            ps.setString(1, username);
            ps.execute();
        } catch (SQLException ignored) {
        }
    }

    // ── Thông tin ─────────────────────────────────────────────────────────
    public String getRole(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT role FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "user";
        } catch (SQLException e) {
            return "user";
        }
    }

    public boolean isBanned(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT banned FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        try {
            ResultSet rs = DBConnection.get().createStatement()
                    .executeQuery(
                            "SELECT username,IFNULL(fullname,''),role,status,banned FROM users ORDER BY username");
            while (rs.next()) {
                User u = new User();
                u.setUsername(rs.getString(1));
                u.setFullname(rs.getString(2));
                u.setRole(rs.getString(3));
                u.setStatus(rs.getString(4));
                u.setBanned(rs.getInt(5) == 1);
                list.add(u);
            }
        } catch (SQLException ignored) {
        }
        return list;
    }

    // ── Ban / Unban ───────────────────────────────────────────────────────
    /** INSERT vào bảng bans → trigger tự cập nhật users.banned=1. */
    public void ban(String username, String bannedBy, String reason) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT INTO bans(username,banned_by,reason,is_permanent) VALUES(?,?,?,1)");
            ps.setString(1, username);
            ps.setString(2, bannedBy);
            ps.setString(3, reason);
            ps.execute();
        } catch (SQLException ignored) {
        }
    }

    /** DELETE bans → trigger tự cập nhật users.banned=0. */
    public void unban(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "DELETE FROM bans WHERE username=?");
            ps.setString(1, username);
            ps.execute();
        } catch (SQLException ignored) {
        }
    }

    // ── Tạo tài khoản từ admin ────────────────────────────────────────────
    public boolean createUser(String username, String passHash, String role) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT IGNORE INTO users(username,password,role) VALUES(?,?,?)");
            ps.setString(1, username);
            ps.setString(2, passHash);
            ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Tạo user với đầy đủ thông tin (admin sử dụng). */
    public boolean createUserFull(String username, String passHash, String role, String fullname) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT IGNORE INTO users(username,password,role,fullname) VALUES(?,?,?,?)");
            ps.setString(1, username);
            ps.setString(2, passHash);
            ps.setString(3, role);
            ps.setString(4, fullname != null ? fullname : username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Cập nhật role cho user (admin phân quyền). */
    public boolean updateRole(String username, String newRole) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "UPDATE users SET role=? WHERE username=?");
            ps.setString(1, newRole);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /** Lấy tên hiển thị của role. */
    public String getRoleDisplay(String role) {
        if (role == null)
            return "Nhân viên";
        return switch (role) {
            case "admin" -> "Quản trị viên";
            case "truong_phong" -> "Trưởng phòng";
            case "pho_phong" -> "Phó phòng";
            case "nhan_vien" -> "Nhân viên";
            default -> "Nhân viên";
        };
    }

    public void seedAdmin() {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "INSERT IGNORE INTO users(username,password,role,fullname) VALUES(?,?,?,?)");
            ps.setString(1, Config.ADMIN_USER);
            ps.setString(2, sha256(Config.ADMIN_PASS));
            ps.setString(3, "admin");
            ps.setString(4, "Administrator");
            ps.execute();
        } catch (SQLException ignored) {
        }
    }

    public String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b)
                sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    /** Lay thong tin mot user (de hien thi trong conv info). */
    public String[] getUserInfo(String username) {
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT username, IFNULL(fullname,username), role, status FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[] {
                        rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4)
                };
            }
        } catch (SQLException e) {
            /* ignore */ }
        return new String[] { username, username, "nhan_vien", "offline" };
    }

    /** Lay danh sach thanh vien cua mot phong nhom. */
    public java.util.List<String> getGroupMembers(String roomName) {
        java.util.List<String> members = new java.util.ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT rm.username FROM room_members rm " +
                            "JOIN rooms r ON r.id = rm.room_id " +
                            "WHERE r.room_name=? ORDER BY rm.username");
            ps.setString(1, roomName);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                members.add(rs.getString(1));
        } catch (SQLException e) {
            /* ignore */ }
        return members;
    }

    /** Dem so file va anh trong cuoc tro chuyen private. */
    public int[] getConvMediaCount(String u1, String u2) {
        // Returns [fileCount, imageCount]
        // Su dung bang messages de dem - file co type=file, image co type=image
        int fileCount = 0, imgCount = 0;
        try {
            // Dem trong private_messages bang content prefix
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT content FROM private_messages " +
                            "WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) " +
                            "AND is_deleted_sender=0 AND is_deleted_receiver=0");
            ps.setString(1, u1);
            ps.setString(2, u2);
            ps.setString(3, u2);
            ps.setString(4, u1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String c = rs.getString(1);
                if (c != null) {
                    if (c.startsWith("[FILE]"))
                        fileCount++;
                    else if (c.startsWith("[IMG]") || c.startsWith("[IMAGE]"))
                        imgCount++;
                }
            }
        } catch (SQLException e) {
            /* ignore */ }
        return new int[] { fileCount, imgCount };
    }

    /** Dem so file va anh trong group chat. */
    public int[] getGroupMediaCount(String roomName) {
        int fileCount = 0, imgCount = 0;
        try {
            PreparedStatement ps = DBConnection.get().prepareStatement(
                    "SELECT m.content FROM messages m " +
                            "JOIN rooms r ON r.id = m.room_id " +
                            "WHERE r.room_name=? AND m.is_deleted=0");
            ps.setString(1, roomName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String c = rs.getString(1);
                if (c != null) {
                    if (c.startsWith("[FILE]"))
                        fileCount++;
                    else if (c.startsWith("[IMG]") || c.startsWith("[IMAGE]"))
                        imgCount++;
                }
            }
        } catch (SQLException e) {
            /* ignore */ }
        return new int[] { fileCount, imgCount };
    }
}