package kr.ac.mjc.myappdev.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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
        TextView tvRoomName, tvLastMessage, tvType;

        ViewHolder(View itemView) {
            super(itemView);
            tvRoomName   = itemView.findViewById(R.id.tvRoomName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvType       = itemView.findViewById(R.id.tvType);
        }

        void bind(ChatRoom room, OnItemClickListener listener) {
            String name = room.getRoomName() != null && !room.getRoomName().isEmpty()
                    ? room.getRoomName() : "1:1 채팅";
            tvRoomName.setText(name);
            tvLastMessage.setText(room.getLastMessage() != null ? room.getLastMessage() : "");
            tvType.setText(ChatRoom.TYPE_GROUP.equals(room.getType()) ? "그룹" : "1:1");
            itemView.setOnClickListener(v -> listener.onItemClick(room));
        }
    }
}