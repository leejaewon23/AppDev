package kr.ac.mjc.myappdev.board;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.model.StudyPost;

public class StudyAdapter extends RecyclerView.Adapter<StudyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(StudyPost post);
    }

    private List<StudyPost> posts = new ArrayList<>();
    private final OnItemClickListener listener;

    public StudyAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<StudyPost> newPosts) {
        posts = new ArrayList<>(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(posts.get(position), listener);
    }

    @Override
    public int getItemCount() { return posts.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvMembers, tvField, tvLocation, tvSummary;
        Chip chipStatus;
        LinearProgressIndicator progressMembers;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvTitle);
            tvAuthor   = itemView.findViewById(R.id.tvAuthor);
            tvMembers  = itemView.findViewById(R.id.tvMembers);
            tvField    = itemView.findViewById(R.id.tvField);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvSummary  = itemView.findViewById(R.id.tvSummary);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            progressMembers = itemView.findViewById(R.id.progressMembers);
        }

        void bind(StudyPost post, OnItemClickListener listener) {
            tvTitle.setText(post.getTitle());
            tvAuthor.setText("운영: " + post.getAuthorNickname());
            tvMembers.setText("참여 인원 " + post.getCurrentMembers() + "/" + post.getMaxMembers() + "명");
            tvField.setText(post.getField());
            tvLocation.setText(post.getLocation());
            tvSummary.setText(post.getDescription());
            int progress = post.getMaxMembers() == 0
                    ? 0
                    : Math.min(100, (post.getCurrentMembers() * 100) / post.getMaxMembers());
            progressMembers.setProgress(progress);

            if (post.isRecruiting()) {
                chipStatus.setText("모집 중");
                chipStatus.setChipBackgroundColorResource(R.color.recruiting);
                chipStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            } else {
                chipStatus.setText("모집 완료");
                chipStatus.setChipBackgroundColorResource(R.color.done);
                chipStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            }

            itemView.setOnClickListener(v -> listener.onItemClick(post));
        }
    }
}
