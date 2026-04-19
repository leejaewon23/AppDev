package kr.ac.mjc.myappdev.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Firebase 인스턴스 및 Firestore 컬렉션 참조를 중앙 관리
 */
public class FirebaseUtil {

    // Firestore 컬렉션 이름 상수
    public static final String COLLECTION_USERS      = "users";
    public static final String COLLECTION_POSTS      = "studyPosts";
    public static final String COLLECTION_SCHEDULES  = "schedules";
    public static final String COLLECTION_CHATROOMS  = "chatRooms";
    public static final String COLLECTION_MESSAGES   = "messages";

    // Storage 경로 상수
    public static final String STORAGE_PROFILES = "profiles/";

    private FirebaseUtil() {}

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public static String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static CollectionReference getUsersRef() {
        return getFirestore().collection(COLLECTION_USERS);
    }

    public static CollectionReference getStudyPostsRef() {
        return getFirestore().collection(COLLECTION_POSTS);
    }

    public static CollectionReference getChatRoomsRef() {
        return getFirestore().collection(COLLECTION_CHATROOMS);
    }

    public static CollectionReference getStudySchedulesRef(String postId) {
        return getStudyPostsRef()
                .document(postId)
                .collection(COLLECTION_SCHEDULES);
    }

    public static CollectionReference getMessagesRef(String roomId) {
        return getFirestore()
                .collection(COLLECTION_CHATROOMS)
                .document(roomId)
                .collection(COLLECTION_MESSAGES);
    }

    public static StorageReference getProfileStorageRef(String uid) {
        return FirebaseStorage.getInstance()
                .getReference()
                .child(STORAGE_PROFILES + uid + ".jpg");
    }
}
