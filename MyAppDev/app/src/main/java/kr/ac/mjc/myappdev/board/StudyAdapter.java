package kr.ac.mjc.myappdev.board;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.model.StudyPost;

public class StudyAdapter extends ListAdapter<StudyPost, StudyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(StudyPost post);
    }

    private final OnItemClickListener listener;

    public StudyAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void submitList(List<StudyPost> newPosts) {
        super.submitList(new ArrayList<>(newPosts));
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
        holder.bind(getItem(position), listener);
    }

    @Override
    public int getItemCount() { return super.getItemCount(); }

    private static final DiffUtil.ItemCallback<StudyPost> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull StudyPost oldItem, @NonNull StudyPost newItem) {
            if (oldItem.getPostId() == null || newItem.getPostId() == null) {
                return false;
            }
            return oldItem.getPostId().equals(newItem.getPostId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudyPost oldItem, @NonNull StudyPost newItem) {
            return safe(oldItem.getTitle()).equals(safe(newItem.getTitle()))
                    && safe(oldItem.getAuthorNickname()).equals(safe(newItem.getAuthorNickname()))
                    && safe(oldItem.getDescription()).equals(safe(newItem.getDescription()))
                    && safe(oldItem.getField()).equals(safe(newItem.getField()))
                    && safe(oldItem.getLocation()).equals(safe(newItem.getLocation()))
                    && oldItem.getCurrentMembers() == newItem.getCurrentMembers()
                    && oldItem.getMaxMembers() == newItem.getMaxMembers()
                    && oldItem.isRecruiting() == newItem.isRecruiting();
        }
    };

    private static String safe(String value) {
        return value == null ? "" : value;
    }

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
            String displayTitle = post.getTitle();
            if (displayTitle == null || displayTitle.trim().isEmpty()) {
                displayTitle = "제목 없음";
            }
            tvTitle.setText(displayTitle);
            tvAuthor.setText("운영: " + post.getAuthorNickname());
            tvMembers.setText("참여 인원 " + post.getCurrentMembers() + "/" + post.getMaxMembers() + "명");
            tvField.setText(post.getField());
            tvLocation.setText(post.getLocation());
            tvSummary.setText(post.getDescription());
            applyFieldStyle(post.getField());
            applyLocationStyle(post.getLocation());
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

        private void applyFieldStyle(String field) {
            int backgroundColor;
            int textColor;
            switch (safe(field)) {
                case "코딩":
                    backgroundColor = R.color.info_light;
                    textColor = R.color.info;
                    break;
                case "취업":
                    backgroundColor = R.color.warning_light;
                    textColor = R.color.warning;
                    break;
                case "자격증":
                    backgroundColor = R.color.primary_light;
                    textColor = R.color.primary_mid;
                    break;
                case "영어":
                    backgroundColor = R.color.success_light;
                    textColor = R.color.success;
                    break;
                case "공무원":
                    backgroundColor = R.color.danger_light;
                    textColor = R.color.danger;
                    break;
                default:
                    backgroundColor = R.color.surface_soft;
                    textColor = R.color.text_secondary;
                    break;
            }
            ViewCompat.setBackgroundTintList(tvField, ContextCompat.getColorStateList(itemView.getContext(), backgroundColor));
            tvField.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
        }

        private void applyLocationStyle(String location) {
            int backgroundColor;
            int textColor;
            switch (safe(location)) {
                case "온라인":
                    backgroundColor = R.color.info_light;
                    textColor = R.color.info;
                    break;
                case "서울":
                    backgroundColor = R.color.primary_light;
                    textColor = R.color.primary_mid;
                    break;
                case "경기":
                    backgroundColor = R.color.success_light;
                    textColor = R.color.success;
                    break;
                case "인천":
                    backgroundColor = R.color.warning_light;
                    textColor = R.color.warning;
                    break;
                case "부산":
                    backgroundColor = R.color.danger_light;
                    textColor = R.color.danger;
                    break;
                case "대구":
                    backgroundColor = R.color.nav_active_bg;
                    textColor = R.color.info;
                    break;
                default:
                    backgroundColor = R.color.surface_soft;
                    textColor = R.color.text_secondary;
                    break;
            }
            ViewCompat.setBackgroundTintList(tvLocation, ContextCompat.getColorStateList(itemView.getContext(), backgroundColor));
            tvLocation.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
        }
    }
}
