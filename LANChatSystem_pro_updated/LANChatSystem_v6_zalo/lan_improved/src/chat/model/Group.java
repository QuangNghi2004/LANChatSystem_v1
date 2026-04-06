package chat.model;

import java.util.*;

public class Group {
    private int         id;
    private String      name;
    private String      leader;
    private Set<String> members = new HashSet<>();
    private Map<String, String> roles = new HashMap<>(); // username→"leader"|"member"
    private String      description;

    public Group() {}
    public Group(String name, String leader) {
        this.name = name; this.leader = leader;
        members.add(leader); roles.put(leader, "leader");
    }

    public int         getId()               { return id; }
    public void        setId(int v)          { id = v; }
    public String      getName()             { return name; }
    public void        setName(String v)     { name = v; }
    public String      getLeader()           { return leader; }
    public Set<String> getMembers()          { return members; }
    public boolean     hasMember(String u)   { return members.contains(u); }
    public void        addMember(String u)   { members.add(u); roles.putIfAbsent(u,"member"); }
    public void        removeMember(String u){ members.remove(u); roles.remove(u); }
    public String      getRole(String u)     { return roles.getOrDefault(u,"member"); }
    public String      getDescription()      { return description; }
    public void        setDescription(String v){ description = v; }
}
