package kr.ac.mjc.myappdev.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import kr.ac.mjc.myappdev.databinding.ActivityRegisterBinding;
import kr.ac.mjc.myappdev.model.User;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseUtil.getAuth();

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvGoLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirm  = binding.etPasswordConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("이메일을 입력하세요"); return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("비밀번호는 6자 이상이어야 합니다"); return;
        }
        if (!password.equals(confirm)) {
            binding.etPasswordConfirm.setError("비밀번호가 일치하지 않습니다"); return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) return;

                    // Firestore에 유저 기본 정보 저장 (닉네임은 다음 단계에서 설정)
                    User newUser = new User(firebaseUser.getUid(), email, "");
                    FirebaseUtil.getUsersRef()
                            .document(firebaseUser.getUid())
                            .set(newUser)
                            .addOnSuccessListener(v -> {
                                // 닉네임 / 프로필 설정 화면으로 이동
                                startActivity(new Intent(this, ProfileSetupActivity.class));
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "회원가입 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }
}