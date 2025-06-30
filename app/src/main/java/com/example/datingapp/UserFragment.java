package com.example.datingapp;

import android.content.Context;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ImageView imageAvatar;
    private TextView textName, textAgeGender, textLocation, textBio, textHobbies, textHeight;
    private RecyclerView recyclerPhotos;
    private PhotoAdapter photoAdapter;
    private List<String> photoList; // Khai báo biến
    private Button btn_edit_profile;

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

        // Cấu hình RecyclerView cho ảnh (cuộn dọc)
        recyclerPhotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        // --- SỬA ĐỔI QUAN TRỌNG Ở ĐÂY: KHỞI TẠO photoList TRƯỚC KHI TRUYỀN VÀO ADAPTER ---
        photoList = new ArrayList<>(); // Khởi tạo photoList thành một ArrayList rỗng
        photoAdapter = new PhotoAdapter(requireContext(), photoList); // Truyền photoList đã được khởi tạo
        // --- KẾT THÚC SỬA ĐỔI ---

        recyclerPhotos.setAdapter(photoAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Listen for edit profile button click
        btn_edit_profile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateProfileActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load user profile data every time the Fragment becomes visible
        loadUserProfileData();
    }


    private void loadUserProfileData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        if (uid != null) {
            db.collection("profiles").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // ✅ Convert DocumentSnapshot -> User object
                            User user = documentSnapshot.toObject(User.class);

                            if (user == null) {
                                Toast.makeText(requireContext(), "Lỗi: Hồ sơ không hợp lệ", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // ✅ Hiển thị dữ liệu từ đối tượng User
                            textName.setText(!TextUtils.isEmpty(user.getName()) ? user.getName() : "Chưa cập nhật tên");
                            textHeight.setText("" + user.getHeight() + "cm");
                            if (user.getAge() > 1900 && !TextUtils.isEmpty(user.getGender())) {
                                int age = Calendar.getInstance().get(Calendar.YEAR) - user.getAge();
                                textAgeGender.setText(age + " tuổi • " + user.getGender());
                            } else {
                                textAgeGender.setText("Chưa cập nhật tuổi/giới tính");
                            }

                            textBio.setText(!TextUtils.isEmpty(user.getBio()) ? user.getBio() : "Chưa có mô tả bản thân");

                            // Hiển thị sở thích
                            List<String> favorites = user.getFavorites();
                            if (favorites != null && !favorites.isEmpty()) {
                                textHobbies.setText(TextUtils.join(", ", favorites));
                            } else {
                                textHobbies.setText("Chưa có sở thích nào.");
                            }

                            // Hiển thị vị trí
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

                            // Hiển thị ảnh
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

}