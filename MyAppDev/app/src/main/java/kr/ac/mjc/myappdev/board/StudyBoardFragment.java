package kr.ac.mjc.myappdev.board;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kr.ac.mjc.myappdev.databinding.FragmentStudyBoardBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class StudyBoardFragment extends Fragment {

    private FragmentStudyBoardBinding binding;
    private StudyAdapter adapter;
    private final List<StudyPost> allPosts = new ArrayList<>();

    private String selectedField    = "";
    private String selectedLocation = "";
    private String searchKeyword    = "";
    private boolean recruitingOnly;
    private boolean filtersInitialized;

    // 필터 옵션 (실제 앱에서는 서버에서 받거나 strings.xml로 관리)
    private static final String[] FIELDS    = {"전체", "코딩", "취업", "자격증", "영어", "공무원", "기타"};
    private static final String[] LOCATIONS = {"전체", "서울", "경기", "인천", "부산", "대구", "온라인"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudyBoardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupFilters();

        binding.fabCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateStudyActivity.class)));

        loadPosts();
    }

    private void setupRecyclerView() {
        adapter = new StudyAdapter(post -> {
            Intent intent = new Intent(requireContext(), StudyDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });
        binding.rvStudyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStudyPosts.setAdapter(adapter);
    }

    private void setupFilters() {
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, FIELDS);
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerField.setAdapter(fieldAdapter);

        ArrayAdapter<String> locAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, LOCATIONS);
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLocation.setAdapter(locAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (parent.getId() == binding.spinnerField.getId()) {
                    selectedField = pos == 0 ? "" : FIELDS[pos];
                } else {
                    selectedLocation = pos == 0 ? "" : LOCATIONS[pos];
                }
                if (!filtersInitialized) {
                    return;
                }
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        binding.spinnerField.setOnItemSelectedListener(filterListener);
        binding.spinnerLocation.setOnItemSelectedListener(filterListener);
        binding.cbRecruitingOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recruitingOnly = isChecked;
            if (filtersInitialized) {
                applyFilters();
            }
        });
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s == null ? "" : s.toString().trim();
                if (filtersInitialized) {
                    applyFilters();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.btnResetFilters.setOnClickListener(v -> resetFilters());
        filtersInitialized = true;
    }

    private void loadPosts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseUtil.getStudyPostsRef()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
            allPosts.clear();
            for (var doc : snapshots) {
                StudyPost post = doc.toObject(StudyPost.class);
                post.setPostId(doc.getId());
                allPosts.add(post);
            }
            binding.progressBar.setVisibility(View.GONE);
            applyFilters();
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvFilterSummary.setText("스터디 목록을 불러오지 못했습니다");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void applyFilters() {
        if (binding == null) {
            return;
        }

        List<StudyPost> filteredPosts = new ArrayList<>();
        for (StudyPost post : allPosts) {
            if (!selectedField.isEmpty() && !selectedField.equals(post.getField())) {
                continue;
            }
            if (!selectedLocation.isEmpty() && !selectedLocation.equals(post.getLocation())) {
                continue;
            }
            if (recruitingOnly && !post.isRecruiting()) {
                continue;
            }
            if (!matchesKeyword(post, searchKeyword)) {
                continue;
            }
            filteredPosts.add(post);
        }

        adapter.submitList(filteredPosts);
        binding.tvEmpty.setVisibility(filteredPosts.isEmpty() ? View.VISIBLE : View.GONE);
        updateFilterSummary(filteredPosts.size());
    }

    private boolean matchesKeyword(StudyPost post, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.getDefault());
        return safeLower(post.getTitle()).contains(normalized)
                || safeLower(post.getDescription()).contains(normalized)
                || safeLower(post.getAuthorNickname()).contains(normalized)
                || safeLower(post.getField()).contains(normalized)
                || safeLower(post.getLocation()).contains(normalized);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.getDefault());
    }

    private void updateFilterSummary(int resultCount) {
        List<String> tokens = new ArrayList<>();
        if (!selectedField.isEmpty()) {
            tokens.add(selectedField);
        }
        if (!selectedLocation.isEmpty()) {
            tokens.add(selectedLocation);
        }
        if (recruitingOnly) {
            tokens.add("모집 중");
        }
        if (!searchKeyword.isEmpty()) {
            tokens.add("\"" + searchKeyword + "\" 검색");
        }

        if (tokens.isEmpty()) {
            binding.tvFilterSummary.setText("전체 스터디 " + resultCount + "개를 보고 있습니다");
            return;
        }
        binding.tvFilterSummary.setText(String.join(" · ", tokens) + " 조건으로 " + resultCount + "개 찾음");
    }

    private void resetFilters() {
        selectedField = "";
        selectedLocation = "";
        searchKeyword = "";
        recruitingOnly = false;
        binding.spinnerField.setSelection(0);
        binding.spinnerLocation.setSelection(0);
        binding.cbRecruitingOnly.setChecked(false);
        binding.etSearch.setText("");
        applyFilters();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
