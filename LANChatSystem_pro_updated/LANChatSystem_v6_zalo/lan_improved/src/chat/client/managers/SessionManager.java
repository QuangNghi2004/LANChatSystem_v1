package chat.client.managers;

import chat.model.User;

/** Lưu thông tin phiên đăng nhập hiện tại. */
public class SessionManager {

    private static SessionManager instance;

    private String username;
    private String role;
    private String serverHost;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(String username, String role, String host) {
        this.username   = username;
        this.role       = role;
        this.serverHost = host;
    }

    public void logout() { username = null; role = null; }

    public String  getUsername()   { return username; }
    public String  getRole()       { return role; }
    public String  getServerHost() { return serverHost; }
    public boolean isLoggedIn()    { return username != null; }
    public boolean isAdmin()       { return "admin".equals(role); }
}
