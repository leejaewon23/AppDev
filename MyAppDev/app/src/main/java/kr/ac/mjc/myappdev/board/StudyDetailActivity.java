package kr.ac.mjc.myappdev.board;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
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
                    if (currentPost == null) return;
                    currentPost.setPostId(doc.getId());
                    renderPost();
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void renderPost() {
        binding.tvTitle.setText(currentPost.getTitle());
        binding.tvAuthor.setText("작성자: " + currentPost.getAuthorNickname());
        binding.tvField.setText("분야: " + currentPost.getField());
        binding.tvLocation.setText("지역: " + currentPost.getLocation());
        binding.tvMembers.setText("인원: " + currentPost.getCurrentMembers()
                + "/" + currentPost.getMaxMembers() + "명");
        binding.tvDescription.setText(currentPost.getDescription());

        String myUid = FirebaseUtil.getCurrentUid();
        boolean isMember  = currentPost.getMemberUids().contains(myUid);
        boolean isAuthor  = myUid.equals(currentPost.getAuthorUid());
        boolean full      = currentPost.getCurrentMembers() >= currentPost.getMaxMembers();

        // 모집 상태 표시
        binding.tvStatus.setText(currentPost.isRecruiting() ? "모집 중" : "모집 완료");

        if (isMember) {
            // 이미 참여한 멤버: 채팅방 이동 버튼
            binding.btnJoin.setText("채팅방 입장");
            binding.btnJoin.setOnClickListener(v -> openChatRoom());
        } else if (!currentPost.isRecruiting() || full) {
            binding.btnJoin.setText("모집 완료");
            binding.btnJoin.setEnabled(false);
        } else {
            binding.btnJoin.setText("참여 신청");
        }

        // 작성자는 수정/삭제 가능 (overflow menu에서 처리)
        invalidateOptionsMenu();
    }

    private void joinStudy() {
        String myUid = FirebaseUtil.getCurrentUid();

        // 이미 멤버인 경우 채팅방으로
        if (currentPost.getMemberUids().contains(myUid)) {
            openChatRoom();
            return;
        }

        // Firestore 트랜잭션으로 atomically 업데이트
        Map<String, Object> updates = new HashMap<>();
        updates.put("memberUids", FieldValue.arrayUnion(myUid));
        updates.put("currentMembers", currentPost.getCurrentMembers() + 1);

        boolean willBeFull = (currentPost.getCurrentMembers() + 1) >= currentPost.getMaxMembers();
        if (willBeFull) updates.put("recruiting", false);

        FirebaseUtil.getStudyPostsRef().document(postId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    // 사용자 joinedStudyIds에도 추가
                    FirebaseUtil.getUsersRef().document(myUid)
                            .update("joinedStudyIds", FieldValue.arrayUnion(postId));
                    Toast.makeText(this, "참여 완료!", Toast.LENGTH_SHORT).show();
                    loadPost();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "참여 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openChatRoom() {
        if (currentPost.getChatRoomId() == null || currentPost.getChatRoomId().isEmpty()) {
            createChatRoom();
        } else {
            goToChatRoom(currentPost.getChatRoomId());
        }
    }

    private void createChatRoom() {
        ChatRoom room = new ChatRoom(
                currentPost.getTitle(),
                postId,
                currentPost.getMemberUids());

        FirebaseUtil.getChatRoomsRef().add(room)
                .addOnSuccessListener(ref -> {
                    // 스터디 게시글에 chatRoomId 저장
                    FirebaseUtil.getStudyPostsRef().document(postId)
                            .update("chatRoomId", ref.getId());
                    goToChatRoom(ref.getId());
                });
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
}
