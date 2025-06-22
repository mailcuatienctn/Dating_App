package com.example.datingapp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gắn Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Cài đặt font chữ cho tiêu đề trên Toolbar
        Typeface customFont = ResourcesCompat.getFont(this, R.font.uvn);
        TextView titleTextView = null;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                titleTextView = (TextView) view;
                break;
            }
        }

        if (titleTextView != null) {
            titleTextView.setTypeface(customFont);
        }

        // Hiển thị nút back
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Xử lý khi nhấn nút back
        toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish(); // hoặc moveTaskToBack(true) nếu muốn về home
            }
        });


        // Xử lý bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setItemIconTintList(null);

        // Hiển thị mặc định fragment đầu tiên (ví dụ HomeFragment)
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_search) {
                    selectedFragment = new HomeFragment();
                } else if (item.getItemId() == R.id.nav_dating) {
                    selectedFragment = new DatingFragment(); // Giả sử có DatingFragment
                } else if (item.getItemId() == R.id.nav_message) {
                    selectedFragment = new ChatFragment(); // Giả sử có MessageFragment
                } else if (item.getItemId() == R.id.nav_profile) {
                    selectedFragment = new UserFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }

                return true;
            }
        });
    }
}
