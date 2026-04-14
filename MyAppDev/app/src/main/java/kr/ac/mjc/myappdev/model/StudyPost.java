package kr.ac.mjc.myappdev.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class StudyPost {
    private String postId;
    private String authorUid;
    private String authorNickname;
    private String title;
    private String description;
    private String field;         // 분야 (예: 코딩, 취업, 영어, 자격증 등)
    private String location;      // 지역 (예: 서울, 경기, 온라인 등)
    private int maxMembers;
    private int currentMembers;
    private boolean recruiting;   // 모집 완료 여부
    private List<String> memberUids;    // 참여자 UID 목록
    private String chatRoomId;    // 연결된 그룹 채팅방 ID
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public StudyPost() {
        memberUids = new ArrayList<>();
        recruiting = true;
        currentMembers = 1;
    }

    public StudyPost(String authorUid, String authorNickname,
                     String title, String description,
                     String field, String location, int maxMembers) {
        this.authorUid = authorUid;
        this.authorNickname = authorNickname;
        this.title = title;
        this.description = description;
        this.field = field;
        this.location = location;
        this.maxMembers = maxMembers;
        this.currentMembers = 1;
        this.recruiting = true;
        this.memberUids = new ArrayList<>();
        this.memberUids.add(authorUid);
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getAuthorUid() { return authorUid; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; }

    public String getAuthorNickname() { return authorNickname; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }

    public int getCurrentMembers() { return currentMembers; }
    public void setCurrentMembers(int currentMembers) { this.currentMembers = currentMembers; }

    public boolean isRecruiting() { return recruiting; }
    public void setRecruiting(boolean recruiting) { this.recruiting = recruiting; }

    public List<String> getMemberUids() {
        if (memberUids == null) {
            memberUids = new ArrayList<>();
        }
        return memberUids;
    }
    public void setMemberUids(List<String> memberUids) { this.memberUids = memberUids; }

    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
