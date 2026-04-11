package kr.ac.mjc.myappdev;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import kr.ac.mjc.myappdev.auth.LoginActivity;
import kr.ac.mjc.myappdev.board.StudyBoardFragment;
import kr.ac.mjc.myappdev.chat.ChatListFragment;
import kr.ac.mjc.myappdev.databinding.ActivityMainBinding;
import kr.ac.mjc.myappdev.mypage.MyPageFragment;
import kr.ac.mjc.myappdev.util.FirebaseUtil;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 로그인 상태 확인
        if (!FirebaseUtil.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 초기 Fragment: 스터디 게시판
        loadFragment(new StudyBoardFragment());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_board) {
                loadFragment(new StudyBoardFragment());
            } else if (id == R.id.nav_chat) {
                loadFragment(new ChatListFragment());
            } else if (id == R.id.nav_mypage) {
                loadFragment(new MyPageFragment());
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
