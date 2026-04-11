package kr.ac.mjc.myappdev.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private List<String> joinedStudyIds;  // 참여 중인 스터디 ID 목록
    private Timestamp createdAt;

    // Firestore 역직렬화용 기본 생성자
    public User() {
        joinedStudyIds = new ArrayList<>();
    }

    public User(String uid, String email, String nickname) {
        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = "";
        this.joinedStudyIds = new ArrayList<>();
        this.createdAt = Timestamp.now();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public List<String> getJoinedStudyIds() { return joinedStudyIds; }
    public void setJoinedStudyIds(List<String> joinedStudyIds) { this.joinedStudyIds = joinedStudyIds; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}