package kr.ac.mjc.myappdev.mypage;

import android.os.Bundle;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

import kr.ac.mjc.myappdev.databinding.ActivityProfileSetupBinding;
import kr.ac.mjc.myappdev.model.User;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileSetupBinding binding;
    private Uri selectedImageUri;
    private String existingImageUrl = "";

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivProfile.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvTitle.setText("프로필 수정");
        binding.tvSubtitle.setText("닉네임과 프로필 사진을 수정할 수 있습니다");
        binding.btnComplete.setText("저장");
        binding.ivProfile.setOnClickListener(v -> pickImage.launch("image/*"));
        binding.btnComplete.setOnClickListener(v -> saveProfile());

        binding.tvSkip.setText("취소");
        binding.tvSkip.setOnClickListener(v -> finish());

        loadCurrentProfile();
    }

    private void loadCurrentProfile() {
        setLoading(true);
        String uid = FirebaseUtil.getCurrentUid();
        if (uid == null) {
            setLoading(false);
            Toast.makeText(this, "로그인 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUtil.getUsersRef().document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "프로필을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    binding.etNickname.setText(user.getNickname());
                    existingImageUrl = user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "";
                    if (!existingImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(existingImageUrl)
                                .circleCrop()
                                .into(binding.ivProfile);
                    }
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "프로필을 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveProfile() {
        String nickname = binding.etNickname.getText().toString().trim();
        if (TextUtils.isEmpty(nickname)) {
            binding.etNickname.setError("닉네임을 입력하세요");
            return;
        }
        if (nickname.length() < 2 || nickname.length() > 12) {
            binding.etNickname.setError("닉네임은 2~12자로 입력하세요");
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(nickname);
        } else {
            updateProfile(nickname, existingImageUrl);
        }
    }

    private void uploadImageThenSave(String nickname) {
        String uid = FirebaseUtil.getCurrentUid();
        StorageReference ref = FirebaseUtil.getProfileStorageRef(uid);
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                updateProfile(nickname, uri.toString())))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfile(String nickname, String imageUrl) {
        String uid = FirebaseUtil.getCurrentUid();
        FirebaseUtil.getUsersRef()
                .document(uid)
                .update("nickname", nickname, "profileImageUrl", imageUrl)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "프로필이 수정되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "프로필 저장 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnComplete.setEnabled(!loading);
        binding.etNickname.setEnabled(!loading);
        binding.ivProfile.setEnabled(!loading);
        binding.tvSkip.setEnabled(!loading);
    }
}
