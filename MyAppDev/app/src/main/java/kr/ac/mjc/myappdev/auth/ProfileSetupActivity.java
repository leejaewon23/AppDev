package kr.ac.mjc.myappdev.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.StorageReference;

import kr.ac.mjc.myappdev.MainActivity;
import kr.ac.mjc.myappdev.databinding.ActivityProfileSetupBinding;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class ProfileSetupActivity extends AppCompatActivity {

    private ActivityProfileSetupBinding binding;
    private Uri selectedImageUri;

    // 갤러리에서 이미지 선택
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

        binding.ivProfile.setOnClickListener(v -> pickImage.launch("image/*"));
        binding.btnComplete.setOnClickListener(v -> saveProfile());
        binding.tvSkip.setOnClickListener(v -> goToMain());
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
            saveToFirestore(nickname, "");
        }
    }

    private void uploadImageThenSave(String nickname) {
        String uid = FirebaseUtil.getCurrentUid();
        StorageReference ref = FirebaseUtil.getProfileStorageRef(uid);
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                saveToFirestore(nickname, uri.toString())))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String nickname, String imageUrl) {
        String uid = FirebaseUtil.getCurrentUid();
        FirebaseUtil.getUsersRef()
                .document(uid)
                .update("nickname", nickname, "profileImageUrl", imageUrl)
                .addOnSuccessListener(v -> goToMain())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "프로필 저장 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnComplete.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
