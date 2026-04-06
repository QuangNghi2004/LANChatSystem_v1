package chat.server.managers;

import chat.sql.DAO.UserDAO;

/** Cập nhật last_activity / heartbeat theo định kỳ. */
public class ActivityTracker {

    private final UserDAO userDAO;

    public ActivityTracker(UserDAO userDAO) { this.userDAO = userDAO; }

    public void heartbeat(String username) {
        userDAO.heartbeat(username);
    }

    public void setOffline(String username) {
        userDAO.setOffline(username);
    }
}
