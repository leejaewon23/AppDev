package kr.ac.mjc.myappdev.mypage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.ac.mjc.myappdev.auth.LoginActivity;
import kr.ac.mjc.myappdev.board.StudyDetailActivity;
import kr.ac.mjc.myappdev.databinding.FragmentMyPageBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.model.User;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class MyPageFragment extends Fragment {

    private static final String TAG = "MyPageFragment";

    private FragmentMyPageBinding binding;
    private MyStudyAdapter myPostsAdapter;
    private MyStudyAdapter joinedStudyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupAdapters();
        loadUserProfile();
        loadMyPosts();
        loadJoinedStudies();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileEditActivity.class)));
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        loadUserProfile();
        loadMyPosts();
        loadJoinedStudies();
    }

    private void setupAdapters() {
        myPostsAdapter = new MyStudyAdapter(post -> {
            Intent intent = new Intent(requireContext(), StudyDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });
        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPosts.setAdapter(myPostsAdapter);
        binding.rvMyPosts.setNestedScrollingEnabled(false);

        joinedStudyAdapter = new MyStudyAdapter(post -> {
            Intent intent = new Intent(requireContext(), StudyDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });
        binding.rvJoinedStudies.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvJoinedStudies.setAdapter(joinedStudyAdapter);
        binding.rvJoinedStudies.setNestedScrollingEnabled(false);
    }

    private void loadUserProfile() {
        String uid = FirebaseUtil.getCurrentUid();
        FirebaseUtil.getUsersRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) return;
                    binding.tvNickname.setText(user.getNickname());
                    binding.tvEmail.setText(user.getEmail());
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(this)
                                .load(user.getProfileImageUrl())
                                .circleCrop()
                                .into(binding.ivProfile);
                    } else {
                        binding.ivProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "프로필 로드 실패", e));
    }

    private void loadMyPosts() {
        String uid = FirebaseUtil.getCurrentUid();
        // orderBy 없이 조회 후 메모리에서 정렬 (Firestore 복합 인덱스 불필요)
        FirebaseUtil.getStudyPostsRef()
                .whereEqualTo("authorUid", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<StudyPost> posts = new ArrayList<>();
                    for (var doc : snapshots) {
                        StudyPost post = doc.toObject(StudyPost.class);
                        post.setPostId(doc.getId());
                        posts.add(post);
                    }
                    // 최신순 정렬
                    posts.sort((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    myPostsAdapter.submitList(posts);
                    binding.tvCreatedCount.setText(String.valueOf(posts.size()));
                    binding.tvMyPostsEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "내가 만든 스터디 로드 실패", e);
                    binding.tvCreatedCount.setText("0");
                    Toast.makeText(requireContext(), "스터디 목록을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadJoinedStudies() {
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null || uid.trim().isEmpty()) {
            binding.tvJoinedCount.setText("0");
            binding.tvJoinedEmpty.setVisibility(View.VISIBLE);
            return;
        }

        var memberQueryTask = FirebaseUtil.getStudyPostsRef()
                .whereArrayContains("memberUids", uid)
                .get();
        var userDocTask = FirebaseUtil.getUsersRef().document(uid).get();

        Tasks.whenAllComplete(memberQueryTask, userDocTask)
                .addOnSuccessListener(results -> {
                    if (binding == null) {
                        return;
                    }

                    Map<String, StudyPost> mergedPosts = new LinkedHashMap<>();

                    if (memberQueryTask.isSuccessful() && memberQueryTask.getResult() != null) {
                        var memberSnapshots = memberQueryTask.getResult();
                        for (var doc : memberSnapshots) {
                            StudyPost post = doc.toObject(StudyPost.class);
                            post.setPostId(doc.getId());
                            mergedPosts.put(doc.getId(), post);
                        }
                    } else if (memberQueryTask.getException() != null) {
                        Log.e(TAG, "memberUids 기반 스터디 조회 실패", memberQueryTask.getException());
                    }

                    List<String> joinedStudyIds = new ArrayList<>();
                    if (userDocTask.isSuccessful() && userDocTask.getResult() != null) {
                        User user = userDocTask.getResult().toObject(User.class);
                        joinedStudyIds = user != null && user.getJoinedStudyIds() != null
                                ? user.getJoinedStudyIds()
                                : new ArrayList<>();
                    } else if (userDocTask.getException() != null) {
                        Log.e(TAG, "유저 문서 기반 joinedStudyIds 조회 실패", userDocTask.getException());
                    }

                    loadJoinedStudiesByIds(joinedStudyIds, mergedPosts);
                });
    }

    private void loadJoinedStudiesByIds(List<String> joinedStudyIds, Map<String, StudyPost> mergedPosts) {
        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>> tasks = new ArrayList<>();
        for (String studyId : joinedStudyIds) {
            if (studyId == null || studyId.trim().isEmpty() || mergedPosts.containsKey(studyId)) {
                continue;
            }
            tasks.add(FirebaseUtil.getStudyPostsRef().document(studyId).get());
        }

        if (tasks.isEmpty()) {
            renderJoinedStudies(mergedPosts);
            return;
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    for (Task<?> task : results) {
                        if (!task.isSuccessful() || task.getResult() == null) {
                            if (task.getException() != null) {
                                Log.e(TAG, "joinedStudyIds 단건 로드 실패", task.getException());
                            }
                            continue;
                        }

                        var doc = (com.google.firebase.firestore.DocumentSnapshot) task.getResult();
                        if (!doc.exists()) {
                            continue;
                        }

                        StudyPost post = doc.toObject(StudyPost.class);
                        if (post == null) {
                            continue;
                        }
                        String uid = FirebaseUtil.getCurrentUid();
                        if (uid == null || !post.getMemberUids().contains(uid)) {
                            continue;
                        }
                        post.setPostId(doc.getId());
                        mergedPosts.put(doc.getId(), post);
                    }
                    renderJoinedStudies(mergedPosts);
                });
    }

    private void renderJoinedStudies(Map<String, StudyPost> mergedPosts) {
        if (binding == null) {
            return;
        }
        List<StudyPost> posts = new ArrayList<>(mergedPosts.values());
        posts.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        joinedStudyAdapter.submitList(posts);
        binding.tvJoinedCount.setText(String.valueOf(posts.size()));
        binding.tvJoinedEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, w) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logout() {
        FirebaseUtil.getAuth().signOut();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
