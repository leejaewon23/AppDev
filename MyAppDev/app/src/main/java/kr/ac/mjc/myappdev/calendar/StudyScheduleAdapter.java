package kr.ac.mjc.myappdev.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import kr.ac.mjc.myappdev.R;
import kr.ac.mjc.myappdev.model.StudySchedule;

public class StudyScheduleAdapter extends RecyclerView.Adapter<StudyScheduleAdapter.ViewHolder> {

    public interface OnScheduleActionListener {
        void onEdit(StudySchedule schedule);
        void onDelete(StudySchedule schedule);
    }

    private final List<StudySchedule> schedules = new ArrayList<>();
    private final OnScheduleActionListener actionListener;
    private Set<String> manageableStudyIds = java.util.Collections.emptySet();

    public StudyScheduleAdapter(OnScheduleActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<StudySchedule> list, Set<String> manageableStudyIds) {
        schedules.clear();
        schedules.addAll(list);
        this.manageableStudyIds = manageableStudyIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(schedules.get(position), manageableStudyIds, actionListener);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTime;
        private final TextView tvTitle;
        private final TextView tvStudy;
        private final TextView tvDescription;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvStudy = itemView.findViewById(R.id.tvStudy);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(StudySchedule schedule,
                  Set<String> manageableStudyIds,
                  OnScheduleActionListener actionListener) {
            String timeText = "";
            if (schedule.getScheduledAt() != null) {
                timeText = new SimpleDateFormat("a h:mm", Locale.KOREA)
                        .format(new Date(schedule.getScheduledAt().toDate().getTime()));
            }
            tvTime.setText(timeText);
            tvTitle.setText(schedule.getTitle());
            tvStudy.setText(schedule.getStudyTitle());

            String description = schedule.getDescription();
            boolean hasDescription = description != null && !description.trim().isEmpty();
            tvDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            tvDescription.setText(description);

            boolean canManage = manageableStudyIds.contains(schedule.getStudyPostId());
            btnEdit.setVisibility(canManage ? View.VISIBLE : View.GONE);
            btnDelete.setVisibility(canManage ? View.VISIBLE : View.GONE);

            btnEdit.setOnClickListener(canManage ? v -> actionListener.onEdit(schedule) : null);
            btnDelete.setOnClickListener(canManage ? v -> actionListener.onDelete(schedule) : null);
        }
    }
}
