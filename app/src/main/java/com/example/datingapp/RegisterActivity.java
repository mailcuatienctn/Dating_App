package com.example.datingapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
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
import com.google.firebase.firestore.QuerySnapshot; // Cần import này

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    EditText editPhone, editPassword, editCheckPassword, editOTP;
    Button btnRegister, btnVerifyOTP;
    LinearLayout otpLayout;
    TextView textToLogin;
    ImageView showPassword, showCheckPassword;
    // Biến trạng thái để theo dõi mật khẩu đang hiển thị hay ẩn
    private boolean isPasswordVisible = false;
    private boolean isCheckPasswordVisible = false;
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
        textToLogin = findViewById(R.id.textToLogin);
        showPassword = findViewById(R.id.showPassword);
        showCheckPassword = findViewById(R.id.showCheckPassword);

        showPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(editPassword, showPassword, !isPasswordVisible);
                isPasswordVisible = !isPasswordVisible; // Cập nhật trạng thái
            }
        });

        showCheckPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(editCheckPassword, showCheckPassword, !isCheckPasswordVisible);
                isCheckPasswordVisible = !isCheckPasswordVisible; // Cập nhật trạng thái
            }
        });


        textToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

        // Xử lý nút đăng ký
        btnRegister.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String checkPassword = editCheckPassword.getText().toString().trim();

            if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty() || !password.equals(checkPassword)) {
                Toast.makeText(this, "Mật khẩu không khớp hoặc rỗng", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isStrongPassword(password)) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_LONG).show();
                return;
            }

            // ⭐ Bắt đầu kiểm tra số điện thoại đã tồn tại hay chưa ⭐
            checkPhoneNumberExists(phone, password);
        });

        // Xử lý xác thực OTP và đăng ký
        btnVerifyOTP.setOnClickListener(v -> {
            String otp = editOTP.getText().toString().trim();
            if (otp.isEmpty() || verificationId == null) {
                Toast.makeText(this, "Vui lòng nhập OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            String password = editPassword.getText().toString().trim();
            String hashedPassword = hashPassword(password);

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        String phone = editPhone.getText().toString().trim();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", uid);
                        userData.put("phone", phone);
                        userData.put("password", hashedPassword);

                        Map<String, Object> profileData = new HashMap<>();
                        profileData.put("uid", uid);
                        profileData.put("name", phone); // Vẫn lưu SỐ ĐIỆN THOẠI làm tên người dùng
                        profileData.put("gender", "");
                        profileData.put("bio", "");
                        profileData.put("avatarUrl", "");
                        profileData.put("photos", new ArrayList<String>());
                        profileData.put("interests", new ArrayList<String>());

                        // Lấy vị trí cuối cùng được biết
                        if (ActivityCompat.checkSelfPermission(RegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(RegisterActivity.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location cellLocation) { // Đổi tên biến để tránh trùng
                                            if (cellLocation != null) {
                                                profileData.put("latitude", cellLocation.getLatitude());
                                                profileData.put("longitude", cellLocation.getLongitude());
                                            }
                                            saveUserAndProfileData(uid, userData, profileData);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Location", "Failed to get last location: " + e.getMessage());
                                        Toast.makeText(RegisterActivity.this, "Không lấy được vị trí. Hồ sơ được lưu mà không có vị trí.", Toast.LENGTH_SHORT).show();
                                        saveUserAndProfileData(uid, userData, profileData);
                                    });
                        } else {
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

    // ⭐ Phương thức mới: Kiểm tra số điện thoại đã tồn tại chưa ⭐
    private void checkPhoneNumberExists(String phone, String password) {
        db.collection("users") // Truy vấn collection "users"
                .whereEqualTo("phone", phone) // Kiểm tra trường "phone"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // Số điện thoại đã tồn tại
                            Toast.makeText(RegisterActivity.this, "Số điện thoại này đã được đăng ký.", Toast.LENGTH_LONG).show();
                        } else {
                            // Số điện thoại chưa tồn tại, tiến hành gửi OTP
                            sendOtpForRegistration(phone);
                        }
                    } else {
                        // Lỗi khi truy vấn Firestore
                        Toast.makeText(RegisterActivity.this, "Lỗi kiểm tra số điện thoại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("Firestore", "Error checking phone number", task.getException());
                    }
                });
    }

    private void sendOtpForRegistration(String phone) {
        otpLayout.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber("+84" + phone.substring(1))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Tự động xác minh thành công (nếu có)
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "Gửi OTP thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                        otpLayout.setVisibility(View.GONE);
                        Log.e("PhoneAuth", "Verification failed", e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                        Toast.makeText(RegisterActivity.this, "OTP đã được gửi!", Toast.LENGTH_SHORT).show();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void saveUserAndProfileData(String uid, Map<String, Object> userData, Map<String, Object> profileData) {
        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(unusedUser -> {
                    db.collection("profiles").document(uid).set(profileData)
                            .addOnSuccessListener(unusedProfile -> {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Lỗi lưu hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private boolean isStrongPassword(String password) {
        return password.length() >= 8;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền vị trí.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền vị trí để hoạt động! Một số tính năng sẽ bị hạn chế.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleImageView, boolean showPassword) {
        if (showPassword) {
            // Hiển thị mật khẩu: chuyển InputType từ password sang text
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleImageView.setImageResource(R.drawable.ic_hide_password); // ⭐ Đổi icon thành mắt gạch ⭐
        } else {
            // Ẩn mật khẩu: chuyển InputType từ text sang password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleImageView.setImageResource(R.drawable.ic_show_password); // ⭐ Đổi icon thành mắt mở ⭐
        }
        // Di chuyển con trỏ về cuối văn bản sau khi thay đổi InputType
        editText.setSelection(editText.getText().length());
    }

}