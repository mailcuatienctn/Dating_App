package com.example.datingapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    private EditText editPhone, editPassword;
    private Button btnLogin;
    private TextView textToRegister, textForgotPassword;
    private ImageView showPassword;
    private boolean isPasswordVisible = false;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textToRegister = findViewById(R.id.textToLogin);
        showPassword = findViewById(R.id.showPassword);
        textForgotPassword = findViewById(R.id.textForgotPassword);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng nhập, vui lòng đợi...");
        progressDialog.setCancelable(false);

        loadLoginCredentials();

        showPassword.setOnClickListener(v -> {
            togglePasswordVisibility(editPassword, showPassword, !isPasswordVisible);
            isPasswordVisible = !isPasswordVisible;
        });

        textToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        textForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String uid = prefs.getString("uid", null);
        if (uid != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void attemptLogin() {
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            Toast.makeText(this, "Lỗi khi xử lý mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String storedHashedPassword = document.getString("password");
                        String uid = document.getString("uid");

                        if (hashedPassword.equals(storedHashedPassword)) {
                            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            saveLoginCredentials(phone, password);

                            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            prefs.edit().putString("uid", uid).apply();
                            Log.d("Login", "UID người dùng đã lưu: " + uid);

                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(tokenTask -> {
                                        if (tokenTask.isSuccessful()) {
                                            String token = tokenTask.getResult();
                                            if (token != null && !token.isEmpty()) {
                                                db.collection("profiles").document(uid)
                                                        .update("fcmToken", token)
                                                        .addOnSuccessListener(aVoid -> Log.d("FCM", "FCM token đã lưu vào Firestore: " + token))
                                                        .addOnFailureListener(e -> Log.e("FCM", "Lỗi lưu token: ", e));
                                            }
                                        } else {
                                            Log.e("FCM", "Không lấy được FCM token", tokenTask.getException());
                                        }
                                    });

                            progressDialog.show();
                            db.collection("profiles").document(uid).get()
                                    .addOnSuccessListener(profileDoc -> {
                                        progressDialog.dismiss();
                                        String gender = profileDoc.getString("gender");
                                        Intent intent;
                                        if (gender != null && !gender.isEmpty()) {
                                            intent = new Intent(LoginActivity.this, MainActivity.class);
                                        } else {
                                            intent = new Intent(LoginActivity.this, CreateProfileActivity.class);
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Log.e("Login", "Lỗi kiểm tra hồ sơ", e);
                                        Toast.makeText(LoginActivity.this, "Lỗi kiểm tra hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "Mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Số điện thoại không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Lỗi truy vấn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Login", "Lỗi Firestore", e);
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

    private void loadLoginCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String phone = sharedPreferences.getString(KEY_PHONE, "");
            String password = sharedPreferences.getString(KEY_PASSWORD, "");
            editPhone.setText(phone);
            editPassword.setText(password);
            Log.d("Login", "Đã tự động điền thông tin đăng nhập.");
        }
    }

    private void saveLoginCredentials(String phone, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.apply();
        Log.d("Login", "Đã lưu thông tin đăng nhập.");
    }
}
