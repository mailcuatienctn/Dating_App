package com.example.datingapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private boolean isPasswordVisible = false;
    private boolean isCheckPasswordVisible = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final int GPS_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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

        showPassword.setOnClickListener(v -> togglePasswordVisibility(editPassword, showPassword, !isPasswordVisible));
        showCheckPassword.setOnClickListener(v -> togglePasswordVisibility(editCheckPassword, showCheckPassword, !isCheckPasswordVisible));
        textToLogin.setOnClickListener(v -> startActivity(new android.content.Intent(RegisterActivity.this, LoginActivity.class)));

        btnRegister.setOnClickListener(v -> handleRegister());
        btnVerifyOTP.setOnClickListener(v -> verifyOtpAndRegister());

        checkAndRequestLocation();
    }

    private void checkAndRequestLocation() {
        LocationRequest locationRequest = LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> getCurrentLocation());
        task.addOnFailureListener(e -> {
            if (e instanceof  ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(RegisterActivity.this, GPS_REQUEST_CODE);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocation = location;
                    Log.d("LocationDebug", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                } else {
                    Log.d("LocationDebug", "Location is null");
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    private void handleRegister() {
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String checkPassword = editCheckPassword.getText().toString().trim();

        if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
            Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty() || !password.equals(checkPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_LONG).show();
            return;
        }
        if (password.length() > 20) {
            Toast.makeText(this, "Mật khẩu không được vượt quá 20 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        checkPhoneNumberExists(phone);
    }

    private void verifyOtpAndRegister() {
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
                    profileData.put("name", "");
                    profileData.put("gender", "");
                    profileData.put("bio", "");
                    profileData.put("avatarUrl", "");
                    profileData.put("photos", new ArrayList<String>());
                    profileData.put("interests", new ArrayList<String>());

                    if (currentLocation != null) {
                        profileData.put("latitude", currentLocation.getLatitude());
                        profileData.put("longitude", currentLocation.getLongitude());
                    }

                    saveUserAndProfileData(uid, userData, profileData);
                }
            } else {
                Toast.makeText(this, "Xác minh OTP thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPhoneNumberExists(String phone) {
        db.collection("users").whereEqualTo("phone", phone).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null && !task.getResult().isEmpty()) {
                    Toast.makeText(this, "Số điện thoại đã tồn tại", Toast.LENGTH_SHORT).show();
                } else {
                    sendOtp(phone);
                }
            } else {
                Toast.makeText(this, "Lỗi kiểm tra số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOtp(String phone) {
        otpLayout.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber("+84" + phone.substring(1))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "Gửi OTP thất bại", Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                        otpLayout.setVisibility(View.GONE);
                    }
                    @Override
                    public void onCodeSent(@NonNull String vId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = vId;
                        Toast.makeText(RegisterActivity.this, "OTP đã được gửi", Toast.LENGTH_SHORT).show();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void saveUserAndProfileData(String uid, Map<String, Object> userData, Map<String, Object> profileData) {
        db.collection("users").document(uid).set(userData).addOnSuccessListener(unused -> {
            db.collection("profiles").document(uid).set(profileData).addOnSuccessListener(unused1 -> {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class);
                intent.putExtra("latitude", currentLocation != null ? currentLocation.getLatitude() : 0.0);
                intent.putExtra("longitude", currentLocation != null ? currentLocation.getLongitude() : 0.0);
                startActivity(intent);
                finish();
            }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu hồ sơ", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi lưu dữ liệu người dùng", Toast.LENGTH_SHORT).show());
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
            return null;
        }
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleImageView, boolean showPassword) {
        if (showPassword) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleImageView.setImageResource(R.drawable.ic_hide_password);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleImageView.setImageResource(R.drawable.ic_show_password);
        }
        editText.setSelection(editText.getText().length());
    }
}