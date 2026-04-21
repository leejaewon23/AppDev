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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
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

import kr.ac.mjc.myappdev.R;
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
    private final Set<Long> scheduleDayKeys = new LinkedHashSet<>();

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
        setupScrollMotion();
        binding.btnAddSchedule.setOnClickListener(v -> showScheduleEditor(null));
        updateCurrentMonthLabel();
        updateSelectedDateLabel();
        renderMarkedDatesOfSelectedMonth();
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
        binding.rvSchedules.setNestedScrollingEnabled(false);
        binding.rvSchedules.setHasFixedSize(false);
        binding.rvSchedules.setItemViewCacheSize(12);
        if (binding.rvSchedules.getItemAnimator() != null) {
            binding.rvSchedules.getItemAnimator().setChangeDuration(120);
        }
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
            renderMarkedDatesOfSelectedMonth();
            renderSelectedDaySchedules();
        });
    }

    private void setupScrollMotion() {
        final float maxHeroShift = getResources().getDimension(R.dimen.calendar_hero_parallax_distance);
        final float fadeDistance = getResources().getDimension(R.dimen.calendar_hero_fade_distance);

        NestedScrollView.OnScrollChangeListener listener = (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float progress = fadeDistance <= 0f ? 0f : Math.min(1f, scrollY / fadeDistance);
            float heroShift = Math.min(maxHeroShift, scrollY * 0.35f);
            float calendarShift = Math.min(maxHeroShift * 0.5f, scrollY * 0.14f);

            binding.cardHero.setTranslationY(-heroShift);
            binding.cardHero.setAlpha(1f - (0.18f * progress));
            binding.cardMonthCalendar.setTranslationY(-calendarShift);
        };
        binding.nestedScrollCalendar.setOnScrollChangeListener(listener);
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
                        refreshScheduleDayKeys();
                        binding.progressBar.setVisibility(View.GONE);
                        renderMarkedDatesOfSelectedMonth();
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
                                refreshScheduleDayKeys();
                                binding.progressBar.setVisibility(View.GONE);
                                renderMarkedDatesOfSelectedMonth();
                                renderSelectedDaySchedules();
                            });
                })
                .addOnFailureListener(e -> {
                    refreshScheduleDayKeys();
                    binding.progressBar.setVisibility(View.GONE);
                    renderMarkedDatesOfSelectedMonth();
                    Toast.makeText(requireContext(), getString(R.string.calendar_schedule_load_failed), Toast.LENGTH_SHORT).show();
                });
    }

    private void refreshScheduleDayKeys() {
        scheduleDayKeys.clear();
        for (StudySchedule schedule : allSchedules) {
            if (schedule.getScheduledAt() == null) {
                continue;
            }
            scheduleDayKeys.add(normalizeToDay(schedule.getScheduledAt().toDate().getTime()));
        }
    }

    private void renderMarkedDatesOfSelectedMonth() {
        if (binding == null) {
            return;
        }

        List<Integer> markedDays = new ArrayList<>();
        for (Long dayKey : scheduleDayKeys) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(dayKey);
            if (date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                    && date.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)) {
                markedDays.add(date.get(Calendar.DAY_OF_MONTH));
            }
        }
        markedDays.sort(Integer::compareTo);

        binding.chipGroupMarkedDates.removeAllViews();
        if (markedDays.isEmpty()) {
            binding.tvMarkedDatesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvMarkedDatesEmpty.setVisibility(View.GONE);
        for (Integer day : markedDays) {
            Chip chip = new Chip(requireContext());
            chip.setText(day + "일");
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setChipBackgroundColorResource(R.color.success_light);
            chip.setTextColor(requireContext().getColor(R.color.secondary_dark));
            chip.setOnClickListener(v -> {
                selectedDate.set(Calendar.DAY_OF_MONTH, day);
                selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                selectedDate.set(Calendar.MINUTE, 0);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);
                binding.calendarView.setDate(selectedDate.getTimeInMillis(), true, true);
                updateSelectedDateLabel();
                renderSelectedDaySchedules();
            });
            binding.chipGroupMarkedDates.addView(chip);
        }
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
            Toast.makeText(requireContext(), getString(R.string.calendar_no_manageable_studies), Toast.LENGTH_SHORT).show();
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
                .setTitle(existing == null ? getString(R.string.calendar_dialog_add_title) : getString(R.string.calendar_dialog_edit_title))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(existing == null ? getString(R.string.calendar_dialog_positive_add) : getString(R.string.calendar_dialog_positive_save), null)
                .setNegativeButton(getString(R.string.common_cancel), null)
                .create();

        dialog.setOnShowListener(d -> {
            View positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = dialogBinding.etTitle.getText().toString().trim();
                String description = dialogBinding.etDescription.getText().toString().trim();
                dialogBinding.etTitle.setError(null);
                if (TextUtils.isEmpty(title)) {
                    dialogBinding.etTitle.setError(getString(R.string.calendar_title_required));
                    return;
                }
                if (existing == null && scheduledAtMillis[0] < System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), getString(R.string.calendar_time_past_not_allowed), Toast.LENGTH_SHORT).show();
                    return;
                }

                StudyPost selectedStudy = manageableList.get(spinner.getSelectedItemPosition());
                setEditorSubmittingState(dialogBinding, dialog, spinner, existing == null, true);
                saveSchedule(existing, selectedStudy, title, description, scheduledAtMillis[0], dialog, dialogBinding, spinner);
            });
        });

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
                              AlertDialog dialog,
                              DialogScheduleEditorBinding dialogBinding,
                              Spinner spinner) {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null || uid.trim().isEmpty()) {
            setEditorSubmittingState(dialogBinding, dialog, spinner, existing == null, false);
            Toast.makeText(requireContext(), getString(R.string.calendar_auth_required), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(requireContext(), getString(R.string.calendar_schedule_added), Toast.LENGTH_SHORT).show();
                        loadStudiesAndSchedules();
                    })
                    .addOnFailureListener(e -> {
                        setEditorSubmittingState(dialogBinding, dialog, spinner, true, false);
                        Toast.makeText(requireContext(), getString(R.string.calendar_schedule_add_failed), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        FirebaseUtil.getStudySchedulesRef(existing.getStudyPostId())
                .document(existing.getScheduleId())
                .update(payload)
                .addOnSuccessListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), getString(R.string.calendar_schedule_updated), Toast.LENGTH_SHORT).show();
                    loadStudiesAndSchedules();
                })
                .addOnFailureListener(e -> {
                    setEditorSubmittingState(dialogBinding, dialog, spinner, false, false);
                    Toast.makeText(requireContext(), getString(R.string.calendar_schedule_update_failed), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDelete(StudySchedule schedule) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.calendar_delete_title))
                .setMessage(getString(R.string.calendar_delete_message))
                .setPositiveButton(getString(R.string.common_delete), (dialog, w) -> deleteSchedule(schedule))
                .setNegativeButton(getString(R.string.common_cancel), null)
                .show();
    }

    private void deleteSchedule(StudySchedule schedule) {
        FirebaseUtil.getStudySchedulesRef(schedule.getStudyPostId())
                .document(schedule.getScheduleId())
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), getString(R.string.calendar_schedule_deleted), Toast.LENGTH_SHORT).show();
                    loadStudiesAndSchedules();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), getString(R.string.calendar_schedule_delete_failed), Toast.LENGTH_SHORT).show());
    }

    private void setEditorSubmittingState(DialogScheduleEditorBinding dialogBinding,
                                          AlertDialog dialog,
                                          Spinner spinner,
                                          boolean allowStudySelection,
                                          boolean isSubmitting) {
        dialogBinding.etTitle.setEnabled(!isSubmitting);
        dialogBinding.etDescription.setEnabled(!isSubmitting);
        dialogBinding.btnPickDateTime.setEnabled(!isSubmitting);
        spinner.setEnabled(!isSubmitting && allowStudySelection);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!isSubmitting);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!isSubmitting);
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
