package kr.ac.mjc.myappdev.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    // 채팅방 유형
    public static final String TYPE_DIRECT = "DIRECT";   // 1:1 채팅
    public static final String TYPE_GROUP  = "GROUP";    // 그룹 채팅 (스터디)

    private String roomId;
    private String type;
    private List<String> memberUids;      // 참여자 UID 목록
    private String studyPostId;           // 그룹 채팅인 경우 연결된 스터디 ID
    private String roomName;              // 그룹 채팅방 이름 (스터디 제목)
    private String lastMessage;
    private Timestamp lastMessageAt;

    public ChatRoom() {
        memberUids = new ArrayList<>();
    }

    // 그룹 채팅방 생성용
    public ChatRoom(String roomName, String studyPostId, List<String> memberUids) {
        this.type = TYPE_GROUP;
        this.roomName = roomName;
        this.studyPostId = studyPostId;
        this.memberUids = memberUids;
        this.lastMessage = "";
        this.lastMessageAt = Timestamp.now();
    }

    // 1:1 채팅방 생성용
    public ChatRoom(String uid1, String uid2) {
        this.type = TYPE_DIRECT;
        this.memberUids = new ArrayList<>();
        this.memberUids.add(uid1);
        this.memberUids.add(uid2);
        this.lastMessage = "";
        this.lastMessageAt = Timestamp.now();
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getMemberUids() {
        if (memberUids == null) {
            memberUids = new ArrayList<>();
        }
        return memberUids;
    }
    public void setMemberUids(List<String> memberUids) { this.memberUids = memberUids; }

    public String getStudyPostId() { return studyPostId; }
    public void setStudyPostId(String studyPostId) { this.studyPostId = studyPostId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
