package chat.model;

import java.time.LocalDateTime;

/**
 * Model người dùng.
 * roles: "admin" | "truong_phong" | "pho_phong" | "nhan_vien" | "user"
 */
public class User {
    private String        username;
    private String        password;
    private String        fullname;
    private String        email;
    private String        role;       // "admin" | "truong_phong" | "pho_phong" | "nhan_vien" | "user"
    private String        status;     // "online" | "offline"
    private boolean       banned;
    private String        empCode;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;

    public User() {}
    public User(String username, String role) {
        this.username = username; this.role = role; this.status = "offline";
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public String   getUsername()                 { return username; }
    public void     setUsername(String v)         { username = v; }
    public String   getPassword()                 { return password; }
    public void     setPassword(String v)         { password = v; }
    public String   getFullname()                 { return fullname == null ? username : fullname; }
    public void     setFullname(String v)         { fullname = v; }
    public String   getEmail()                    { return email; }
    public void     setEmail(String v)            { email = v; }
    public String   getRole()                     { return role; }
    public void     setRole(String v)             { role = v; }
    public String   getStatus()                   { return status; }
    public void     setStatus(String v)           { status = v; }
    public boolean  isBanned()                    { return banned; }
    public void     setBanned(boolean v)          { banned = v; }
    public String   getEmpCode()                  { return empCode; }
    public void     setEmpCode(String v)          { empCode = v; }
    public boolean  isAdmin()                     { return "admin".equals(role); }
    public boolean  isOnline()                    { return "online".equals(status); }

    /** Nhãn hiển thị cho role */
    public String getRoleDisplay() {
        if (role == null) return "Nhân viên";
        return switch (role) {
            case "admin"        -> "Quản trị viên";
            case "truong_phong" -> "Trưởng phòng";
            case "pho_phong"    -> "Phó phòng";
            case "nhan_vien"    -> "Nhân viên";
            default             -> "Nhân viên";
        };
    }

    @Override public String toString() { return username; }
}
