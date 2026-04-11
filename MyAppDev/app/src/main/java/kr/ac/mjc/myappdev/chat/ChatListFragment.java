package kr.ac.mjc.myappdev.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.databinding.FragmentChatListBinding;
import kr.ac.mjc.myappdev.model.ChatRoom;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class ChatListFragment extends Fragment {

    private FragmentChatListBinding binding;
    private ChatRoomAdapter adapter;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ChatRoomAdapter(room -> {
            // 보안: 채팅방 멤버인지 확인 (memberUids 포함 여부 — ChatRoomActivity에서 재검증)
            String myUid = FirebaseUtil.getCurrentUid();
            if (room.getMemberUids() == null || !room.getMemberUids().contains(myUid)) return;

            Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
            intent.putExtra("roomId", room.getRoomId());
            intent.putExtra("roomName", room.getRoomName() != null ? room.getRoomName() : "채팅");
            startActivity(intent);
        });

        binding.rvChatRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChatRooms.setAdapter(adapter);

        subscribeChatRooms();
    }

    private void subscribeChatRooms() {
        String myUid = FirebaseUtil.getCurrentUid();

        // 실시간 구독: 내가 참여한 채팅방 목록 (최신순)
        listenerRegistration = FirebaseUtil.getChatRoomsRef()
                .whereArrayContains("memberUids", myUid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<ChatRoom> rooms = new ArrayList<>();
                    for (var doc : snapshots) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        room.setRoomId(doc.getId());
                        rooms.add(room);
                    }
                    adapter.submitList(rooms);
                    binding.tvEmpty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) listenerRegistration.remove();
        binding = null;
    }
}