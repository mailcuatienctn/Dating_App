package com.example.datingapp;

import android.content.Context;
import android.content.SharedPreferences; // Import này giờ là tùy chọn nếu không dùng UID từ đây
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // Giữ nguyên RecyclerView theo mã của bạn

// Các import cho EdgeToEdge (Đảm bảo đã kích hoạt trong Gradle)
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.datingapp.adapter.UserCardAdapter; // Adapter tùy chỉnh của bạn
import com.example.datingapp.model.User; // Model User của bạn
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryActivity extends AppCompatActivity {

    private static final String TAG = "DiscoveryActivity"; // Để ghi log

    private RecyclerView cardStackView; // Vẫn đặt tên là cardStackView nhưng là RecyclerView
    private UserCardAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId; // UID của người dùng hiện tại đang đăng nhập

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kích hoạt EdgeToEdge để hiển thị nội dung toàn màn hình (Đảm bảo ID root layout là 'main')
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discovery); // Đảm bảo layout này tồn tại

        // Áp dụng insets cửa sổ cho view chính để xử lý EdgeToEdge
        // Đảm bảo ConstraintLayout hoặc layout gốc của bạn trong activity_discovery.xml có ID là 'main'
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Khởi tạo các instance Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance(); // Lấy instance FirebaseAuth

        // Lấy UID của người dùng hiện tại
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            // Xử lý trường hợp người dùng chưa đăng nhập
            Toast.makeText(this, "Bạn cần đăng nhập để xem hồ sơ.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Người dùng hiện tại là null. Đang chuyển hướng hoặc đóng Activity.");
            finish(); // Đóng activity nếu không có người dùng
            return; // Thoát khỏi onCreate
        }

        // Khởi tạo RecyclerView và Adapter
        cardStackView = findViewById(R.id.card_stack_view); // Đảm bảo ID này có trong activity_discovery.xml
        adapter = new UserCardAdapter(this, userList); // Truyền danh sách rỗng ban đầu và context
        cardStackView.setAdapter(adapter);

        // Tải tất cả hồ sơ từ Firestore
        loadAllProfiles();
    }

    /**
     * Tải TẤT CẢ hồ sơ từ collection Firestore "profiles".
     * Phiên bản này KHÔNG lọc ra hồ sơ của người dùng hiện tại
     * hoặc các hồ sơ đã tương tác trước đó.
     */
    private void loadAllProfiles() {
        if (currentUserId == null) {
            Log.e(TAG, "currentUserId là null, không thể tải hồ sơ. Điều này không nên xảy ra nếu onCreate đã xử lý.");
            return;
        }

        db.collection("profiles") // Đảm bảo đây là tên collection chính xác của bạn
                .get() // Lấy TẤT CẢ tài liệu
                .addOnSuccessListener(querySnapshots -> {
                    List<User> newProfiles = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshots) {
                        Log.d("FirebaseData", "Đang xử lý ID tài liệu: " + document.getId()); // Ghi log ID tài liệu
                        User profile = document.toObject(User.class); // Chuyển đổi tài liệu thành đối tượng User

                        if (profile != null) {
                            // ⭐ QUAN TRỌNG: Gán thủ công UID từ ID tài liệu Firestore
                            profile.setUid(document.getId());
                            Log.d("FirebaseData", "Đã tải hồ sơ: " + profile.getName() + ", UID: " + profile.getUid());
                            newProfiles.add(profile);
                        } else {
                            Log.w("FirebaseData", "Tài liệu " + document.getId() + " không thể chuyển đổi thành đối tượng User.");
                        }
                    }

                    userList.clear(); // Xóa danh sách hiện có
                    userList.addAll(newProfiles); // Thêm các hồ sơ mới tải về
                    adapter.notifyDataSetChanged(); // Thông báo cho adapter để làm mới UI

                    if (userList.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy hồ sơ nào để hiển thị.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Không tìm thấy hồ sơ nào trong collection 'profiles'.");
                    } else {
                        Log.d(TAG, "Đã tải thành công " + userList.size() + " hồ sơ.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải hồ sơ: " + e.getMessage(), e); // Ghi log exception
                    Toast.makeText(this, "Lỗi khi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Bạn sẽ cần thêm nhiều phương thức ở đây nếu muốn thêm
    // các tính năng như vuốt, nút thích/không thích, hoặc xem hồ sơ chi tiết.
    // Thiết lập hiện tại chỉ tải và hiển thị.
}