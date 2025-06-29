package com.example.datingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar for back button setup
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.datingapp.adapter.PhotoAdapter;
import com.example.datingapp.model.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp; // ⭐ IMPORT này cần thiết ⭐
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // ⭐ IMPORT này cần thiết nếu dùng LocalBroadcastManager ⭐


import java.io.IOException;
import java.util.*;

public class ProfileDetailActivity extends AppCompatActivity {
    public static final String EXTRA_USER_PROFILE = "extra_user_profile";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String ACTION_NAVIGATE_TO_CHAT = "com.example.datingapp.NAVIGATE_TO_CHAT"; // ⭐ Định nghĩa action ⭐

    private static final String TAG = "ProfileDetailActivity";

    private String currentUserId;
    private ShapeableImageView imageAvatar;
    private TextView textName, textAgeGender, textLocation, textBio, textHobbies;
    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<String> photoList;
    private FirebaseFirestore db;
    private LinearLayout linearButtons;
    private FloatingActionButton btnDislike, btnLike;

    private User displayedUser; // <-- user đang được hiển thị

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_detail);

        // Ánh xạ view
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

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, photoList);
        recyclerPhotos.setAdapter(photoAdapter);

        db = FirebaseFirestore.getInstance();

        // Lấy UID người dùng hiện tại
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("uid", null);

//        // ⭐ Cài đặt Toolbar (nếu bạn có Toolbar trong activity_profile_detail.xml) ⭐
//        Toolbar toolbar = findViewById(R.id.toolbar); // Đảm bảo bạn có ID này trong XML
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
//            getSupportActionBar().setTitle(""); // Tùy chỉnh tiêu đề nếu cần
//        }
//        // Xử lý sự kiện click nút back trên toolbar
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Lấy dữ liệu từ Intent
        String flag = getIntent().getStringExtra("EXTRA_FLAG");
        String targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        displayedUser = (User) getIntent().getSerializableExtra(EXTRA_USER_PROFILE);

        // Ẩn nút Like/Dislike nếu đang xem hồ sơ của chính mình hoặc không có currentUserId
        if (currentUserId == null || (targetUserId != null && currentUserId.equals(targetUserId))) {
            linearButtons.setVisibility(View.GONE);
        } else {
            linearButtons.setVisibility(View.VISIBLE);
        }

        // ⭐ Xử lý tải dữ liệu hồ sơ dựa trên flag hoặc targetUserId ⭐
        if ("1".equals(flag) && displayedUser != null) {
            // Nếu là user từ trang Home (đã có object User)
            displayUserProfile(displayedUser);
        } else if (targetUserId != null) {
            // Nếu là user từ "likes received" HOẶC không có flag/displayedUser, cần fetch từ Firestore
            db.collection("profiles")
                    .document(targetUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            displayedUser = documentSnapshot.toObject(User.class);
                            if (displayedUser != null) {
                                displayedUser.setUid(targetUserId); // Set UID thủ công nếu không có trong model User
                                displayUserProfile(displayedUser);
                            } else {
                                Toast.makeText(this, "Không thể đọc thông tin hồ sơ.", Toast.LENGTH_SHORT).show();
                                finish(); // Đóng Activity nếu không thể đọc
                            }
                        } else {
                            Log.d(TAG, "Không tìm thấy hồ sơ với uid = " + targetUserId);
                            Toast.makeText(this, "Hồ sơ không tồn tại.", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng Activity nếu không tìm thấy hồ sơ
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi lấy dữ liệu: " + e.getMessage());
                        Toast.makeText(this, "Lỗi khi lấy dữ liệu", Toast.LENGTH_SHORT).show();
                        finish(); // Đóng Activity khi có lỗi
                    });
        } else {
            // Trường hợp không có đủ thông tin để hiển thị hồ sơ
            Toast.makeText(this, "Không thể tải hồ sơ. Thiếu thông tin.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ⭐ Listener cho nút Like ⭐
        btnLike.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thực hiện thao tác này.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (displayedUser != null && !TextUtils.isEmpty(displayedUser.getUid())) {
                saveSwipe(currentUserId, true, displayedUser.getUid());
            } else {
                Toast.makeText(this, "Thông tin người dùng chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            }
        });

        // ⭐ Listener cho nút Dislike ⭐
        btnDislike.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thực hiện thao tác này.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (displayedUser != null && !TextUtils.isEmpty(displayedUser.getUid())) {
                saveSwipe(currentUserId, false, displayedUser.getUid());
            } else {
                Toast.makeText(this, "Thông tin người dùng chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ⭐ Xử lý nút back trên Toolbar chuẩn (nếu dùng) ⭐
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void displayUserProfile(User user) {
        textName.setText(user.getName());
        // Giả định user.getAge() là năm sinh. Nếu là tuổi thật, hãy bỏ (2025 -)
        textAgeGender.setText((2025 - user.getAge()) + " tuổi • " + user.getGender());
        textBio.setText(!TextUtils.isEmpty(user.getBio()) ? user.getBio() : "Chưa có mô tả bản thân");

        List<String> favorites = user.getFavorites(); // Giả định trường này là `favorites` thay vì `interests`
        if (favorites != null && !favorites.isEmpty()) {
            textHobbies.setText("Sở thích: " + TextUtils.join(", ", favorites));
        } else {
            textHobbies.setText("Chưa có sở thích nào.");
        }

        if (user.getLatitude() != null && user.getLongitude() != null) {
            Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
            try {
                List<Address> addresses = geocoder.getFromLocation(user.getLatitude(), user.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String locationDisplay = address.getAdminArea(); // Lấy tỉnh/thành phố
                    if (TextUtils.isEmpty(locationDisplay) && address.getLocality() != null) { // Fallback nếu không có adminArea
                        locationDisplay = address.getLocality();
                    }
                    textLocation.setText(!TextUtils.isEmpty(locationDisplay) ? locationDisplay : "Vị trí không xác định");
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
            photoList.clear(); // Clear any previous photos
            photoAdapter.notifyDataSetChanged();
        }
    }

    private void saveSwipe(String currentUid, boolean isLiked, String targetUid) {
        if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(targetUid)) {
            Log.e(TAG, "currentUid or targetUid is empty. Cannot save swipe.");
            return;
        }

        String swipeCollection = isLiked ? "likes" : "dislikes";
        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("swipes")
                .document(currentUid)
                .collection(swipeCollection)
                .document(targetUid)
                .set(swipeData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isLiked ? "Đã thích!" : "Đã bỏ qua.", Toast.LENGTH_SHORT).show();

                    if (isLiked) {
                        // Ghi nhận lượt thích đến cho người được thích
                        Map<String, Object> receivedLikeData = new HashMap<>();
                        receivedLikeData.put("timestamp", FieldValue.serverTimestamp());
                        receivedLikeData.put("likerUid", currentUid);
                        db.collection("profiles")
                                .document(targetUid)
                                .collection("likes_received")
                                .document(currentUid)
                                .set(receivedLikeData)
                                .addOnSuccessListener(unused -> Log.d(TAG, "Lượt thích đã ghi vào likes_received của " + targetUid))
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi ghi likes_received: " + e.getMessage()));


                        // Kiểm tra nếu người kia cũng đã like mình
                        db.collection("swipes")
                                .document(targetUid)
                                .collection("likes")
                                .document(currentUid)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // Cả hai đã thích nhau -> Match!
                                        createMatch(currentUid, targetUid);
                                    } else {
                                        // Chỉ là like đơn phương, kết thúc activity
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi kiểm tra like hai chiều: " + e.getMessage());
                                    finish(); // Kết thúc activity ngay cả khi có lỗi kiểm tra
                                });
                    } else {
                        // Nếu là dislike, kết thúc activity ngay lập tức
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lưu hành động vuốt: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi lưu thao tác", Toast.LENGTH_SHORT).show();
                });
    }

    private void createMatch(String user1, String user2) {
        Log.d(TAG, "Tạo match giữa " + user1 + " và " + user2);
        Timestamp currentTimestamp = Timestamp.now(); // Sử dụng Timestamp từ Firebase

        // Dữ liệu match cho user1
        Map<String, Object> matchDataUser1 = new HashMap<>();
        matchDataUser1.put("timestamp", currentTimestamp);
        matchDataUser1.put("matchedWith", user2);
        matchDataUser1.put("lastMessage", ""); // Khởi tạo tin nhắn cuối cùng
        matchDataUser1.put("lastMessageTime", currentTimestamp); // Thời gian tin nhắn cuối cùng

        db.collection("matches")
                .document(user1)
                .collection("matchedWith")
                .document(user2)
                .set(matchDataUser1)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã tạo match cho user1"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tạo match cho user1: " + e.getMessage()));

        // Dữ liệu match cho user2
        Map<String, Object> matchDataUser2 = new HashMap<>();
        matchDataUser2.put("timestamp", currentTimestamp);
        matchDataUser2.put("matchedWith", user1);
        matchDataUser2.put("lastMessage", ""); // Khởi tạo tin nhắn cuối cùng
        matchDataUser2.put("lastMessageTime", currentTimestamp); // Thời gian tin nhắn cuối cùng

        db.collection("matches")
                .document(user2)
                .collection("matchedWith")
                .document(user1)
                .set(matchDataUser2)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã tạo match cho user2"))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi tạo match cho user2: " + e.getMessage()));

        Toast.makeText(this, "🎉 Bạn và người kia đã match!", Toast.LENGTH_LONG).show();

        // ⭐ QUAN TRỌNG: Gửi broadcast để MainActivity biết và chuyển tab ⭐
        // Sử dụng LocalBroadcastManager để gửi broadcast nội bộ ứng dụng
        Intent intent = new Intent(ACTION_NAVIGATE_TO_CHAT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        finish(); // Kết thúc ProfileDetailActivity sau khi match được xử lý
    }
}