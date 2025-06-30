package com.example.datingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build; // ⭐ IMPORT này cần thiết ⭐
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
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

    private BroadcastReceiver navigateToChatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ProfileDetailActivity.ACTION_NAVIGATE_TO_CHAT)) {
                Log.d(TAG, "Received broadcast to navigate to Chat tab.");
                bottomNav.setSelectedItemId(R.id.nav_message);
            }
        }
    };

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
                    .add(R.id.fragment_container, homeFragment, HomeFragment.class.getSimpleName())
                    .add(R.id.fragment_container, datingFragment, DatingFragment.class.getSimpleName())
                    .add(R.id.fragment_container, matchFragment, MatchFragment.class.getSimpleName())
                    .add(R.id.fragment_container, userFragment, UserFragment.class.getSimpleName())
                    .hide(datingFragment)
                    .hide(matchFragment)
                    .hide(userFragment)
                    .show(homeFragment)
                    .commit();
            activeFragment = homeFragment;
        } else {
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag(HomeFragment.class.getSimpleName());
            datingFragment = (DatingFragment) fragmentManager.findFragmentByTag(DatingFragment.class.getSimpleName());
            matchFragment = (MatchFragment) fragmentManager.findFragmentByTag(MatchFragment.class.getSimpleName());
            userFragment = (UserFragment) fragmentManager.findFragmentByTag(UserFragment.class.getSimpleName());

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

        bottomNav.setSelectedItemId(R.id.nav_search);

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_search) {
                    selectedFragment = homeFragment;
                } else if (itemId == R.id.nav_dating) {
                    selectedFragment = datingFragment;
                    BadgeDrawable badge = bottomNav.getBadge(item.getItemId());
                    if (badge != null) {
                        badge.setVisible(false);
                        badge.clearNumber();
                    }
                } else if (itemId == R.id.nav_message) {
                    selectedFragment = matchFragment;
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = userFragment;
                }

                if (selectedFragment != null && selectedFragment != activeFragment) {
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.hide(activeFragment);
                    transaction.show(selectedFragment);
                    transaction.commit();
                    activeFragment = selectedFragment;
                    return true;
                }
                return false;
            }
        });

        updateDatingFragmentBadgeFromPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ProfileDetailActivity.ACTION_NAVIGATE_TO_CHAT);

        // ⭐ THE FIX: Add RECEIVER_NOT_EXPORTED flag for Android 14+ ⭐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34) and higher
            registerReceiver(navigateToChatReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(navigateToChatReceiver, filter);
        }
        Log.d(TAG, "BroadcastReceiver registered.");

        // If you choose to use LocalBroadcastManager (recommended for in-app broadcasts)
        // LocalBroadcastManager.getInstance(this).registerReceiver(navigateToChatReceiver, filter);
        // Log.d(TAG, "LocalBroadcastManager receiver registered.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ⭐ Unregistering is generally the same, no special flags needed for unregister ⭐
        unregisterReceiver(navigateToChatReceiver);
        Log.d(TAG, "BroadcastReceiver unregistered.");

        // If you choose to use LocalBroadcastManager
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(navigateToChatReceiver);
        // Log.d(TAG, "LocalBroadcastManager receiver unregistered.");
    }

    @Override
    public void onUpdateBadge(int count) {
        Log.d(TAG, "onUpdateBadge called with count: " + count);
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
        Context context = getApplicationContext();
        if (context != null) {
            int unreadCount = context.getSharedPreferences(DatingFragment.PREF_NAME, Context.MODE_PRIVATE)
                    .getInt(DatingFragment.KEY_UNREAD_LIKES_COUNT, 0);
            Log.d(TAG, "Unread likes count from SharedPreferences on startup: " + unreadCount);
            onUpdateBadge(unreadCount);
        }
    }
}