package kr.ac.mjc.myappdev.mypage;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import kr.ac.mjc.myappdev.auth.LoginActivity;
import kr.ac.mjc.myappdev.board.StudyDetailActivity;
import kr.ac.mjc.myappdev.databinding.FragmentMyPageBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.model.User;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class MyPageFragment extends Fragment {

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

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void setupAdapters() {
        // 내가 작성한 글 목록
        myPostsAdapter = new MyStudyAdapter(post -> {
            Intent intent = new Intent(requireContext(), StudyDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });
        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPosts.setAdapter(myPostsAdapter);
        binding.rvMyPosts.setNestedScrollingEnabled(false);

        // 참여 중인 스터디 목록
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
                    }
                });
    }

    private void loadMyPosts() {
        String uid = FirebaseUtil.getCurrentUid();
        FirebaseUtil.getStudyPostsRef()
                .whereEqualTo("authorUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<StudyPost> posts = new ArrayList<>();
                    for (var doc : snapshots) {
                        StudyPost post = doc.toObject(StudyPost.class);
                        post.setPostId(doc.getId());
                        posts.add(post);
                    }
                    myPostsAdapter.submitList(posts);
                    binding.tvMyPostsEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void loadJoinedStudies() {
        String uid = FirebaseUtil.getCurrentUid();
        // 내가 멤버인 스터디 (내가 작성한 것 포함)
        FirebaseUtil.getStudyPostsRef()
                .whereArrayContains("memberUids", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<StudyPost> posts = new ArrayList<>();
                    for (var doc : snapshots) {
                        StudyPost post = doc.toObject(StudyPost.class);
                        post.setPostId(doc.getId());
                        posts.add(post);
                    }
                    joinedStudyAdapter.submitList(posts);
                    binding.tvJoinedEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                });
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