package kr.ac.mjc.myappdev.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kr.ac.mjc.myappdev.databinding.DialogScheduleEditorBinding;
import kr.ac.mjc.myappdev.databinding.FragmentCalendarBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.model.StudySchedule;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private StudyScheduleAdapter adapter;

    private final List<StudySchedule> allSchedules = new ArrayList<>();
    private final Map<String, StudyPost> memberStudies = new LinkedHashMap<>();
    private final Map<String, StudyPost> manageableStudies = new LinkedHashMap<>();

    private final Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        selectedDate.setTimeInMillis(normalizeToDay(System.currentTimeMillis()));
        setupList();
        setupCalendar();
        binding.btnAddSchedule.setOnClickListener(v -> showScheduleEditor(null));
        updateCurrentMonthLabel();
        updateSelectedDateLabel();
        loadStudiesAndSchedules();
    }

    private void setupList() {
        adapter = new StudyScheduleAdapter(new StudyScheduleAdapter.OnScheduleActionListener() {
            @Override
            public void onEdit(StudySchedule schedule) {
                showScheduleEditor(schedule);
            }

            @Override
            public void onDelete(StudySchedule schedule) {
                confirmDelete(schedule);
            }
        });
        binding.rvSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSchedules.setAdapter(adapter);
    }

    private void setupCalendar() {
        binding.calendarView.setDate(selectedDate.getTimeInMillis(), false, true);
        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            selectedDate.set(Calendar.HOUR_OF_DAY, 0);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            updateCurrentMonthLabel();
            updateSelectedDateLabel();
            renderSelectedDaySchedules();
        });
    }

    private void updateCurrentMonthLabel() {
        String monthLabel = new SimpleDateFormat("M월", Locale.KOREA)
                .format(new Date(selectedDate.getTimeInMillis()));
        binding.tvCurrentMonth.setText(monthLabel);
    }

    private void updateSelectedDateLabel() {
        String label = new SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREA)
                .format(new Date(selectedDate.getTimeInMillis()));
        binding.tvSelectedDate.setText(label + " 일정");
    }

    private void loadStudiesAndSchedules() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null || uid.trim().isEmpty()) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAddSchedule.setEnabled(false);

        FirebaseUtil.getStudyPostsRef()
                .whereArrayContains("memberUids", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    memberStudies.clear();
                    manageableStudies.clear();
                    List<Task<com.google.firebase.firestore.QuerySnapshot>> scheduleTasks = new ArrayList<>();

                    for (var doc : snapshots) {
                        StudyPost post = doc.toObject(StudyPost.class);
                        if (post == null) {
                            continue;
                        }
                        post.setPostId(doc.getId());
                        memberStudies.put(doc.getId(), post);
                        if (uid.equals(post.getAuthorUid())) {
                            manageableStudies.put(doc.getId(), post);
                        }
                        scheduleTasks.add(FirebaseUtil.getStudySchedulesRef(doc.getId()).get());
                    }

                    binding.btnAddSchedule.setEnabled(!manageableStudies.isEmpty());
                    if (scheduleTasks.isEmpty()) {
                        allSchedules.clear();
                        binding.progressBar.setVisibility(View.GONE);
                        renderSelectedDaySchedules();
                        return;
                    }

                    Tasks.whenAllComplete(scheduleTasks)
                            .addOnSuccessListener(results -> {
                                allSchedules.clear();
                                for (int i = 0; i < results.size(); i++) {
                                    Task<?> task = results.get(i);
                                    if (!task.isSuccessful() || task.getResult() == null) {
                                        continue;
                                    }
                                    var scheduleSnapshots = (com.google.firebase.firestore.QuerySnapshot) task.getResult();
                                    for (var scheduleDoc : scheduleSnapshots) {
                                        StudySchedule schedule = scheduleDoc.toObject(StudySchedule.class);
                                        if (schedule == null) {
                                            continue;
                                        }
                                        schedule.setScheduleId(scheduleDoc.getId());
                                        if (schedule.getStudyPostId() == null || schedule.getStudyPostId().isEmpty()) {
                                            schedule.setStudyPostId(scheduleDoc.getReference().getParent().getParent().getId());
                                        }
                                        StudyPost post = memberStudies.get(schedule.getStudyPostId());
                                        if (post != null && (schedule.getStudyTitle() == null || schedule.getStudyTitle().isEmpty())) {
                                            schedule.setStudyTitle(post.getTitle());
                                        }
                                        allSchedules.add(schedule);
                                    }
                                }
                                allSchedules.sort(Comparator.comparing(StudySchedule::getScheduledAt,
                                        Comparator.nullsLast(Comparator.naturalOrder())));
                                binding.progressBar.setVisibility(View.GONE);
                                renderSelectedDaySchedules();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "일정을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderSelectedDaySchedules() {
        List<StudySchedule> selectedSchedules = new ArrayList<>();
        for (StudySchedule schedule : allSchedules) {
            if (schedule.getScheduledAt() == null) {
                continue;
            }
            if (isSameDay(schedule.getScheduledAt().toDate().getTime(), selectedDate.getTimeInMillis())) {
                selectedSchedules.add(schedule);
            }
        }
        selectedSchedules.sort(Comparator.comparing(StudySchedule::getScheduledAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Set<String> manageableIds = new LinkedHashSet<>(manageableStudies.keySet());
        adapter.submitList(selectedSchedules, manageableIds);
        binding.tvEmpty.setVisibility(selectedSchedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showScheduleEditor(@Nullable StudySchedule existing) {
        if (manageableStudies.isEmpty()) {
            Toast.makeText(requireContext(), "일정을 관리할 스터디가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        List<StudyPost> manageableList = new ArrayList<>(manageableStudies.values());
        DialogScheduleEditorBinding dialogBinding = DialogScheduleEditorBinding.inflate(getLayoutInflater());

        Spinner spinner = dialogBinding.spinnerStudy;
        List<String> studyTitles = new ArrayList<>();
        for (StudyPost post : manageableList) {
            studyTitles.add(post.getTitle());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, studyTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        long[] scheduledAtMillis = new long[] {
                existing != null && existing.getScheduledAt() != null
                        ? existing.getScheduledAt().toDate().getTime()
                        : selectedDate.getTimeInMillis() + (12 * 60 * 60 * 1000L)
        };

        if (existing != null) {
            dialogBinding.etTitle.setText(existing.getTitle());
            dialogBinding.etDescription.setText(existing.getDescription());
            int index = findStudyIndexById(manageableList, existing.getStudyPostId());
            spinner.setSelection(Math.max(index, 0));
            spinner.setEnabled(false);
        }

        updateDateTimePreview(dialogBinding.tvDateTimeValue, scheduledAtMillis[0]);
        dialogBinding.btnPickDateTime.setOnClickListener(v -> pickDateTime(scheduledAtMillis, millis ->
                updateDateTimePreview(dialogBinding.tvDateTimeValue, millis)));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(existing == null ? "일정 추가" : "일정 수정")
                .setView(dialogBinding.getRoot())
                .setPositiveButton(existing == null ? "등록" : "저장", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = dialogBinding.etTitle.getText().toString().trim();
            String description = dialogBinding.etDescription.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                dialogBinding.etTitle.setError("일정 제목을 입력하세요");
                return;
            }

            StudyPost selectedStudy = manageableList.get(spinner.getSelectedItemPosition());
            saveSchedule(existing, selectedStudy, title, description, scheduledAtMillis[0], dialog);
        }));

        dialog.show();
    }

    private void pickDateTime(long[] scheduledAtMillis, OnDateTimePicked callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(scheduledAtMillis[0]);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                scheduledAtMillis[0] = calendar.getTimeInMillis();
                                callback.onPicked(scheduledAtMillis[0]);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveSchedule(@Nullable StudySchedule existing,
                              StudyPost selectedStudy,
                              String title,
                              String description,
                              long scheduledAtMillis,
                              AlertDialog dialog) {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null || uid.trim().isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studyPostId", selectedStudy.getPostId());
        payload.put("studyTitle", selectedStudy.getTitle());
        payload.put("title", title);
        payload.put("description", description);
        payload.put("scheduledAt", new Timestamp(new Date(scheduledAtMillis)));
        payload.put("updatedAt", Timestamp.now());

        if (existing == null) {
            payload.put("createdByUid", uid);
            payload.put("createdAt", Timestamp.now());
            FirebaseUtil.getStudySchedulesRef(selectedStudy.getPostId())
                    .add(payload)
                    .addOnSuccessListener(ref -> {
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "일정을 등록했습니다", Toast.LENGTH_SHORT).show();
                        loadStudiesAndSchedules();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "일정 등록에 실패했습니다", Toast.LENGTH_SHORT).show());
            return;
        }

        FirebaseUtil.getStudySchedulesRef(existing.getStudyPostId())
                .document(existing.getScheduleId())
                .update(payload)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "일정을 수정했습니다", Toast.LENGTH_SHORT).show();
                    loadStudiesAndSchedules();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "일정 수정에 실패했습니다", Toast.LENGTH_SHORT).show());
    }

    private void confirmDelete(StudySchedule schedule) {
        new AlertDialog.Builder(requireContext())
                .setTitle("일정 삭제")
                .setMessage("이 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, w) -> deleteSchedule(schedule))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSchedule(StudySchedule schedule) {
        FirebaseUtil.getStudySchedulesRef(schedule.getStudyPostId())
                .document(schedule.getScheduleId())
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "일정을 삭제했습니다", Toast.LENGTH_SHORT).show();
                    loadStudiesAndSchedules();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "일정 삭제에 실패했습니다", Toast.LENGTH_SHORT).show());
    }

    private void updateDateTimePreview(TextView target, long millis) {
        String value = new SimpleDateFormat("yyyy.MM.dd (E) a h:mm", Locale.KOREA)
                .format(new Date(millis));
        target.setText(value);
    }

    private int findStudyIndexById(List<StudyPost> studies, String postId) {
        for (int i = 0; i < studies.size(); i++) {
            if (studies.get(i).getPostId().equals(postId)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSameDay(long lhsMillis, long rhsMillis) {
        Calendar lhs = Calendar.getInstance();
        lhs.setTimeInMillis(lhsMillis);
        Calendar rhs = Calendar.getInstance();
        rhs.setTimeInMillis(rhsMillis);
        return lhs.get(Calendar.YEAR) == rhs.get(Calendar.YEAR)
                && lhs.get(Calendar.DAY_OF_YEAR) == rhs.get(Calendar.DAY_OF_YEAR);
    }

    private long normalizeToDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface OnDateTimePicked {
        void onPicked(long millis);
    }
}
