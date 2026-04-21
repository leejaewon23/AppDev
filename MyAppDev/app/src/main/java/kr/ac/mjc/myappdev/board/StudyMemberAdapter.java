package kr.ac.mjc.myappdev.board;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.R;

public class StudyMemberAdapter extends RecyclerView.Adapter<StudyMemberAdapter.ViewHolder> {

    public static class MemberItem {
        private final String uid;
        private final String nickname;
        private final boolean author;

        public MemberItem(String uid, String nickname, boolean author) {
            this.uid = uid;
            this.nickname = nickname;
            this.author = author;
        }

        public String getUid() {
            return uid;
        }

        public String getNickname() {
            return nickname;
        }

        public boolean isAuthor() {
            return author;
        }
    }

    public interface OnKickClickListener {
        void onKick(MemberItem memberItem);
    }

    private final OnKickClickListener onKickClickListener;
    private final List<MemberItem> members = new ArrayList<>();
    private boolean canKickMembers;

    public StudyMemberAdapter(OnKickClickListener onKickClickListener) {
        this.onKickClickListener = onKickClickListener;
    }

    public void submitList(List<MemberItem> items, boolean canKickMembers) {
        members.clear();
        members.addAll(items);
        this.canKickMembers = canKickMembers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(members.get(position), canKickMembers, onKickClickListener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMemberName;
        private final TextView tvMemberMeta;
        private final MaterialButton btnKick;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberMeta = itemView.findViewById(R.id.tvMemberMeta);
            btnKick = itemView.findViewById(R.id.btnKick);
        }

        void bind(MemberItem memberItem, boolean canKickMembers, OnKickClickListener listener) {
            tvMemberName.setText(memberItem.getNickname());
            tvMemberMeta.setText(memberItem.isAuthor() ? "스터디장" : "참여자");

            boolean showKickButton = canKickMembers && !memberItem.isAuthor();
            btnKick.setVisibility(showKickButton ? View.VISIBLE : View.GONE);
            btnKick.setOnClickListener(showKickButton ? v -> listener.onKick(memberItem) : null);
        }
    }
}
