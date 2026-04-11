package kr.ac.mjc.myappdev.board;

import android.content.Intent;
import android.os.Bundle;
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

import kr.ac.mjc.myappdev.databinding.FragmentStudyBoardBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class StudyBoardFragment extends Fragment {

    private FragmentStudyBoardBinding binding;
    private StudyAdapter adapter;

    private String selectedField    = "";
    private String selectedLocation = "";

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
                loadPosts();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        binding.spinnerField.setOnItemSelectedListener(filterListener);
        binding.spinnerLocation.setOnItemSelectedListener(filterListener);
    }

    private void loadPosts() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Query query = FirebaseUtil.getStudyPostsRef()
                .orderBy("createdAt", Query.Direction.DESCENDING);

        if (!selectedField.isEmpty()) {
            query = query.whereEqualTo("field", selectedField);
        }
        if (!selectedLocation.isEmpty()) {
            query = query.whereEqualTo("location", selectedLocation);
        }

        query.get().addOnSuccessListener(snapshots -> {
            List<StudyPost> posts = new ArrayList<>();
            for (var doc : snapshots) {
                StudyPost post = doc.toObject(StudyPost.class);
                post.setPostId(doc.getId());
                posts.add(post);
            }
            adapter.submitList(posts);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> binding.progressBar.setVisibility(View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}