package chat.model;

public class Reaction {
    private int    messageId;
    private String username;
    private String emoji;

    public Reaction() {}
    public Reaction(int messageId, String username, String emoji) {
        this.messageId = messageId; this.username = username; this.emoji = emoji;
    }

    public int    getMessageId()          { return messageId; }
    public String getUsername()           { return username; }
    public String getEmoji()              { return emoji; }
}
