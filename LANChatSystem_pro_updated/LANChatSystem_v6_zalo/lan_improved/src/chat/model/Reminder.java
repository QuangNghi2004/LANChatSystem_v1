package chat.model;

import java.time.LocalDateTime;

public class Reminder {
    private int           id;
    private String        username;
    private String        content;
    private LocalDateTime triggerTime;
    private boolean       fired;

    public Reminder() {}
    public Reminder(String username, String content, LocalDateTime triggerTime) {
        this.username = username; this.content = content; this.triggerTime = triggerTime;
    }

    public int    getId()                     { return id; }
    public void   setId(int v)                { id = v; }
    public String getUsername()               { return username; }
    public String getContent()                { return content; }
    public LocalDateTime getTriggerTime()     { return triggerTime; }
    public boolean isFired()                  { return fired; }
    public void   setFired(boolean v)         { fired = v; }
}
