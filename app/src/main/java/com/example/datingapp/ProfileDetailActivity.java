package com.example.datingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.adapter.PhotoAdapter;
import com.example.datingapp.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.*;

public class ProfileDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_PROFILE = "extra_user_profile";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String ACTION_NAVIGATE_TO_CHAT = "com.example.datingapp.NAVIGATE_TO_CHAT";

    private static final String TAG = "ProfileDetailActivity";

    private String currentUserId;
    private ShapeableImageView imageAvatar;
    private TextView textName, textAgeGender, textLocation, textBio, textHobbies, textHeight;
    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<String> photoList;
    private FirebaseFirestore db;
    private LinearLayout linearButtons;
    private FloatingActionButton btnDislike, btnLike;

    private User displayedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        boolean shouldHideLikeDislike = getIntent().getBooleanExtra("EXTRA_HIDE_LIKE_DISLIKE", false);

        imageAvatar = findViewById(R.id.image_avatar);
        textName = findViewById(R.id.text_name);
        textAgeGender = findViewById(R.id.text_age_gender);
        textLocation = findViewById(R.id.text_location);
        textBio = findViewById(R.id.text_bio);
        textHobbies = findViewById(R.id.text_hobbies);
        recyclerPhotos = findViewById(R.id.recycler_photos);
        linearButtons = findViewById(R.id.linear_buttons);
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        textHeight = findViewById(R.id.text_height);

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, photoList);
        recyclerPhotos.setAdapter(photoAdapter);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("uid", null);

        String flag = getIntent().getStringExtra("EXTRA_FLAG");
        String targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        displayedUser = (User) getIntent().getSerializableExtra(EXTRA_USER_PROFILE);

        if (shouldHideLikeDislike || currentUserId == null || (targetUserId != null && currentUserId.equals(targetUserId))) {
            linearButtons.setVisibility(View.GONE);
        } else {
            linearButtons.setVisibility(View.VISIBLE);
        }

        if ("1".equals(flag) && displayedUser != null) {
            displayUserProfile(displayedUser);
        } else if (targetUserId != null) {
            db.collection("profiles")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            displayedUser = documentSnapshot.toObject(User.class);
                            if (displayedUser != null) {
                                displayedUser.setUid(targetUserId);
                                displayUserProfile(displayedUser);
                            } else {
                                Toast.makeText(this, "Không thể đọc thông tin hồ sơ.", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "Hồ sơ không tồn tại.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu: " + e.getMessage());
                        Toast.makeText(this, "Lỗi khi lấy dữ liệu", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            Toast.makeText(this, "Không thể tải hồ sơ. Thiếu thông tin.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnLike.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thực hiện.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (displayedUser != null && !TextUtils.isEmpty(displayedUser.getUid())) {
                saveSwipe(currentUserId, true, displayedUser.getUid());
            }
        });

        btnDislike.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thực hiện.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (displayedUser != null && !TextUtils.isEmpty(displayedUser.getUid())) {
                saveSwipe(currentUserId, false, displayedUser.getUid());
            }
        });
    }

    private void displayUserProfile(User user) {
        textName.setText(user.getName());
        textAgeGender.setText(user.getAge() + " tuổi • " + user.getGender());
        textBio.setText(!TextUtils.isEmpty(user.getBio()) ? user.getBio() : "Chưa có mô tả bản thân");
        textHeight.setText(user.getHeight() + " cm");

        List<String> favorites = user.getFavorites();
        if (favorites != null && !favorites.isEmpty()) {
            textHobbies.setText(TextUtils.join(", ", favorites));
        } else {
            textHobbies.setText("Chưa có sở thích.");
        }

        if (user.getLatitude() != null && user.getLongitude() != null) {
            Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
            try {
                List<Address> addresses = geocoder.getFromLocation(user.getLatitude(), user.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String location = address.getAdminArea();
                    if (TextUtils.isEmpty(location) && address.getLocality() != null) {
                        location = address.getLocality();
                    }
                    textLocation.setText(!TextUtils.isEmpty(location) ? location : "Vị trí không xác định");
                } else {
                    textLocation.setText("Vị trí không xác định");
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
                textLocation.setText("Không thể xác định vị trí");
            }
        } else {
            textLocation.setText("Vị trí chưa cập nhật");
        }

        List<String> imgUrls = user.getImgUrls();
        if (imgUrls != null && !imgUrls.isEmpty()) {
            Glide.with(this).load(imgUrls.get(0)).into(imageAvatar);
            photoList.clear();
            photoList.addAll(imgUrls);
            photoAdapter.notifyDataSetChanged();
        } else {
            imageAvatar.setImageResource(R.drawable.bg_avatar_circle);
            photoList.clear();
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void saveSwipe(String currentUid, boolean isLiked, String targetUid) {
        finishWithViewedUid();
        if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(targetUid)) return;

        String collection = isLiked ? "likes" : "dislikes";
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());
        if (isLiked) {
            data.put("liked", 1);
        }

        db.collection("swipes").document(currentUid)
                .collection(collection).document(targetUid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (isLiked) {
                        checkMatch(currentUid, targetUid);
                        Map<String, Object> likerData = new HashMap<>();
                        likerData.put("timestamp", FieldValue.serverTimestamp());
                        db.collection("profiles")
                                .document(targetUid)
                                .collection("likes_received")
                                .document(currentUid)
                                .set(likerData);
                    }
                    Toast.makeText(this, isLiked ? "Bạn đã like!" : "Bạn đã bỏ qua!", Toast.LENGTH_SHORT).show();
                });

        Map<String, Object> fieldUpdate = new HashMap<>();
        fieldUpdate.put(targetUid, 1);
        db.collection("swipes").document(currentUid)
                .set(fieldUpdate, SetOptions.merge());
    }

    private void checkMatch(String currentUid, String targetUid) {
        db.collection("swipes")
                .document(targetUid)
                .collection("likes")
                .document(currentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        createMatch(currentUid, targetUid);
                    } else {
                        finishWithViewedUid();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi kiểm tra match: " + e.getMessage());
                    finishWithViewedUid();
                });
    }

    private void createMatch(String user1, String user2) {
        Timestamp timestamp = Timestamp.now();
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("timestamp", timestamp);
        matchData.put("matchedWith", user2);

        db.collection("matches")
                .document(user1)
                .collection("matchedWith")
                .document(user2)
                .set(matchData);

        matchData.put("matchedWith", user1);
        db.collection("matches")
                .document(user2)
                .collection("matchedWith")
                .document(user1)
                .set(matchData);

        Toast.makeText(this, "🎉 Bạn đã match!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(ACTION_NAVIGATE_TO_CHAT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        finishWithViewedUid();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void finishWithViewedUid() {
        if (displayedUser != null && displayedUser.getUid() != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_USER_ID, displayedUser.getUid());  // Gửi User ID về cho Activity A
            setResult(RESULT_OK, resultIntent);  // Gửi kết quả thành công về Activity A
        }
        finish();
    }

}
