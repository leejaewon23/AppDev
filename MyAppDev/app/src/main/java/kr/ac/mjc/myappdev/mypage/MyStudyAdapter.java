package kr.ac.mjc.myappdev.mypage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.model.StudyPost;

public class MyStudyAdapter extends RecyclerView.Adapter<MyStudyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(StudyPost post);
    }

    private List<StudyPost> posts = new ArrayList<>();
    private final OnItemClickListener listener;

    public MyStudyAdapter(OnItemClickListener listener) {
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
                .inflate(R.layout.item_my_study, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(posts.get(position), listener);
    }

    @Override
    public int getItemCount() { return posts.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvFieldLocation, tvMembers, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle         = itemView.findViewById(R.id.tvTitle);
            tvFieldLocation = itemView.findViewById(R.id.tvFieldLocation);
            tvMembers       = itemView.findViewById(R.id.tvMembers);
            tvStatus        = itemView.findViewById(R.id.tvStatus);
        }

        void bind(StudyPost post, OnItemClickListener listener) {
            tvTitle.setText(post.getTitle());
            tvFieldLocation.setText(post.getField() + " · " + post.getLocation());
            tvMembers.setText(post.getCurrentMembers() + "/" + post.getMaxMembers() + "명");
            tvStatus.setText(post.isRecruiting() ? "모집 중" : "모집 완료");
            tvStatus.setTextColor(itemView.getContext().getResources().getColor(
                    post.isRecruiting() ? R.color.recruiting : R.color.done, null));
            itemView.setOnClickListener(v -> listener.onItemClick(post));
        }
    }
}