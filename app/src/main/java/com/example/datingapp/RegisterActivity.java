package com.example.datingapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.location.*;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnSuccessListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    EditText editPhone, editPassword, editCheckPassword, editOTP;
    Button btnRegister, btnVerifyOTP;
    LinearLayout otpLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;

    private FusedLocationProviderClient fusedLocationClient;
    private final int LOCATION_PERMISSION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Yêu cầu quyền vị trí (nếu chưa được cấp)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }

        // Ánh xạ view
        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        editCheckPassword = findViewById(R.id.editCheckPassword);
        editOTP = findViewById(R.id.editOTP);
        btnRegister = findViewById(R.id.btnRegister);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        otpLayout = findViewById(R.id.otpLayout);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        Typeface customFont = ResourcesCompat.getFont(this, R.font.uvn);
        // Duyệt qua các view con của toolbar để tìm TextView và áp dụng font
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View view = toolbar.getChildAt(i);
            if (view instanceof TextView) {
                ((TextView) view).setTypeface(customFont);
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Xử lý nút đăng ký
        btnRegister.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            String password = editPassword.getText().toString().trim();
            String checkPassword = editCheckPassword.getText().toString().trim();

            if (password.isEmpty() || !password.equals(checkPassword)) {
                Toast.makeText(this, "Mật khẩu không khớp hoặc rỗng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isStrongPassword(password)) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_LONG).show();
                return;
            }

            otpLayout.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);

            // Gửi OTP
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber("+84" + phone.substring(1)) // Định dạng số điện thoại chuẩn quốc tế
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            // Tự động xác minh thành công (nếu có)
                            // Bạn có thể đăng nhập người dùng ngay tại đây nếu muốn
                            // signInWithCredential(credential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Toast.makeText(RegisterActivity.this, "Gửi OTP thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnRegister.setEnabled(true);
                            otpLayout.setVisibility(View.GONE); // Ẩn trường OTP nếu gửi thất bại
                            Log.e("PhoneAuth", "Verification failed", e);
                        }

                        @Override
                        public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            verificationId = vId;
                            Toast.makeText(RegisterActivity.this, "OTP đã được gửi!", Toast.LENGTH_SHORT).show();
                        }
                    }).build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        });

        // Xử lý xác thực OTP và đăng ký
        btnVerifyOTP.setOnClickListener(v -> {
            String otp = editOTP.getText().toString().trim();
            if (otp.isEmpty() || verificationId == null) {
                Toast.makeText(this, "Vui lòng nhập OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            String password = editPassword.getText().toString().trim(); // Lấy mật khẩu người dùng nhập để băm
            String hashedPassword = hashPassword(password); // Băm mật khẩu

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        String phone = editPhone.getText().toString().trim();

                        // --- Giữ lại việc lưu mật khẩu đã băm vào Firestore ---
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", uid);
                        userData.put("phone", phone);
                        userData.put("password", hashedPassword); // LƯU MẬT KHẨU ĐÃ BĂM VÀO COLLECTION "users"

                        Map<String, Object> profileData = new HashMap<>();
                        profileData.put("uid", uid);
                        profileData.put("name", "");
                        profileData.put("gender", "");
                        profileData.put("bio", "");
                        profileData.put("avatarUrl", ""); // Ảnh đại diện chính nếu có
                        profileData.put("photos", new ArrayList<String>()); // Danh sách các URL ảnh
                        profileData.put("interests", new ArrayList<String>()); // Danh sách sở thích

                        // Lấy vị trí cuối cùng được biết
                        // Kiểm tra quyền vị trí trước khi yêu cầu
                        if (ActivityCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(RegisterActivity.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                            if (location != null) {
                                                profileData.put("latitude", location.getLatitude());
                                                profileData.put("longitude", location.getLongitude());
                                            }
                                            // Dù có vị trí hay không, vẫn tiến hành lưu dữ liệu
                                            saveUserAndProfileData(uid, userData, profileData);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Location", "Failed to get last location: " + e.getMessage());
                                        // Nếu không lấy được vị trí, vẫn lưu dữ liệu mà không có vị trí
                                        Toast.makeText(RegisterActivity.this, "Không lấy được vị trí. Hồ sơ được lưu mà không có vị trí.", Toast.LENGTH_SHORT).show();
                                        saveUserAndProfileData(uid, userData, profileData);
                                    });
                        } else {
                            // Nếu không có quyền vị trí (dù đã yêu cầu ở onCreate, có thể người dùng từ chối)
                            Toast.makeText(RegisterActivity.this, "Không có quyền vị trí. Hồ sơ sẽ được lưu mà không có vị trí.", Toast.LENGTH_LONG).show();
                            saveUserAndProfileData(uid, userData, profileData);
                        }
                    }
                } else {
                    Toast.makeText(this, "Xác minh OTP thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Auth", "OTP verification failed", task.getException());
                }
            });
        });
    }

    // Phương thức chung để lưu dữ liệu người dùng và hồ sơ vào Firestore
    private void saveUserAndProfileData(String uid, Map<String, Object> userData, Map<String, Object> profileData) {
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(unusedUser -> {
                    db.collection("profiles").document(uid).set(profileData)
                            .addOnSuccessListener(unusedProfile -> {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                // Chuyển sang LoginActivity sau khi đăng ký thành công
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish(); // Đóng RegisterActivity
                            })
                            .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Lỗi lưu hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Hàm mã hóa mật khẩu SHA-256
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

    // Kiểm tra độ mạnh mật khẩu (ít nhất 8 ký tự)
    private boolean isStrongPassword(String password) {
        return password.length() >= 8;
    }

    // Xử lý kết quả yêu cầu quyền vị trí
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền vị trí để hoạt động! Một số tính năng sẽ bị hạn chế.", Toast.LENGTH_LONG).show();
                // Tùy chọn: Bạn có thể chọn finish() activity nếu vị trí là bắt buộc
                // finish();
            }
        }
    }
}