package com.example.datingapp;

import android.content.Context; // Thêm import này
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.firebase.auth.FirebaseAuth; // Thêm import Firebase Auth
import com.google.firebase.firestore.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    EditText editPhone, editPassword;
    Button btnLogin;
    TextView textToLogin;

    FirebaseFirestore db;
    // Khai báo FirebaseAuth để quản lý người dùng đăng nhập
    FirebaseAuth mAuth; // Thêm dòng này

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editPhone = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textToLogin = findViewById(R.id.textToLogin);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // Khởi tạo FirebaseAuth

        Typeface customFont = ResourcesCompat.getFont(this, R.font.uvn);

        // Điều hướng đến đăng ký nếu chưa có tài khoản
        textToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Xử lý nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            String hashedPassword = hashPassword(password);

            db.collection("users")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String storedHashedPassword = document.getString("password");
                            String uid = document.getString("uid"); // Lấy UID ngay từ đây

                            if (hashedPassword.equals(storedHashedPassword)) {
                                // Đăng nhập thành công
                                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                                // --- Bắt đầu quan trọng: Xử lý UID và chuyển hướng ---

                                // 1. Lưu UID mới vào SharedPreferences
                                // Luôn đảm bảo UID của người dùng hiện tại được lưu chính xác
                                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                prefs.edit().putString("uid", uid).apply();
                                Log.d("Login", "UID mới đã được lưu vào SharedPreferences: " + uid);

                                // 2. (Tùy chọn nhưng nên làm) Đăng nhập Firebase Auth (nếu bạn chưa làm trong RegisterActivity)
                                // Nếu bạn đang dùng Firebase Auth để quản lý trạng thái đăng nhập,
                                // bạn sẽ cần phải đăng nhập người dùng vào Auth system của Firebase.
                                // Tuy nhiên, vì bạn đang quản lý bằng sđt/mật khẩu và tự hash,
                                // phần này có thể không cần thiết nếu bạn chỉ dùng Firestore cho data.
                                // Nhưng nếu bạn muốn getCurrentUser() của Firebase Auth trả về user này, bạn cần làm điều đó.
                                // Để đơn giản cho vấn đề hiện tại, chúng ta tập trung vào UID từ Firestore.

                                // 3. Kiểm tra hồ sơ trong Firestore
                                db.collection("profiles").document(uid).get()
                                        .addOnSuccessListener(profileDoc -> {
                                            String gender = profileDoc.getString("gender");

                                            Log.d("check ho so", "Giới tính từ hồ sơ: " + gender);

                                            if (gender != null && !gender.isEmpty()) {
                                                // Người dùng đã có hồ sơ -> chuyển đến MainActivity
                                                Log.d("check ho so", "Có hồ sơ. Chuyển đến MainActivity.");
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                // Xóa tất cả các activity cũ trên stack, đảm bảo MainActivity là activity gốc mới
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            } else {
                                                // Người dùng mới hoặc hồ sơ chưa hoàn chỉnh -> chuyển đến CreateProfileActivity
                                                Log.d("check ho so", "Không có hồ sơ hoặc hồ sơ chưa hoàn chỉnh. Chuyển đến CreateProfileActivity.");
                                                Intent intent = new Intent(LoginActivity.this, CreateProfileActivity.class);
                                                // Không cần truyền UID qua Intent nữa vì CreateProfileActivity sẽ tự lấy từ SharedPreferences/Firebase Auth
                                                // intent.putExtra("uid", uid);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Đảm bảo CreateProfileActivity là activity gốc mới
                                                startActivity(intent);
                                            }
                                            // Gọi finish() sau khi chuyển hướng và làm sạch stack
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Xử lý lỗi khi kiểm tra hồ sơ
                                            Log.e("Login", "Lỗi khi kiểm tra hồ sơ: ", e);
                                            Toast.makeText(LoginActivity.this, "Lỗi khi kiểm tra hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            // Trong trường hợp lỗi kiểm tra hồ sơ, vẫn có thể chuyển hướng để tránh kẹt
                                            // Hoặc xử lý lỗi cụ thể hơn nếu cần.
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Vẫn chuyển về MainActivity như một fallback
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();
                                        });

                                // --- Kết thúc quan trọng ---

                            } else {
                                Toast.makeText(LoginActivity.this, "Mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Login", "Error", e);
                    });
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}