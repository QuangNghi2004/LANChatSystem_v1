package chat.server.handlers;

import chat.server.ClientConnection;
import chat.database.GroupDAO;
import chat.database.JoinRequestDAO;
import chat.database.UserDAO;
import chat.server.ClientManager;

public class GroupJoinHandler {
    private ClientManager clients;
    private GroupDAO groupDAO;
    private JoinRequestDAO joinDAO;
    private UserDAO userDAO;

    public GroupJoinHandler(ClientManager clients, GroupDAO groupDAO, JoinRequestDAO joinDAO, UserDAO userDAO) {
        this.clients = clients;
        this.groupDAO = groupDAO;
        this.joinDAO = joinDAO;
        this.userDAO = userDAO;
    }

    public void handleJoinRequest(String[] p, ClientConnection cc) {
        // Logic to handle a join request
        // Validate request, update database, notify clients, etc.
    }

    public void handleApprove(String[] p, ClientConnection cc) {
        // Logic to approve a join request
        // Update database, notify clients, etc.
    }

    public void handleReject(String[] p, ClientConnection cc) {
        // Logic to reject a join request
        // Update database, notify clients, etc.
    }
}