package kr.ac.mjc.myappdev.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.model.ChatRoom;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ChatRoom room);
    }

    private List<ChatRoom> rooms = new ArrayList<>();
    private final OnItemClickListener listener;

    public ChatRoomAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ChatRoom> newRooms) {
        rooms = new ArrayList<>(newRooms);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rooms.get(position), listener);
    }

    @Override
    public int getItemCount() { return rooms.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvLastMessage, tvType, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvRoomName   = itemView.findViewById(R.id.tvRoomName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvType       = itemView.findViewById(R.id.tvType);
            tvTime       = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatRoom room, OnItemClickListener listener) {
            String name = room.getRoomName() != null && !room.getRoomName().isEmpty()
                    ? room.getRoomName() : "1:1 채팅";
            tvRoomName.setText(name);
            String lastMessage = room.getLastMessage();
            tvLastMessage.setText(lastMessage != null && !lastMessage.isEmpty()
                    ? lastMessage
                    : "아직 메시지가 없습니다");
            tvType.setText(ChatRoom.TYPE_GROUP.equals(room.getType()) ? "그룹" : "개인");
            tvTime.setText(formatTime(room.getLastMessageAt()));
            itemView.setOnClickListener(v -> listener.onItemClick(room));
        }

        private String formatTime(Timestamp timestamp) {
            if (timestamp == null) {
                return "";
            }

            Date date = timestamp.toDate();
            long diff = System.currentTimeMillis() - date.getTime();
            long oneDay = 24L * 60L * 60L * 1000L;

            if (diff < oneDay) {
                return new SimpleDateFormat("a h:mm", Locale.KOREA).format(date);
            }
            if (diff < oneDay * 2) {
                return "어제";
            }
            return new SimpleDateFormat("M월 d일", Locale.KOREA).format(date);
        }
    }
}
