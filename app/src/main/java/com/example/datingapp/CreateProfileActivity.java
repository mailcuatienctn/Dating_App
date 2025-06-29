package com.example.datingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.datingapp.adapter.SelectedImagesAdapter;
import com.example.datingapp.model.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gun0912.tedimagepicker.builder.TedImagePicker;

public class CreateProfileActivity extends AppCompatActivity {

    private EditText editName, editBio;
    private RadioGroup radioGender;
    private RecyclerView recyclerView;
    private SelectedImagesAdapter adapter;
    private List<Uri> selectedUris; // Holds UIs selected from gallery
    private Button btnAddImg;
    private Button btnFavorite;
    private Button btnSave;
    private Spinner spinnerYearOfBirth;
    private List<String> finalImageUrlsToSave = new ArrayList<>(); // This will hold the URLs to be saved to Firestore
    private ChipGroup chipGroupFavorites;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private Double storedLatitude = null;
    private Double storedLongitude = null;

    private static final String TAG = "CreateProfileActivity";
    private static final String ERROR_UPLOAD_IMAGE = "Lỗi tải ảnh: ";
    private static final String MSG_PLEASE_SELECT_IMAGES = "Vui lòng chọn ảnh trước khi lưu hồ sơ.";
    private static final String MSG_UPLOADING_IMAGES = "Đang tải ảnh lên Cloudinary...";
    private static final String MSG_UPLOAD_SUCCESS = "Tải ảnh lên thành công!";
    private static final String MSG_SAVE_PROFILE_SUCCESS = "Lưu hồ sơ thành công!";
    private static final String MSG_SAVE_PROFILE_ERROR = "Lỗi khi lưu hồ sơ: ";
    private static final String MSG_NOT_LOGGED_IN = "Bạn cần đăng nhập để lưu hồ sơ.";
    private static final String DIALOG_TITLE_FAVORITES = "Chọn sở thích";
    private static final String BUTTON_OK = "OK";
    private static final String BUTTON_CANCEL = "Hủy";
    private static final int MAX_IMAGES = 6;
    private static final String MSG_MAX_IMAGES = "Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh";


    private String[] availableFavorites = {
            "Du lịch", "Âm nhạc", "Thể thao",
            "Ẩm thực", "Chơi game", "Nấu ăn"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        // Ánh xạ các thành phần UI
        recyclerView = findViewById(R.id.recyclerSelectedImages);
        btnAddImg = findViewById(R.id.btnAddImg);
        btnFavorite = findViewById(R.id.btnFavorite);
        chipGroupFavorites = findViewById(R.id.chipGroupFavorites);
        btnSave = findViewById(R.id.btnSave);
        radioGender = findViewById(R.id.radioGender);
        editName = findViewById(R.id.editName);
        editBio = findViewById(R.id.editBio);
        spinnerYearOfBirth = findViewById(R.id.spinnerYearOfBirth);

        // Tạo list năm sinh từ 1950 đến 2009
        List<String> yearList = new ArrayList<>();
        for (int year = 1950; year <= 2009; year++) {
            yearList.add(String.valueOf(year));
        }

        // Tạo adapter và gán vào spinner
        ArrayAdapter<String> yearOfBirthAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                yearList);
        yearOfBirthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearOfBirth.setAdapter(yearOfBirthAdapter);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Kiểm tra người dùng đã đăng nhập chưa
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, MSG_NOT_LOGGED_IN, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        // Cấu hình RecyclerView cho ảnh đã chọn
        selectedUris = new ArrayList<>();
        adapter = new SelectedImagesAdapter(this, selectedUris);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        // TẢI DỮ LIỆU HỒ SƠ HIỆN CÓ (NẾU CÓ)
        loadProfileData();

        // Lắng nghe sự kiện click cho nút "Thêm ảnh"
        btnAddImg.setOnClickListener(v -> {
            TedImagePicker.with(this)
                    .max(MAX_IMAGES, MSG_MAX_IMAGES)
                    .startMultiImage(uriList -> {
                        selectedUris.clear(); // Clear existing Uris to replace them with new selection
                        selectedUris.addAll(uriList);
                        adapter.notifyDataSetChanged();
                    });
        });

        // Lắng nghe sự kiện click cho nút "Chọn sở thích"
        btnFavorite.setOnClickListener(v -> showFavoritesDialog());

        // Lắng nghe sự kiện click cho nút "Lưu Hồ Sơ"
        btnSave.setOnClickListener(v -> {
            // Check if user has selected new images.
            if (!selectedUris.isEmpty()) {
                uploadImagesToCloudinary();
            } else {
                // If no new images selected, use existing ones (if any) or show error
                if (finalImageUrlsToSave.isEmpty()) { // No new images, and no existing images
                    Toast.makeText(this, MSG_PLEASE_SELECT_IMAGES, Toast.LENGTH_SHORT).show();
                } else { // No new images selected, but there are existing ones (from loadProfileData)
                    saveProfileToFirestore(); // Proceed to save with current finalImageUrlsToSave
                }
            }
        });
    }

    private void loadProfileData() {
        db.collection("profiles").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editName.setText(documentSnapshot.getString("name"));
                        editBio.setText(documentSnapshot.getString("bio"));

                        String gender = documentSnapshot.getString("gender");
                        if ("Nam".equals(gender)) {
                            radioGender.check(R.id.radioMale);
                        } else if ("Nữ".equals(gender)) {
                            radioGender.check(R.id.radioFemale);
                        }

                        Long yearLong = documentSnapshot.getLong("age");
                        if (yearLong != null) {
                            String yearString = String.valueOf(yearLong);
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerYearOfBirth.getAdapter();
                            if (adapter != null) {
                                int spinnerPosition = adapter.getPosition(yearString);
                                if (spinnerPosition != -1) {
                                    spinnerYearOfBirth.setSelection(spinnerPosition);
                                }
                            }
                        }

                        List<String> favorites = (List<String>) documentSnapshot.get("favorites");
                        if (favorites != null) {
                            chipGroupFavorites.removeAllViews();
                            for (String favorite : favorites) {
                                addChip(favorite);
                            }
                        }

                        // **IMPORTANT CHANGE HERE:** Populate finalImageUrlsToSave with existing URLs
                        // These are the images currently associated with the profile in Firestore
                        List<String> imgUrls = (List<String>) documentSnapshot.get("imgUrls");
                        if (imgUrls != null) {
                            finalImageUrlsToSave.clear();
                            finalImageUrlsToSave.addAll(imgUrls);
                            Log.d(TAG, "Loaded existing image URLs: " + finalImageUrlsToSave);

                            // Optional: If you want to show loaded images in the RecyclerView
                            // You'd need to convert these URLs back to Uris if your SelectedImagesAdapter
                            // only works with Uris. Or modify the adapter to handle URLs.
                            // For simplicity, we are NOT displaying old URLs in selectedUris directly
                            // unless the user explicitly re-selects them.
                        }

                        storedLatitude = documentSnapshot.getDouble("latitude");
                        storedLongitude = documentSnapshot.getDouble("longitude");

                        Log.d(TAG, "Đã tải dữ liệu hồ sơ: " + documentSnapshot.getData());
                        Log.d(TAG, "Vị trí đã lưu: Lat=" + storedLatitude + ", Lng=" + storedLongitude);

                    } else {
                        Log.d(TAG, "Không có hồ sơ hiện có cho người dùng.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải hồ sơ: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImagesToCloudinary() {
        // Create a *temporary* list for newly uploaded URLs
        // This list will only contain URLs from the current upload session.
        List<String> newUploadedUrls = new ArrayList<>();

        final int totalImages = selectedUris.size();
        final int[] uploadedCount = {0}; // Use an array for mutable int in anonymous class

        Toast.makeText(this, MSG_UPLOADING_IMAGES, Toast.LENGTH_LONG).show();

        for (Uri uri : selectedUris) {
            MediaManager.get().upload(uri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) { Log.d(TAG, "Upload started: " + requestId); }
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            double progress = (double) bytes / totalBytes;
                            Log.d(TAG, "Progress: " + (int) (progress * 100) + "%");
                        }
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            uploadedCount[0]++;
                            String imageUrl = (String) resultData.get("secure_url");
                            newUploadedUrls.add(imageUrl); // Add to the temporary list
                            Log.d(TAG, "Upload success: " + imageUrl);
                            checkIfAllImagesUploaded(totalImages, uploadedCount[0], newUploadedUrls);
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            uploadedCount[0]++;
                            Log.e(TAG, ERROR_UPLOAD_IMAGE + error.getDescription());
                            Toast.makeText(CreateProfileActivity.this, ERROR_UPLOAD_IMAGE + error.getDescription(), Toast.LENGTH_LONG).show();
                            checkIfAllImagesUploaded(totalImages, uploadedCount[0], newUploadedUrls);
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + requestId + ": " + error.getDescription());
                        }
                    }).dispatch();
        }
    }

    // Modified to pass the temporary list of newly uploaded URLs
    private void checkIfAllImagesUploaded(int totalImages, int uploadedCount, List<String> newUploadedUrls) {
        if (uploadedCount == totalImages) {
            if (newUploadedUrls.size() == totalImages) {
                Toast.makeText(CreateProfileActivity.this, MSG_UPLOAD_SUCCESS, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Tất cả ảnh đã upload: " + newUploadedUrls.toString());
                // **IMPORTANT:** Replace the final list with the *newly uploaded* URLs
                finalImageUrlsToSave.clear();
                finalImageUrlsToSave.addAll(newUploadedUrls);
                saveProfileToFirestore(); // Proceed to save profile
            } else {
                String message = "Chỉ có " + newUploadedUrls.size() + " trên " + totalImages + " ảnh được tải lên thành công. ";
                if (newUploadedUrls.size() > 0) {
                    message += "Bạn có muốn lưu hồ sơ với các ảnh này?";
                } else {
                    message = "Không có ảnh nào được tải lên thành công. Vui lòng thử lại.";
                }
                new AlertDialog.Builder(CreateProfileActivity.this)
                        .setTitle("Lỗi tải ảnh")
                        .setMessage(message)
                        .setPositiveButton("Có", (dialog, which) -> {
                            // **IMPORTANT:** Replace the final list with the *partially uploaded* URLs
                            finalImageUrlsToSave.clear();
                            finalImageUrlsToSave.addAll(newUploadedUrls);
                            saveProfileToFirestore(); // If user accepts, save with partial upload
                        })
                        .setNegativeButton("Không", null)
                        .show();
                Log.w(TAG, "Một số ảnh không được upload thành công.");
            }
        }
    }

    private List<String> getSelectedFavorites() {
        List<String> favorites = new ArrayList<>();
        for (int i = 0; i < chipGroupFavorites.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupFavorites.getChildAt(i);
            favorites.add(chip.getText().toString());
        }
        return favorites;
    }

    private void saveProfileToFirestore() {
        String bio = editBio.getText().toString().trim();
        String name = editName.getText().toString().trim();
        String gender = "";
        int selectedId = radioGender.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            gender = selectedRadioButton.getText().toString();
        }

        int age = 0;
        if (spinnerYearOfBirth.getSelectedItem() != null) {
            try {
                age = Integer.parseInt(spinnerYearOfBirth.getSelectedItem().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi chuyển đổi năm sinh: " + e.getMessage());
                Toast.makeText(this, "Năm sinh không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Vui lòng chọn năm sinh.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> favorites = getSelectedFavorites();

        // ✅ Tạo đối tượng User
        User userProfile = new User(
                currentUserId,
                name,
                gender,
                bio,
                age,
                favorites,
                finalImageUrlsToSave,
                storedLatitude,
                storedLongitude
        );

        // ✅ Lưu lên Firestore
        db.collection("profiles").document(currentUserId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateProfileActivity.this, MSG_SAVE_PROFILE_SUCCESS, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Profile successfully saved for user: " + currentUserId);
                    Intent i = new Intent(CreateProfileActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateProfileActivity.this, MSG_SAVE_PROFILE_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error saving profile for user: " + currentUserId, e);
                });
    }


    private void showFavoritesDialog() {
        // Implement logic to correctly pre-select existing favorites in the dialog
        boolean[] selectedItems = new boolean[availableFavorites.length];
        List<String> currentFavorites = getSelectedFavorites(); // Get currently displayed chips

        for (int i = 0; i < availableFavorites.length; i++) {
            selectedItems[i] = currentFavorites.contains(availableFavorites[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(DIALOG_TITLE_FAVORITES);

        builder.setMultiChoiceItems(availableFavorites, selectedItems, (dialog, which, isChecked) -> {
            selectedItems[which] = isChecked;
        });

        builder.setPositiveButton(BUTTON_OK, (dialog, which) -> {
            chipGroupFavorites.removeAllViews();
            for (int i = 0; i < availableFavorites.length; i++) {
                if (selectedItems[i]) {
                    addChip(availableFavorites[i]);
                }
            }
        });

        builder.setNegativeButton(BUTTON_CANCEL, null);
        builder.show();
    }

    private void addChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> chipGroupFavorites.removeView(chip));

        int iconResId = getIconForFavorite(text);
        if (iconResId != 0) {
            chip.setChipIconResource(iconResId);
            // Ensure you have a proper tint color defined in your colors.xml or theme
            chip.setChipIconTintResource(R.color.black); // Using R.color.black if defined
        }

        chipGroupFavorites.addView(chip);
    }

    private int getIconForFavorite(String favorite) {
        switch (favorite) {
            case "Du lịch":
                return R.drawable.ic_dulich; // Replace with your actual drawable
            case "Âm nhạc":
                return R.drawable.ic_dulich; // Replace with your actual drawable
            case "Thể thao":
                return R.drawable.ic_dulich; // Replace with your actual drawable
            case "Ẩm thực":
                return R.drawable.ic_dulich;   // Replace with your actual drawable
            case "Chơi game":
                return R.drawable.ic_dulich; // Replace with your actual drawable
            case "Nấu ăn":
                return R.drawable.ic_dulich; // Replace with your actual drawable
            default:
                return 0;
        }
    }
}