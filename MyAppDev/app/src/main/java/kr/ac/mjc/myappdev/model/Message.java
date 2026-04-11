package kr.ac.mjc.myappdev.model;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String roomId;
    private String senderUid;
    private String senderNickname;
    private String content;
    private Timestamp createdAt;

    public Message() {}

    public Message(String roomId, String senderUid, String senderNickname, String content) {
        this.roomId = roomId;
        this.senderUid = senderUid;
        this.senderNickname = senderNickname;
        this.content = content;
        this.createdAt = Timestamp.now();
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}