package chat.server;

import chat.connection.ClientConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Theo dõi tất cả ClientConnection đang online + quản lý nhóm in-memory. */
public class ClientManager {

    private final Map<String, ClientConnection> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>>      groups   = new ConcurrentHashMap<>();

    // ── Session ───────────────────────────────────────────────────────────
    public synchronized void register(ClientConnection cc) {
        sessions.put(cc.getUsername(), cc);
    }

    public synchronized void unregister(ClientConnection cc) {
        if (cc.getUsername() != null) sessions.remove(cc.getUsername());
    }

    public boolean isOnline(String username) { return sessions.containsKey(username); }

    public ClientConnection getSession(String username) { return sessions.get(username); }

    public List<String> getOnlineUsers() { return new ArrayList<>(sessions.keySet()); }

    public Map<String, ClientConnection> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    // ── Broadcast ─────────────────────────────────────────────────────────
    public void broadcast(String msg, String exclude) {
        sessions.forEach((u, cc) -> { if (!u.equals(exclude)) cc.send(msg); });
    }

    public void broadcastAll(String msg) { sessions.values().forEach(cc -> cc.send(msg)); }

    public void broadcastGroup(String group, String msg, String exclude) {
        Set<String> members = groups.getOrDefault(group, Collections.emptySet());
        for (String m : members) {
            if (!m.equals(exclude)) {
                ClientConnection cc = sessions.get(m);
                if (cc != null) cc.send(msg);
            }
        }
    }

    // ── Nhóm ──────────────────────────────────────────────────────────────
    public void createGroup(String group, String creator) {
        groups.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(creator);
    }

    public void joinGroup(String group, String user) {
        groups.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(user);
    }

    public void leaveGroup(String group, String user) {
        Set<String> m = groups.get(group);
        if (m != null) m.remove(user);
    }

    public String getGroupListString() {
        StringBuilder sb = new StringBuilder();
        groups.forEach((g, members) ->
            sb.append(g).append(":").append(String.join(",", members)).append(";"));
        return sb.toString();
    }

    public Map<String, Set<String>> getAllGroups() {
        return Collections.unmodifiableMap(groups);
    }
}
