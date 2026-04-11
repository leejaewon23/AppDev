package kr.ac.mjc.myappdev.board;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

import kr.ac.mjc.myappdev.databinding.ActivityCreateStudyBinding;
import kr.ac.mjc.myappdev.model.StudyPost;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class CreateStudyActivity extends AppCompatActivity {

    private ActivityCreateStudyBinding binding;
    private String editPostId;   // 수정 모드일 때 게시글 ID
    private boolean isEditMode;

    private static final String[] FIELDS    = {"코딩", "취업", "자격증", "영어", "공무원", "기타"};
    private static final String[] LOCATIONS = {"서울", "경기", "인천", "부산", "대구", "온라인"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateStudyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editPostId = getIntent().getStringExtra("postId");
        isEditMode = editPostId != null;

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "스터디 수정" : "스터디 만들기");
        }

        setupSpinners();

        if (isEditMode) loadExistingPost();

        binding.btnSubmit.setText(isEditMode ? "수정 완료" : "등록하기");
        binding.btnSubmit.setOnClickListener(v -> submitPost());
    }

    private void setupSpinners() {
        binding.spinnerField.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, FIELDS));
        binding.spinnerLocation.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LOCATIONS));
    }

    private void loadExistingPost() {
        FirebaseUtil.getStudyPostsRef().document(editPostId).get()
                .addOnSuccessListener(doc -> {
                    StudyPost post = doc.toObject(StudyPost.class);
                    if (post == null) return;
                    binding.etTitle.setText(post.getTitle());
                    binding.etDescription.setText(post.getDescription());
                    binding.etMaxMembers.setText(String.valueOf(post.getMaxMembers()));
                    // Spinner 선택 복원
                    for (int i = 0; i < FIELDS.length; i++) {
                        if (FIELDS[i].equals(post.getField())) {
                            binding.spinnerField.setSelection(i);
                            break;
                        }
                    }
                    for (int i = 0; i < LOCATIONS.length; i++) {
                        if (LOCATIONS[i].equals(post.getLocation())) {
                            binding.spinnerLocation.setSelection(i);
                            break;
                        }
                    }
                });
    }

    private void submitPost() {
        String title       = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String maxStr      = binding.etMaxMembers.getText().toString().trim();
        String field       = binding.spinnerField.getSelectedItem().toString();
        String location    = binding.spinnerLocation.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) { binding.etTitle.setError("제목을 입력하세요"); return; }
        if (TextUtils.isEmpty(description)) { binding.etDescription.setError("내용을 입력하세요"); return; }
        if (TextUtils.isEmpty(maxStr)) { binding.etMaxMembers.setError("최대 인원을 입력하세요"); return; }

        int maxMembers = Integer.parseInt(maxStr);
        if (maxMembers < 2 || maxMembers > 20) {
            binding.etMaxMembers.setError("2~20명 사이로 입력하세요"); return;
        }

        setLoading(true);

        if (isEditMode) {
            updatePost(title, description, field, location, maxMembers);
        } else {
            createPost(title, description, field, location, maxMembers);
        }
    }

    private void createPost(String title, String description,
                            String field, String location, int maxMembers) {
        String uid = FirebaseUtil.getCurrentUid();

        // 작성자 닉네임 조회 후 게시글 저장
        FirebaseUtil.getUsersRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    String nickname = doc.getString("nickname");
                    StudyPost post = new StudyPost(uid, nickname, title, description,
                            field, location, maxMembers);
                    FirebaseUtil.getStudyPostsRef().add(post)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "등록 완료!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "등록 실패", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updatePost(String title, String description,
                            String field, String location, int maxMembers) {
        // 보안: 본인 게시글만 수정 가능 — Firestore Security Rules로 이중 검증
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("field", field);
        updates.put("location", location);
        updates.put("maxMembers", maxMembers);
        updates.put("updatedAt", Timestamp.now());

        FirebaseUtil.getStudyPostsRef().document(editPostId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "수정 완료!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}