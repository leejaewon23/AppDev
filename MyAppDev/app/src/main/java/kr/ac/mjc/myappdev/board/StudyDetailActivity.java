package kr.ac.mjc.myappdev.board;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postId = getIntent().getStringExtra("postId");
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadPost();
        binding.btnJoin.setOnClickListener(v -> joinStudy());
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
        boolean isMember  = currentPost.getMemberUids().contains(myUid);
        boolean full      = currentPost.getCurrentMembers() >= currentPost.getMaxMembers();

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

        if (isMember) {
            // 이미 참여한 멤버: 채팅방 이동 버튼
            binding.btnJoin.setText("채팅방 입장");
            binding.btnJoin.setEnabled(true);
            binding.btnJoin.setOnClickListener(v -> openChatRoom());
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.secondary_dark)));
        } else if (!currentPost.isRecruiting() || full) {
            binding.btnJoin.setText("모집 완료");
            binding.btnJoin.setEnabled(false);
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.done)));
        } else {
            binding.btnJoin.setText("참여 신청");
            binding.btnJoin.setEnabled(true);
            binding.btnJoin.setOnClickListener(v -> joinStudy());
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        }

        // 작성자는 수정/삭제 가능 (overflow menu에서 처리)
        invalidateOptionsMenu();
    }

    private void joinStudy() {
        if (joinInFlight || currentPost == null) {
            return;
        }
        String myUid = FirebaseUtil.getCurrentUid();

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

            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("memberUids", memberUids);
            postUpdates.put("currentMembers", nextCount);
            postUpdates.put("recruiting", nextRecruiting);
            postUpdates.put("updatedAt", com.google.firebase.Timestamp.now());
            transaction.update(postRef, postUpdates);
            transaction.update(userRef, "joinedStudyIds", FieldValue.arrayUnion(postId));

            String chatRoomId = freshPost.getChatRoomId();
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
                        transaction.update(roomRef, "memberUids", roomMembers);
                    }
                }
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
            postUpdates.put("updatedAt", com.google.firebase.Timestamp.now());
            transaction.update(postRef, postUpdates);
            return roomId;
        }).addOnSuccessListener(this::goToChatRoom)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "채팅방을 열지 못했습니다", Toast.LENGTH_SHORT).show()
                )
                .addOnCompleteListener(task -> binding.btnJoin.setEnabled(true));
    }

    private void goToChatRoom(String roomId) {
        // 보안: 채팅방 멤버인지 서버에서 확인 후 입장 (ChatRoomActivity에서 재확인)
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomName", currentPost.getTitle());
        startActivity(intent);
    }

    // ── 작성자 전용: 수정/삭제 메뉴 ──────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentPost != null &&
                FirebaseUtil.getCurrentUid().equals(currentPost.getAuthorUid())) {
            getMenuInflater().inflate(R.menu.menu_post_detail, menu);
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
        if (!FirebaseUtil.getCurrentUid().equals(currentPost.getAuthorUid())) {
            Toast.makeText(this, "삭제 권한이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUtil.getStudyPostsRef().document(postId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setJoinLoading(boolean loading) {
        joinInFlight = loading;
        binding.btnJoin.setEnabled(!loading);
    }
}
