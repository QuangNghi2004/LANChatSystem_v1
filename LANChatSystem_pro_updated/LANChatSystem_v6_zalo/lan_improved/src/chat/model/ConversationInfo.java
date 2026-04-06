package chat.model;

public class ConversationInfo {
    private String target;       // username hoặc group name
    private String lastMessage;
    private String lastTs;
    private int    unreadCount;
    private boolean isGroup;

    public ConversationInfo(String target, boolean isGroup) {
        this.target = target; this.isGroup = isGroup;
    }

    public String  getTarget()              { return target; }
    public String  getLastMessage()         { return lastMessage; }
    public void    setLastMessage(String v) { lastMessage = v; }
    public String  getLastTs()              { return lastTs; }
    public void    setLastTs(String v)      { lastTs = v; }
    public int     getUnreadCount()         { return unreadCount; }
    public void    setUnreadCount(int v)    { unreadCount = v; }
    public void    incrementUnread()        { unreadCount++; }
    public boolean isGroup()               { return isGroup; }
}
