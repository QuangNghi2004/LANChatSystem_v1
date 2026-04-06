package chat.model;

import java.time.LocalDateTime;

public class Message {
    public enum Type { BROADCAST, PM, GROUP, SYSTEM }

    private int           id;
    private String        sender;
    private String        target;     // username, group, "ALL"
    private String        content;
    private Type          type;
    private String        ts;         // "HH:mm:ss"
    private boolean       pinned;
    private boolean       deleted;
    private String        quotedContent; // trích dẫn
    private LocalDateTime createdAt;

    public Message() {}
    public Message(String sender, String target, String content, Type type, String ts) {
        this.sender  = sender;  this.target  = target;
        this.content = content; this.type    = type;  this.ts = ts;
    }

    public int    getId()                   { return id; }
    public void   setId(int v)              { id = v; }
    public String getSender()               { return sender; }
    public void   setSender(String v)       { sender = v; }
    public String getTarget()               { return target; }
    public void   setTarget(String v)       { target = v; }
    public String getContent()              { return content; }
    public void   setContent(String v)      { content = v; }
    public Type   getType()                 { return type; }
    public void   setType(Type v)           { type = v; }
    public String getTs()                   { return ts; }
    public void   setTs(String v)           { ts = v; }
    public boolean isPinned()               { return pinned; }
    public void   setPinned(boolean v)      { pinned = v; }
    public boolean isDeleted()              { return deleted; }
    public void   setDeleted(boolean v)     { deleted = v; }
    public String getQuotedContent()        { return quotedContent; }
    public void   setQuotedContent(String v){ quotedContent = v; }
}
