package kr.ac.mjc.myappdev.board;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.chat.ChatRoomActivity;
import kr.ac.mjc.myappdev.databinding.ActivityStudyDetailBinding;
import kr.ac.mjc.myappdev.model.ChatRoom;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class StudyDetailActivity extends AppCompatActivity {

    private ActivityStudyDetailBinding binding;
    private StudyPost currentPost;
    private String postId;
    private boolean joinInFlight;
    private StudyMemberAdapter memberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postId = getIntent().getStringExtra("postId");
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupMemberList();
        loadPost();
        binding.btnJoin.setOnClickListener(v -> joinStudy());
    }

    private void setupMemberList() {
        memberAdapter = new StudyMemberAdapter(member -> confirmKickMember(member));
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMembers.setNestedScrollingEnabled(false);
        binding.rvMembers.setAdapter(memberAdapter);
    }

    private void loadPost() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseUtil.getStudyPostsRef().document(postId).get()
                .addOnSuccessListener(doc -> {
                    currentPost = doc.toObject(StudyPost.class);
                    if (currentPost == null) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "게시글을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentPost.setPostId(doc.getId());
                    renderPost();
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "게시글을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void renderPost() {
        binding.tvTitle.setText(currentPost.getTitle());
        binding.tvAuthor.setText(currentPost.getAuthorNickname() + " 님이 운영 중");
        binding.tvField.setText(currentPost.getField());
        binding.tvLocation.setText(currentPost.getLocation());
        binding.tvMembers.setText("현재 " + currentPost.getCurrentMembers()
                + " / " + currentPost.getMaxMembers() + "명");
        binding.tvDescription.setText(currentPost.getDescription());
        int progress = currentPost.getMaxMembers() == 0
                ? 0
                : Math.min(100, (currentPost.getCurrentMembers() * 100) / currentPost.getMaxMembers());
        binding.progressMembers.setProgress(progress);

        String myUid = FirebaseUtil.getCurrentUid();
        boolean isMember = currentPost.getMemberUids().contains(myUid);
        boolean isAuthor = myUid != null && myUid.equals(currentPost.getAuthorUid());
        boolean full = currentPost.getCurrentMembers() >= currentPost.getMaxMembers();

        if (currentPost.isRecruiting()) {
            binding.tvStatus.setText("모집 중");
            binding.tvStatus.setTextColor(getColor(R.color.recruiting));
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_recruiting);
            binding.progressMembers.setIndicatorColor(getColor(R.color.secondary));
        } else {
            binding.tvStatus.setText("모집 완료");
            binding.tvStatus.setTextColor(getColor(R.color.done));
            binding.tvStatus.setBackgroundResource(R.drawable.bg_status_done);
            binding.progressMembers.setIndicatorColor(getColor(R.color.done));
        }
        binding.progressMembers.setTrackColor(getColor(R.color.primary_light));

        if (isAuthor) {
            binding.btnJoin.setText("내 스터디 관리");
            binding.btnJoin.setEnabled(true);
            binding.btnJoin.setOnClickListener(v -> openChatRoom());
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary_dark)));
            binding.tvActionHint.setText("우측 상단 메뉴에서 모집 상태와 스터디 정보를 관리할 수 있어요.");
        } else if (isMember) {
            binding.btnJoin.setText("채팅방 입장");
            binding.btnJoin.setEnabled(true);
            binding.btnJoin.setOnClickListener(v -> openChatRoom());
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.secondary_dark)));
            binding.tvActionHint.setText("참여 중인 스터디 공지와 대화를 확인해보세요.");
        } else if (!currentPost.isRecruiting() || full) {
            binding.btnJoin.setText("모집 마감");
            binding.btnJoin.setEnabled(false);
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.done)));
            binding.tvActionHint.setText("운영자가 모집을 다시 열면 참여할 수 있어요.");
        } else {
            binding.btnJoin.setText("참여 신청");
            binding.btnJoin.setEnabled(true);
            binding.btnJoin.setOnClickListener(v -> joinStudy());
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary)));
            binding.tvActionHint.setText("참여 후 바로 스터디 채팅방에 합류할 수 있어요.");
        }

        loadMemberProfiles(currentPost.getMemberUids(), currentPost.getAuthorUid(), isAuthor);
        invalidateOptionsMenu();
    }

    private void loadMemberProfiles(List<String> memberUids, String authorUid, boolean canKickMembers) {
        LinkedHashSet<String> uniqueUids = new LinkedHashSet<>(memberUids);
        if (uniqueUids.isEmpty()) {
            memberAdapter.submitList(new ArrayList<>(), canKickMembers);
            binding.tvMembersEmpty.setVisibility(View.VISIBLE);
            return;
        }

        List<Task<com.google.firebase.firestore.DocumentSnapshot>> tasks = new ArrayList<>();
        for (String uid : uniqueUids) {
            if (uid == null || uid.trim().isEmpty()) {
                continue;
            }
            tasks.add(FirebaseUtil.getUsersRef().document(uid).get());
        }

        Tasks.whenAllComplete(tasks).addOnSuccessListener(results -> {
            Map<String, String> nicknames = new HashMap<>();
            for (Task<?> task : results) {
                if (!task.isSuccessful() || task.getResult() == null) {
                    continue;
                }
                var userDoc = (com.google.firebase.firestore.DocumentSnapshot) task.getResult();
                String uid = userDoc.getId();
                String nickname = userDoc.getString("nickname");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    nicknames.put(uid, nickname.trim());
                }
            }

            List<StudyMemberAdapter.MemberItem> items = new ArrayList<>();
            for (String uid : uniqueUids) {
                if (uid == null || uid.trim().isEmpty()) {
                    continue;
                }
                String nickname = nicknames.getOrDefault(uid, "이름 미설정");
                items.add(new StudyMemberAdapter.MemberItem(uid, nickname, uid.equals(authorUid)));
            }
            memberAdapter.submitList(items, canKickMembers);
            binding.tvMembersEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void joinStudy() {
        if (joinInFlight || currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();
        if (myUid == null || myUid.trim().isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }
        String myEmail = FirebaseUtil.getCurrentUser() != null && FirebaseUtil.getCurrentUser().getEmail() != null
                ? FirebaseUtil.getCurrentUser().getEmail()
                : "";

        // 이미 멤버인 경우 채팅방으로
        if (currentPost.getMemberUids().contains(myUid)) {
            openChatRoom();
            return;
        }

        setJoinLoading(true);

        FirebaseFirestore db = FirebaseUtil.getFirestore();
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            var postRef = FirebaseUtil.getStudyPostsRef().document(postId);
            var userRef = FirebaseUtil.getUsersRef().document(myUid);
            var userSnapshot = transaction.get(userRef);
            var postSnapshot = transaction.get(postRef);
            StudyPost freshPost = postSnapshot.toObject(StudyPost.class);
            if (freshPost == null) {
                throw new IllegalStateException("스터디가 존재하지 않습니다.");
            }

            List<String> memberUids = new ArrayList<>(freshPost.getMemberUids());
            if (memberUids.contains(myUid)) {
                return null;
            }
            if (!freshPost.isRecruiting() || freshPost.getCurrentMembers() >= freshPost.getMaxMembers()) {
                throw new IllegalStateException("모집이 마감되었습니다.");
            }

            memberUids.add(myUid);
            int nextCount = freshPost.getCurrentMembers() + 1;
            boolean nextRecruiting = nextCount < freshPost.getMaxMembers();

            String chatRoomId = freshPost.getChatRoomId();
            List<String> nextRoomMembers = null;
            if (chatRoomId != null && !chatRoomId.isEmpty()) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                var roomSnapshot = transaction.get(roomRef);
                if (roomSnapshot.exists()) {
                    ChatRoom room = roomSnapshot.toObject(ChatRoom.class);
                    List<String> roomMembers = room != null
                            ? new ArrayList<>(room.getMemberUids())
                            : new ArrayList<>();
                    if (!roomMembers.contains(myUid)) {
                        roomMembers.add(myUid);
                        nextRoomMembers = roomMembers;
                    }
                }
            }

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("memberUids", memberUids);
            postUpdates.put("currentMembers", nextCount);
            postUpdates.put("recruiting", nextRecruiting);
            postUpdates.put("updatedAt", Timestamp.now());
            transaction.update(postRef, postUpdates);
            if (userSnapshot.exists()) {
                transaction.update(userRef, "joinedStudyIds", FieldValue.arrayUnion(postId));
            } else {
                Map<String, Object> userSeed = new HashMap<>();
                userSeed.put("uid", myUid);
                userSeed.put("email", myEmail);
                userSeed.put("nickname", "");
                userSeed.put("profileImageUrl", "");
                userSeed.put("joinedStudyIds", FieldValue.arrayUnion(postId));
                userSeed.put("createdAt", Timestamp.now());
                transaction.set(userRef, userSeed, SetOptions.merge());
            }
            if (chatRoomId != null && !chatRoomId.isEmpty() && nextRoomMembers != null) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                transaction.update(roomRef, "memberUids", nextRoomMembers);
            }
            return null;
        }).addOnSuccessListener(v -> {
            Toast.makeText(this, "참여 완료!", Toast.LENGTH_SHORT).show();
            loadPost();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "참여 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> setJoinLoading(false));
    }

    private void openChatRoom() {
        if (currentPost == null) {
            return;
        }
        if (currentPost.getChatRoomId() == null || currentPost.getChatRoomId().isEmpty()) {
            createChatRoom();
        } else {
            goToChatRoom(currentPost.getChatRoomId());
        }
    }

    private void createChatRoom() {
        if (currentPost == null) {
            return;
        }

        binding.btnJoin.setEnabled(false);

        String roomId = postId;
        var roomRef = FirebaseUtil.getChatRoomsRef().document(roomId);
        var postRef = FirebaseUtil.getStudyPostsRef().document(postId);

        FirebaseUtil.getFirestore().runTransaction((Transaction.Function<String>) transaction -> {
            var postSnapshot = transaction.get(postRef);
            StudyPost freshPost = postSnapshot.toObject(StudyPost.class);
            if (freshPost == null) {
                throw new IllegalStateException("스터디가 존재하지 않습니다.");
            }

            String existingRoomId = freshPost.getChatRoomId();
            if (existingRoomId != null && !existingRoomId.isEmpty()) {
                return existingRoomId;
            }

            if (!transaction.get(roomRef).exists()) {
                ChatRoom room = new ChatRoom(
                        freshPost.getTitle(),
                        postId,
                        new ArrayList<>(freshPost.getMemberUids()));
                transaction.set(roomRef, room);
            }

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("chatRoomId", roomId);
            postUpdates.put("updatedAt", Timestamp.now());
            transaction.update(postRef, postUpdates);
            return roomId;
        }).addOnSuccessListener(this::goToChatRoom)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "채팅방을 열지 못했습니다", Toast.LENGTH_SHORT).show()
                )
                .addOnCompleteListener(task -> binding.btnJoin.setEnabled(true));
    }

    private void confirmLeaveStudy() {
        new AlertDialog.Builder(this)
                .setTitle("스터디 탈퇴")
                .setMessage("스터디에서 탈퇴하시겠습니까?")
                .setPositiveButton("탈퇴", (dialog, w) -> leaveStudy())
                .setNegativeButton("취소", null)
                .show();
    }

    private void leaveStudy() {
        if (joinInFlight || currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();
        if (myUid == null || myUid.equals(currentPost.getAuthorUid())) {
            Toast.makeText(this, "스터디장은 탈퇴할 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        setJoinLoading(true);
        FirebaseUtil.getFirestore().runTransaction((Transaction.Function<Void>) transaction -> {
            var postRef = FirebaseUtil.getStudyPostsRef().document(postId);
            var userRef = FirebaseUtil.getUsersRef().document(myUid);
            var postSnapshot = transaction.get(postRef);
            StudyPost freshPost = postSnapshot.toObject(StudyPost.class);
            if (freshPost == null) {
                throw new IllegalStateException("스터디가 존재하지 않습니다.");
            }
            if (myUid.equals(freshPost.getAuthorUid())) {
                throw new IllegalStateException("스터디장은 탈퇴할 수 없습니다.");
            }

            List<String> memberUids = new ArrayList<>(freshPost.getMemberUids());
            if (!memberUids.remove(myUid)) {
                return null;
            }

            int nextCount = memberUids.size();
            String chatRoomId = freshPost.getChatRoomId();
            List<String> nextRoomMembers = null;
            if (chatRoomId != null && !chatRoomId.isEmpty()) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                var roomSnapshot = transaction.get(roomRef);
                if (roomSnapshot.exists()) {
                    ChatRoom room = roomSnapshot.toObject(ChatRoom.class);
                    List<String> roomMembers = room != null
                            ? new ArrayList<>(room.getMemberUids())
                            : new ArrayList<>();
                    if (roomMembers.remove(myUid)) {
                        nextRoomMembers = roomMembers;
                    }
                }
            }

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("memberUids", memberUids);
            postUpdates.put("currentMembers", nextCount);
            postUpdates.put("recruiting", nextCount < freshPost.getMaxMembers());
            postUpdates.put("updatedAt", Timestamp.now());
            transaction.update(postRef, postUpdates);
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("joinedStudyIds", FieldValue.arrayRemove(postId));
            transaction.set(userRef, userUpdates, SetOptions.merge());
            if (chatRoomId != null && !chatRoomId.isEmpty() && nextRoomMembers != null) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                transaction.update(roomRef, "memberUids", nextRoomMembers);
            }
            return null;
        }).addOnSuccessListener(v -> {
            Toast.makeText(this, "스터디에서 탈퇴했습니다", Toast.LENGTH_SHORT).show();
            loadPost();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "탈퇴 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> setJoinLoading(false));
    }

    private void confirmKickMember(StudyMemberAdapter.MemberItem memberItem) {
        if (currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();
        if (myUid == null || !myUid.equals(currentPost.getAuthorUid())) {
            Toast.makeText(this, "강퇴 권한이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("참여자 강퇴")
                .setMessage(memberItem.getNickname() + " 님을 강퇴하시겠습니까?")
                .setPositiveButton("강퇴", (dialog, w) -> kickMember(memberItem.getUid()))
                .setNegativeButton("취소", null)
                .show();
    }

    private void kickMember(String targetUid) {
        if (joinInFlight || currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();
        if (myUid == null) {
            return;
        }
        setJoinLoading(true);

        FirebaseUtil.getFirestore().runTransaction((Transaction.Function<Void>) transaction -> {
            var postRef = FirebaseUtil.getStudyPostsRef().document(postId);
            var postSnapshot = transaction.get(postRef);
            StudyPost freshPost = postSnapshot.toObject(StudyPost.class);
            if (freshPost == null) {
                throw new IllegalStateException("스터디가 존재하지 않습니다.");
            }
            if (!myUid.equals(freshPost.getAuthorUid())) {
                throw new IllegalStateException("강퇴 권한이 없습니다.");
            }
            if (targetUid.equals(freshPost.getAuthorUid())) {
                throw new IllegalStateException("스터디장은 강퇴할 수 없습니다.");
            }

            List<String> memberUids = new ArrayList<>(freshPost.getMemberUids());
            if (!memberUids.remove(targetUid)) {
                return null;
            }

            int nextCount = memberUids.size();
            String chatRoomId = freshPost.getChatRoomId();
            List<String> nextRoomMembers = null;
            if (chatRoomId != null && !chatRoomId.isEmpty()) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                var roomSnapshot = transaction.get(roomRef);
                if (roomSnapshot.exists()) {
                    ChatRoom room = roomSnapshot.toObject(ChatRoom.class);
                    List<String> roomMembers = room != null
                            ? new ArrayList<>(room.getMemberUids())
                            : new ArrayList<>();
                    if (roomMembers.remove(targetUid)) {
                        nextRoomMembers = roomMembers;
                    }
                }
            }

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("memberUids", memberUids);
            postUpdates.put("currentMembers", nextCount);
            postUpdates.put("recruiting", nextCount < freshPost.getMaxMembers());
            postUpdates.put("updatedAt", Timestamp.now());
            transaction.update(postRef, postUpdates);
            if (chatRoomId != null && !chatRoomId.isEmpty() && nextRoomMembers != null) {
                var roomRef = FirebaseUtil.getChatRoomsRef().document(chatRoomId);
                transaction.update(roomRef, "memberUids", nextRoomMembers);
            }
            return null;
        }).addOnSuccessListener(v -> {
            Toast.makeText(this, "참여자를 강퇴했습니다", Toast.LENGTH_SHORT).show();
            loadPost();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "강퇴 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> setJoinLoading(false));
    }

    private void confirmToggleRecruiting() {
        if (currentPost == null) {
            return;
        }
        String title = currentPost.isRecruiting() ? "스터디 종료" : "모집 재개";
        String message = currentPost.isRecruiting()
                ? "지금 스터디 모집을 종료하시겠습니까?"
                : "스터디 모집을 다시 시작하시겠습니까?";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", (dialog, w) -> toggleRecruiting())
                .setNegativeButton("취소", null)
                .show();
    }

    private void toggleRecruiting() {
        if (joinInFlight || currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();
        if (myUid == null || !myUid.equals(currentPost.getAuthorUid())) {
            Toast.makeText(this, "수정 권한이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        setJoinLoading(true);
        FirebaseUtil.getFirestore().runTransaction((Transaction.Function<Void>) transaction -> {
            var postRef = FirebaseUtil.getStudyPostsRef().document(postId);
            var snapshot = transaction.get(postRef);
            StudyPost freshPost = snapshot.toObject(StudyPost.class);
            if (freshPost == null) {
                throw new IllegalStateException("스터디가 존재하지 않습니다.");
            }
            if (!myUid.equals(freshPost.getAuthorUid())) {
                throw new IllegalStateException("수정 권한이 없습니다.");
            }

            boolean nextRecruiting;
            if (freshPost.isRecruiting()) {
                nextRecruiting = false;
            } else {
                if (freshPost.getCurrentMembers() >= freshPost.getMaxMembers()) {
                    throw new IllegalStateException("정원이 가득 차 모집을 재개할 수 없습니다.");
                }
                nextRecruiting = true;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("recruiting", nextRecruiting);
            updates.put("updatedAt", Timestamp.now());
            transaction.update(postRef, updates);
            return null;
        }).addOnSuccessListener(v -> {
            Toast.makeText(this, "모집 상태를 변경했습니다", Toast.LENGTH_SHORT).show();
            loadPost();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "상태 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        ).addOnCompleteListener(task -> setJoinLoading(false));
    }

    private void goToChatRoom(String roomId) {
        // 보안: 채팅방 멤버인지 서버에서 확인 후 입장 (ChatRoomActivity에서 재확인)
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomName", currentPost.getTitle());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu == null) {
            return super.onPrepareOptionsMenu(menu);
        }

        String myUid = FirebaseUtil.getCurrentUid();
        boolean hasPost = currentPost != null;
        boolean isAuthor = hasPost && myUid != null && myUid.equals(currentPost.getAuthorUid());
        boolean isMember = hasPost && myUid != null && currentPost.getMemberUids().contains(myUid);

        MenuItem editItem = menu.findItem(R.id.action_edit);
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        MenuItem leaveItem = menu.findItem(R.id.action_leave);
        MenuItem toggleRecruitingItem = menu.findItem(R.id.action_toggle_recruiting);

        if (editItem != null) editItem.setVisible(isAuthor);
        if (deleteItem != null) deleteItem.setVisible(isAuthor);
        if (leaveItem != null) leaveItem.setVisible(isMember && !isAuthor);
        if (toggleRecruitingItem != null) {
            toggleRecruitingItem.setVisible(isAuthor);
            if (isAuthor) {
                toggleRecruitingItem.setTitle(currentPost.isRecruiting() ? "스터디 종료" : "모집 재개");
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_edit) {
            Intent intent = new Intent(this, CreateStudyActivity.class);
            intent.putExtra("postId", postId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_leave) {
            confirmLeaveStudy();
            return true;
        } else if (id == R.id.action_toggle_recruiting) {
            confirmToggleRecruiting();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("게시글 삭제")
                .setMessage("삭제하면 복구할 수 없습니다. 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, w) -> deletePost())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost() {
        // 보안: 본인 게시글만 삭제 — authorUid와 현재 UID 비교
        if (currentPost == null || !FirebaseUtil.getCurrentUid().equals(currentPost.getAuthorUid())) {
            Toast.makeText(this, "삭제 권한이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        setJoinLoading(true);
        binding.progressBar.setVisibility(View.VISIBLE);

        String chatRoomId = currentPost.getChatRoomId();
        if (chatRoomId == null || chatRoomId.isEmpty()) {
            executeCascadeDelete(null, null);
            return;
        }
        FirebaseUtil.getMessagesRef(chatRoomId).get()
                .addOnSuccessListener(messageSnapshots ->
                        executeCascadeDelete(messageSnapshots, chatRoomId))
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    executeCascadeDelete(null, chatRoomId);
                });
    }

    private void executeCascadeDelete(@Nullable QuerySnapshot messageSnapshots,
                                      @Nullable String chatRoomId) {
        FirebaseFirestore db = FirebaseUtil.getFirestore();
        List<Task<Void>> commitTasks = new ArrayList<>();
        WriteBatch batch = db.batch();
        int opCount = 0;

        if (messageSnapshots != null) {
            for (var messageDoc : messageSnapshots) {
                batch.delete(messageDoc.getReference());
                opCount++;
                if (opCount >= 450) {
                    commitTasks.add(batch.commit());
                    batch = db.batch();
                    opCount = 0;
                }
            }
        }

        if (chatRoomId != null && !chatRoomId.isEmpty()) {
            batch.delete(FirebaseUtil.getChatRoomsRef().document(chatRoomId));
            opCount++;
            if (opCount >= 450) {
                commitTasks.add(batch.commit());
                batch = db.batch();
                opCount = 0;
            }
        }

        batch.delete(FirebaseUtil.getStudyPostsRef().document(postId));
        opCount++;

        if (opCount > 0) {
            commitTasks.add(batch.commit());
        }

        Tasks.whenAllComplete(commitTasks).addOnSuccessListener(results -> {
            boolean hasFailure = false;
            for (Task<?> task : results) {
                if (!task.isSuccessful()) {
                    hasFailure = true;
                    break;
                }
            }
            binding.progressBar.setVisibility(View.GONE);
            setJoinLoading(false);
            if (hasFailure) {
                Toast.makeText(this, "일부 데이터 정리에 실패했습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setJoinLoading(boolean loading) {
        joinInFlight = loading;
        binding.btnJoin.setEnabled(!loading);
    }
}
