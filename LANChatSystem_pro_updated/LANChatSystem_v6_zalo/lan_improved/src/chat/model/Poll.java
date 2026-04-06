package chat.model;

import java.util.*;

public class Poll {
    private int           id;
    private String        question;
    private String        createdBy;
    private String        groupName;
    private boolean       closed;
    private List<PollOption> options = new ArrayList<>();

    public Poll() {}
    public Poll(String question, String createdBy, String groupName) {
        this.question   = question;
        this.createdBy  = createdBy;
        this.groupName  = groupName;
    }

    public int    getId()              { return id; }
    public void   setId(int v)         { id = v; }
    public String getQuestion()        { return question; }
    public String getCreatedBy()       { return createdBy; }
    public String getGroupName()       { return groupName; }
    public boolean isClosed()          { return closed; }
    public void   setClosed(boolean v) { closed = v; }
    public List<PollOption> getOptions(){ return options; }
    public void   addOption(PollOption o){ options.add(o); }
}
