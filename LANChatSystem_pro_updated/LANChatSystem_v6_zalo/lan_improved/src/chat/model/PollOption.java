package chat.model;

import java.util.*;

public class PollOption {
    private int         id;
    private String      text;
    private Set<String> voters = new HashSet<>();

    public PollOption() {}
    public PollOption(String text) { this.text = text; }

    public int         getId()          { return id; }
    public void        setId(int v)     { id = v; }
    public String      getText()        { return text; }
    public Set<String> getVoters()      { return voters; }
    public int         getVoteCount()   { return voters.size(); }
    public boolean     addVote(String u){ return voters.add(u); }
    public boolean     removeVote(String u){ return voters.remove(u); }
}
