
package com.example.datingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Address;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.datingapp.adapter.PhotoAdapter;
import com.example.datingapp.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue; // Quan trọng để xóa trường
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Map;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ImageView imageAvatar, icon_change_password, icon_pause_matching;
    private TextView textName, textAgeGender, textLocation, textBio, textHobbies, textHeight;
    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<String> photoList;
    private Button btn_edit_profile;
    private ImageView iconLogout;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    public UserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        // Ánh xạ UI components
        imageAvatar = view.findViewById(R.id.image_avatar);
        textName = view.findViewById(R.id.text_name);
        textAgeGender = view.findViewById(R.id.text_age_gender);
        textLocation = view.findViewById(R.id.text_location);
        textBio = view.findViewById(R.id.text_bio);
        textHobbies = view.findViewById(R.id.text_hobbies);
        recyclerPhotos = view.findViewById(R.id.recycler_photos);
        btn_edit_profile = view.findViewById(R.id.btn_edit_profile);
        textHeight = view.findViewById(R.id.text_height);
        iconLogout = view.findViewById(R.id.icon_logout);
        icon_change_password = view.findViewById(R.id.icon_change_password);
        icon_pause_matching = view.findViewById(R.id.icon_pause_matching);

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        photoList = new ArrayList<>();
        photoAdapter = new PhotoAdapter(requireContext(), photoList);
        recyclerPhotos.setAdapter(photoAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Xử lý sự kiện click cho icon đăng xuất
        iconLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });

        icon_change_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
                startActivity(intent);
            }
        });

        icon_pause_matching.setOnClickListener(v -> {
            showPauseMatchingDialog();
        });


        // Listen for edit profile button click
        btn_edit_profile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfileData();
    }

    private void loadUserProfileData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        if (uid != null) {
            db.collection("profiles").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);

                            if (user == null) {
                                Toast.makeText(requireContext(), "Lỗi: Hồ sơ không hợp lệ", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            textName.setText(!TextUtils.isEmpty(user.getName()) ? user.getName() : "Chưa cập nhật tên");
                            if(user.getHeight() == 0){
                                textHeight.setText("" + "Không tiết lộ");
                            }else {
                                textHeight.setText("" + user.getHeight() + "cm");
                            }
                            if (!TextUtils.isEmpty(user.getGender())) {
                                int age = user.getAge();
                                textAgeGender.setText(age + " tuổi • " + user.getGender());
                            } else {
                                textAgeGender.setText("Chưa cập nhật tuổi/giới tính");
                            }

                            textBio.setText(!TextUtils.isEmpty(user.getBio()) ? user.getBio() : "Chưa có mô tả bản thân");

                            List<String> favorites = user.getFavorites();
                            if (favorites != null && !favorites.isEmpty()) {
                                textHobbies.setText(TextUtils.join(", ", favorites));
                            } else {
                                textHobbies.setText("Chưa có sở thích nào.");
                            }

                            if (user.getLatitude() != null && user.getLongitude() != null) {
                                Geocoder geocoder = new Geocoder(requireContext(), new Locale("vi", "VN"));
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(user.getLatitude(), user.getLongitude(), 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address address = addresses.get(0);
                                        String adminArea = address.getAdminArea();
                                        String locality = address.getLocality();
                                        String country = address.getCountryName();

                                        String locationDisplay = "";
                                        if ("Vietnam".equalsIgnoreCase(country) || "Việt Nam".equalsIgnoreCase(country)) {
                                            locationDisplay = !TextUtils.isEmpty(adminArea) ? adminArea :
                                                    (!TextUtils.isEmpty(locality) ? locality : "");
                                        } else {
                                            locationDisplay = !TextUtils.isEmpty(locality) ? locality : "";
                                            if (!TextUtils.isEmpty(country)) locationDisplay += ", " + country;
                                        }

                                        textLocation.setText(!locationDisplay.isEmpty() ? locationDisplay : "Vị trí không xác định");
                                    } else {
                                        textLocation.setText(String.format(Locale.getDefault(), "Vị trí: %.4f, %.4f", user.getLatitude(), user.getLongitude()));
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Geocoder error: " + e.getMessage());
                                    textLocation.setText(String.format(Locale.getDefault(), "Vị trí: %.4f, %.4f", user.getLatitude(), user.getLongitude()));
                                }
                            } else {
                                textLocation.setText("Vị trí chưa cập nhật");
                            }


                            if (user.getLatitude() != null && user.getLongitude() != null) {
                                Geocoder geocoder = new Geocoder(requireContext(), new Locale("vi", "VN"));
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(user.getLatitude(), user.getLongitude(), 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address address = addresses.get(0);
                                        String adminArea = address.getAdminArea();
                                        String locality = address.getLocality();
                                        String country = address.getCountryName();

                                        String locationDisplay = "";
                                        if ("Vietnam".equalsIgnoreCase(country) || "Việt Nam".equalsIgnoreCase(country)) {
                                            locationDisplay = !TextUtils.isEmpty(adminArea) ? adminArea :
                                                    (!TextUtils.isEmpty(locality) ? locality : "");
                                        } else {
                                            locationDisplay = !TextUtils.isEmpty(locality) ? locality : "";
                                            if (!TextUtils.isEmpty(country)) locationDisplay += ", " + country;
                                        }

                                        textLocation.setText(!locationDisplay.isEmpty() ? locationDisplay : "Vị trí không xác định");

                                        // Lưu province nếu chưa có hoặc khác
                                        String provinceName = !TextUtils.isEmpty(adminArea) ? adminArea :
                                                (!TextUtils.isEmpty(locality) ? locality : null);

                                        if (!TextUtils.isEmpty(provinceName) &&
                                                (TextUtils.isEmpty(user.getProvince()) || !provinceName.equals(user.getProvince()))) {

                                            Map<String, Object> updates = new HashMap<>();
                                            updates.put("province", provinceName);

                                            db.collection("profiles").document(uid)
                                                    .update(updates)
                                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Province updated to Firestore: " + provinceName))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating province: " + e.getMessage()));
                                        }

                                    } else {
                                        textLocation.setText(String.format(Locale.getDefault(), "Vị trí: %.4f, %.4f", user.getLatitude(), user.getLongitude()));
                                    }
                                } catch (IOException e) {
                                    Log.e(TAG, "Geocoder error: " + e.getMessage());
                                    textLocation.setText(String.format(Locale.getDefault(), "Vị trí: %.4f, %.4f", user.getLatitude(), user.getLongitude()));
                                }
                            }




                            List<String> imgUrls = user.getImgUrls();
                            if (imgUrls != null && !imgUrls.isEmpty()) {
                                Glide.with(requireContext()).load(imgUrls.get(0)).into(imageAvatar);
                                photoList.clear();
                                photoList.addAll(imgUrls);
                                photoAdapter.notifyDataSetChanged();
                            } else {
                                imageAvatar.setImageResource(R.drawable.bg_avatar_circle);
                                photoList.clear();
                                photoAdapter.notifyDataSetChanged();
                            }

                        } else {
                            Toast.makeText(requireContext(), "Hồ sơ chưa được tạo. Vui lòng tạo hồ sơ.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi khi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "loadUserProfileData: ", e);
                    });
        } else {
            Toast.makeText(requireContext(), "Bạn chưa đăng nhập. Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    // Hàm hiển thị hộp thoại xác nhận đăng xuất
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn lưu tài khoản và mật khẩu cho lần đăng nhập tiếp theo không?")
                .setPositiveButton("Lưu và Đăng xuất", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        performLogout(true);
                    }
                })
                .setNegativeButton("Đăng xuất (Không lưu)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        performLogout(false);
                    }
                })
                .setNeutralButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showPauseMatchingDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Tạm dừng ghép đôi")
                .setMessage("Bạn có muốn tạm dừng ghép đôi không? Hồ sơ của bạn sẽ không xuất hiện cho người khác cho đến khi bạn bật lại.")
                .setPositiveButton("Tạm dừng", (dialog, which) -> {
                    updatePauseMatchingStatus(false);
                })
                .setNegativeButton("Bỏ tạm dừng", (dialog, which) -> {
                    updatePauseMatchingStatus(true);
                })
                .setNeutralButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_pause_matching)
                .show();
    }

    private void updatePauseMatchingStatus(boolean pause) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("uid", null);
        db.collection("profiles").document(uid)
                .update("state", pause)
                .addOnSuccessListener(aVoid -> {
                    if (pause) {
                        Toast.makeText(requireContext(), "Bạn đã bật lại ghép đôi.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Bạn đã tạm dừng ghép đôi.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "updatePauseMatchingStatus: ", e);
                });
    }

    private void performLogout(boolean rememberMe) {
        if (!rememberMe) {
            clearLoginCredentials(); // Xóa phone, password, rememberMe nếu người dùng KHÔNG MUỐN LƯU
        }

        logoutUserFromFirestore();
        FirebaseAuth.getInstance().signOut();

        // LUÔN XÓA UID ĐỂ NGĂN AUTO-LOGIN SAU LOGOUT, nhưng KHÔNG XÓA PHONE, PASSWORD
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        prefs.edit().remove("uid").apply();
        Log.d("Logout", "Đã xóa uid, tránh auto-login, nhưng giữ lại phone/password nếu cần.");

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();

        Toast.makeText(getContext(), "Bạn đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
    }

    // Hàm để xóa thông tin đăng nhập từ SharedPreferences
    private void clearLoginCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_PHONE);
        editor.remove(KEY_PASSWORD);
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.apply();
        Log.d(TAG, "Thông tin đăng nhập đã được xóa khỏi SharedPreferences.");
    }

    // HÀM CHỈ CÓ NHIỆM VỤ XÓA FCM TOKEN TRÊN FIRESTORE (ĐÃ ĐỔI TÊN VÀ SỬA)
    public void logoutUserFromFirestore() {
        if (mAuth == null) {
            mAuth = FirebaseAuth.getInstance();
        }
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }

        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (currentUid != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", FieldValue.delete()); // Xóa trường fcmToken khỏi document

            db.collection("profiles")
                    .document(currentUid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Logout", "FCM Token cleared from Firestore for " + currentUid);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Logout", "Error clearing FCM token from Firestore: " + e.getMessage());
                    });
        }
    }
}