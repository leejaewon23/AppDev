package kr.ac.mjc.myappdev.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import kr.ac.mjc.myappdev.databinding.ActivityChatRoomBinding;
import kr.ac.mjc.myappdev.model.Message;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class ChatRoomActivity extends AppCompatActivity {

    private ActivityChatRoomBinding binding;
    private MessageAdapter adapter;
    private ListenerRegistration messageListener;

    private String roomId;
    private String myNickname = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        roomId = getIntent().getStringExtra("roomId");
        String roomName = getIntent().getStringExtra("roomName");

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(roomName != null ? roomName : "채팅");
        }

        // 보안: 채팅방 입장 전 참여자 확인
        verifyMembershipThenLoad();
    }

    /**
     * Firestore에서 채팅방 문서를 읽어 현재 유저가 memberUids에 포함되는지 확인.
     * 미포함 시 화면을 닫아 비인가 접근 차단.
     */
    private void verifyMembershipThenLoad() {
        String myUid = FirebaseUtil.getCurrentUid();
        FirebaseUtil.getChatRoomsRef().document(roomId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }
                    java.util.List<String> members = (java.util.List<String>) doc.get("memberUids");
                    if (members == null || !members.contains(myUid)) {
                        Toast.makeText(this, "채팅방 참여 권한이 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    loadNicknameThenSetup();
                })
                .addOnFailureListener(e -> finish());
    }

    private void loadNicknameThenSetup() {
        FirebaseUtil.getUsersRef()
                .document(FirebaseUtil.getCurrentUid()).get()
                .addOnSuccessListener(doc -> {
                    myNickname = doc.getString("nickname");
                    if (myNickname == null) myNickname = "익명";
                    setupRecyclerView();
                    subscribeMessages();
                    binding.btnSend.setOnClickListener(v -> sendMessage());
                });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(lm);
        binding.rvMessages.setAdapter(adapter);
    }

    private void subscribeMessages() {
        // 실시간 메시지 구독 (시간순)
        messageListener = FirebaseUtil.getMessagesRef(roomId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    java.util.List<Message> msgs = new java.util.ArrayList<>();
                    for (var doc : snapshots) {
                        Message msg = doc.toObject(Message.class);
                        msg.setMessageId(doc.getId());
                        msgs.add(msg);
                    }
                    adapter.submitList(msgs);
                    binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                });
    }

    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        binding.etMessage.setText("");

        Message msg = new Message(roomId, FirebaseUtil.getCurrentUid(), myNickname, content);

        // 서브컬렉션에 메시지 저장
        FirebaseUtil.getMessagesRef(roomId).add(msg);

        // 채팅방 마지막 메시지 업데이트
        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("lastMessage", content);
        roomUpdate.put("lastMessageAt", Timestamp.now());
        FirebaseUtil.getChatRoomsRef().document(roomId).update(roomUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}