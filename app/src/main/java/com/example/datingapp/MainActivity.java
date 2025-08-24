package com.example.datingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements DatingFragment.OnBadgeUpdateListener {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    private HomeFragment homeFragment;
    private DatingFragment datingFragment;
    private MatchFragment matchFragment;
    private UserFragment userFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setItemIconTintList(null);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            datingFragment = new DatingFragment();
            matchFragment = new MatchFragment();
            userFragment = new UserFragment();

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "HomeFragment")
                    .add(R.id.fragment_container, datingFragment, "DatingFragment").hide(datingFragment)
                    .add(R.id.fragment_container, matchFragment, "MatchFragment").hide(matchFragment)
                    .add(R.id.fragment_container, userFragment, "UserFragment").hide(userFragment)
                    .show(homeFragment)
                    .commit();

            activeFragment = homeFragment;
        } else {
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("HomeFragment");
            datingFragment = (DatingFragment) fragmentManager.findFragmentByTag("DatingFragment");
            matchFragment = (MatchFragment) fragmentManager.findFragmentByTag("MatchFragment");
            userFragment = (UserFragment) fragmentManager.findFragmentByTag("UserFragment");

            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment != null && fragment.isVisible()) {
                    activeFragment = fragment;
                    break;
                }
            }
            if (activeFragment == null) {
                activeFragment = homeFragment;
                fragmentManager.beginTransaction().show(homeFragment).commit();
            }
        }

        updateDatingFragmentBadgeFromPrefs();

        // Xử lý mở fragment cụ thể nếu được gửi từ notification
        String openFragment = getIntent().getStringExtra("OPEN_FRAGMENT");
        if ("MatchFragment".equals(openFragment)) {
            bottomNav.setSelectedItemId(R.id.nav_message);
            switchFragment(matchFragment);
        } else if ("UserFragment".equals(openFragment)) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
            switchFragment(userFragment);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_search);
            switchFragment(homeFragment);
        }

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_search) {
                    switchFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.nav_dating) {
                    switchFragment(datingFragment);
                    clearBadge(itemId);
                    return true;
                } else if (itemId == R.id.nav_message) {
                    switchFragment(matchFragment);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    switchFragment(userFragment);
                    return true;
                }
                return false;
            }
        });

        // Xử lý mở ChatActivity nếu cần
        handleOpenChatIfNeeded(getIntent());

        // Xử lý mở IncomingCallActivity nếu cần
        handleOpenCallIfNeeded(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleOpenChatIfNeeded(intent);
        handleOpenCallIfNeeded(intent);
    }

    private void handleOpenChatIfNeeded(Intent intent) {
        if (intent.getBooleanExtra("OPEN_CHAT", false)) {
            String matchId = intent.getStringExtra("matchId");
            String matchName = intent.getStringExtra("matchName");
            String matchAvatarUrl = intent.getStringExtra("matchAvatarUrl");
            String chatId = intent.getStringExtra("chatId");

            Intent chatIntent = new Intent(MainActivity.this, ChatActivity.class);
            chatIntent.putExtra("matchId", matchId);
            chatIntent.putExtra("matchName", matchName);
            chatIntent.putExtra("matchAvatarUrl", matchAvatarUrl);
            chatIntent.putExtra("chatId", chatId);
            startActivity(chatIntent);

            intent.removeExtra("OPEN_CHAT"); // tránh mở lại khi quay về
        }
    }

    private void handleOpenCallIfNeeded(Intent intent) {
        if (intent.getBooleanExtra("OPEN_CALL", false)) {
            String callerId = intent.getStringExtra("callerId");
            String callerName = intent.getStringExtra("callerName");
            String callerAvatarUrl = intent.getStringExtra("callerAvatarUrl");
            String channelName = intent.getStringExtra("channelName");

            Intent callIntent = new Intent(MainActivity.this, IncomingCallActivity.class);
            callIntent.putExtra("callerId", callerId);
            callIntent.putExtra("callerName", callerName);
            callIntent.putExtra("callerAvatarUrl", callerAvatarUrl);
            callIntent.putExtra("channelName", channelName);
            startActivity(callIntent);

            intent.removeExtra("OPEN_CALL"); // tránh mở lại khi quay về
        }
    }

    private void switchFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            if (activeFragment != null) {
                transaction.hide(activeFragment);
            }
            transaction.show(fragment).commit();
            activeFragment = fragment;
        }
    }

    private void clearBadge(int itemId) {
        BadgeDrawable badge = bottomNav.getBadge(itemId);
        if (badge != null) {
            badge.setVisible(false);
            badge.clearNumber();
            Log.d(TAG, "Badge for item " + itemId + " cleared.");
        } else {
            Log.d(TAG, "No badge found for item " + itemId + " to clear.");
        }
    }

    @Override
    public void onUpdateBadge(int count) {
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_dating);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    private void updateDatingFragmentBadgeFromPrefs() {
        int unreadCount = getSharedPreferences(DatingFragment.PREF_NAME, Context.MODE_PRIVATE)
                .getInt(DatingFragment.KEY_UNREAD_LIKES_COUNT, 0);
        onUpdateBadge(unreadCount);
    }
}
