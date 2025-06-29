package com.example.datingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.datingapp.adapter.UserCardAdapter;
import com.example.datingapp.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.yuyakaido.android.cardstackview.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoveryActivity extends AppCompatActivity implements CardStackListener {

    private static final String TAG = "DiscoveryActivity";

    private CardStackView cardStackView;
    private CardStackLayoutManager layoutManager;
    private UserCardAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;

    private String currentUserId;
    private String currentUserGender = "";

    private String filterLocation = "";
    private String filterGender = "";

    private FloatingActionButton btnLike, btnDislike;

    // Constants for Broadcast Receiver (if you want to notify when *you* like someone)
    public static final String ACTION_USER_LIKED = "com.example.datingapp.USER_LIKED";
    public static final String EXTRA_LIKED_USER_NAME = "liked_user_name";

    // Constants for Broadcast Receiver when a Match occurs
    public static final String ACTION_USER_MATCHED = "com.example.datingapp.USER_MATCHED";
    public static final String EXTRA_MATCHED_USER_NAME = "matched_user_name";

    // Hàm onCreate() là hàm chính được gọi khi màn hình DiscoveryActivity được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Thiết lập giao diện cho màn hình (view)
        setContentView(R.layout.activity_discovery);

        // Khởi tạo Firebase Authentication và Firestore
        db = FirebaseFirestore.getInstance();

        // Lấy thể hiện của SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        // Lấy UID đã lưu
        currentUserId = prefs.getString("uid", null); // Hoặc prefs.getString("uid", ""); nếu bạn thích chuỗi rỗng
        Log.d("currentid", currentUserId);

        // Kiểm tra xem người dùng đã đăng nhập chưa, nếu chưa thì thoát
        if (currentUserId != null) {
            Log.d("uidhientai", currentUserId);
            fetchCurrentUserProfileGender(); // Lấy giới tính của người dùng để lọc hồ sơ
        } else {
            Toast.makeText(this, "You need to log in to continue", Toast.LENGTH_SHORT).show();
            finish(); // Đóng màn hình nếu chưa đăng nhập
            return;
        }

        // Lấy thông tin bộ lọc giới tính và vị trí từ màn hình trước đó
        Intent intent = getIntent();
        if (intent != null) {
            filterLocation = intent.getStringExtra("selectedLocation");
            filterGender = intent.getStringExtra("selectedGender");
        }

        // Thiết lập CardStackView và các nút like, dislike
        cardStackView = findViewById(R.id.card_stack_view);
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);

        layoutManager = new CardStackLayoutManager(this, this);
        adapter = new UserCardAdapter(userList, this);
        cardStackView.setLayoutManager(layoutManager);
        cardStackView.setAdapter(adapter);

        // Xử lý sự kiện khi người dùng nhấn nút like
        btnLike.setOnClickListener(v -> {
            if (layoutManager.getTopPosition() < adapter.getItemCount()) {
                SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Right)
                        .setDuration(Duration.Normal.duration)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe(); // Vuốt thẻ sang phải (like)
            } else {
                Toast.makeText(this, "No more profiles to like!", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý sự kiện khi người dùng nhấn nút dislike
        btnDislike.setOnClickListener(v -> {
            if (layoutManager.getTopPosition() < adapter.getItemCount()) {
                SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Left)
                        .setDuration(Duration.Normal.duration)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe(); // Vuốt thẻ sang trái (dislike)
            } else {
                Toast.makeText(this, "No more profiles to dislike!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm này lấy giới tính của người dùng hiện tại từ Firestore
    private void fetchCurrentUserProfileGender() {
        db.collection("profiles").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserGender = documentSnapshot.getString("gender");
                        loadUsers(); // Sau khi lấy giới tính, tải các hồ sơ người dùng
                    } else {
                        Toast.makeText(this, "Your profile not found. Please create a profile.", Toast.LENGTH_LONG).show();
                        loadUsers(); // Nếu không có hồ sơ, vẫn tiếp tục tải hồ sơ khác
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading your profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadUsers(); // Nếu gặp lỗi, vẫn tiếp tục tải hồ sơ khác
                });
    }

    // Hàm này lưu hành động "thích" hoặc "không thích" của người dùng
    private void saveSwipe(String targetUid, boolean isLiked, String targetUserName) {
        if (currentUserId == null) {
            Log.e(TAG, "currentUserId is null, cannot save swipe.");
            return;
        }

        String swipeCollectionName = isLiked ? "likes" : "dislikes";
        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("swipes")
                .document(currentUserId)
                .collection(swipeCollectionName)
                .document(targetUid)
                .set(swipeData)
                .addOnSuccessListener(aVoid -> {
                    if (isLiked) {
                        Map<String, Object> receivedLikeData = new HashMap<>();
                        receivedLikeData.put("timestamp", FieldValue.serverTimestamp());
                        receivedLikeData.put("likerUid", currentUserId);

                        db.collection("profiles")
                                .document(targetUid)
                                .collection("likes_received")
                                .document(currentUserId)
                                .set(receivedLikeData)
                                .addOnSuccessListener(bVoid -> {
                                    checkForMatch(currentUserId, targetUid, targetUserName); // Kiểm tra xem có match không
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error recording incoming like: " + e.getMessage(), e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving swipe: " + e.getMessage(), e);
                    Toast.makeText(DiscoveryActivity.this, "Error saving interaction.", Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm kiểm tra xem người dùng có "thích" lại mình không, nếu có thì tạo match
    private void checkForMatch(String currentUserUid, String targetUserUid, String targetUserName) {
        db.collection("swipes")
                .document(targetUserUid)
                .collection("likes")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        sendMatchBroadcast(targetUserName); // Gửi broadcast thông báo có match
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for match: " + e.getMessage(), e);
                });
    }

    // Hàm gửi broadcast thông báo khi có match
    private void sendMatchBroadcast(String matchedUserName) {
        Intent intent = new Intent(ACTION_USER_MATCHED);
        intent.putExtra(EXTRA_MATCHED_USER_NAME, matchedUserName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Hàm tải các hồ sơ người dùng từ Firestore và hiển thị lên CardStackView
    private void loadUsers() {
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null. Cannot load profiles.");
            return;
        }

        // Truy vấn Firestore để lấy tất cả hồ sơ người dùng
        Query query = db.collection("profiles");

        // Lọc theo giới tính (nếu có bộ lọc)
        if (!filterGender.isEmpty()) {
            query = query.whereEqualTo("gender", filterGender);
        }

        // Lọc theo vị trí (nếu có bộ lọc)
        if (!filterLocation.isEmpty()) {
            query = query.whereEqualTo("province", filterLocation);
        }

        // Giới hạn số lượng hồ sơ tải xuống (ở đây là 50)
        query.limit(50)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<User> newProfiles = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshots) {
                        User profile = document.toObject(User.class);

                        // Chắc chắn rằng người dùng hiện tại không xuất hiện trong danh sách
                        if (profile != null && !profile.getUid().equals(currentUserId)) {
                            newProfiles.add(profile);  // Thêm hồ sơ vào danh sách nếu không phải là người dùng hiện tại
                        }
                    }

                    // Cập nhật danh sách người dùng và giao diện
                    userList.clear();
                    userList.addAll(newProfiles);
                    adapter.notifyDataSetChanged(); // Cập nhật UI

                    // Kiểm tra xem có hồ sơ nào không
                    if (userList.isEmpty()) {
                        Toast.makeText(this, "No matching profiles found at this time.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No profiles to display after filtering.");
                    } else {
                        Log.d(TAG, "Loaded and displayed " + userList.size() + " matching profiles.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profiles: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Firebase error when loading profiles with filters", e);
                });
    }


    // Các phương thức CardStackListener dùng để xử lý sự kiện vuốt thẻ
    @Override
    public void onCardDragging(Direction direction, float ratio) { }

    @Override
    public void onCardSwiped(Direction direction) {
        int position = layoutManager.getTopPosition() - 1;
        if (position >= 0 && position < userList.size()) {
            User swipedUser = userList.get(position);
            saveSwipe(swipedUser.getUid(), direction == Direction.Right, swipedUser.getName());
        }
    }

    @Override
    public void onCardRewound() { }

    @Override
    public void onCardCanceled() { }

    @Override
    public void onCardAppeared(View view, int position) { }

    @Override
    public void onCardDisappeared(View view, int position) { }
}
