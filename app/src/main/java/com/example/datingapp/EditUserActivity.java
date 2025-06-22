package com.example.datingapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//package com.example.datingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
//import com.example.datingapp.models.ImageItem; // Import ImageItem mới
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
import java.util.concurrent.atomic.AtomicInteger; // Thêm import này

import gun0912.tedimagepicker.builder.TedImagePicker;

public class EditUserActivity extends AppCompatActivity {

//    private EditText editName, editBio;
//    private RadioGroup radioGender;
//    private RecyclerView recyclerView;
//    private SelectedImagesAdapter adapter;
//    private List<ImageItem> selectedImagesForDisplay; // List để hiển thị trên RecyclerView
//    private Button btnAddImg;
//    private Button btnFavorite;
//    private Button btnSave;
//    private Spinner spinnerYearOfBirth;
//    private List<String> finalImageUrlsToSave; // List chứa URLs cuối cùng để lưu vào Firestore
//
//    private ChipGroup chipGroupFavorites;
//
//    private FirebaseFirestore db;
//    private FirebaseAuth mAuth;
//    private String currentUserId;
//
//    private Double storedLatitude = null;
//    private Double storedLongitude = null;
//
//    private static final String TAG = "CreateProfileActivity";
//    private static final String ERROR_UPLOAD_IMAGE = "Lỗi tải ảnh: ";
//    private static final String MSG_PLEASE_SELECT_IMAGES = "Vui lòng chọn ảnh trước khi lưu hồ sơ.";
//    private static final String MSG_UPLOADING_IMAGES = "Đang tải ảnh lên Cloudinary...";
//    private static final String MSG_UPLOAD_SUCCESS = "Tải ảnh lên thành công!";
//    private static final String MSG_SAVE_PROFILE_SUCCESS = "Lưu hồ sơ thành công!";
//    private static final String MSG_SAVE_PROFILE_ERROR = "Lỗi khi lưu hồ sơ: ";
//    private static final String MSG_NOT_LOGGED_IN = "Bạn cần đăng nhập để lưu hồ sơ.";
//    private static final String DIALOG_TITLE_FAVORITES = "Chọn sở thích";
//    private static final String BUTTON_OK = "OK";
//    private static final String BUTTON_CANCEL = "Hủy";
//    private static final int MAX_IMAGES = 6;
//    private static final String MSG_MAX_IMAGES = "Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh";
//
//
//    private String[] availableFavorites = {
//            "Du lịch", "Âm nhạc", "Thể thao",
//            "Ẩm thực", "Chơi game", "Nấu ăn"
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
//
//        // Ánh xạ các thành phần UI
//        recyclerView = findViewById(R.id.recyclerSelectedImages);
//        btnAddImg = findViewById(R.id.btnAddImg);
//        btnFavorite = findViewById(R.id.btnFavorite);
//        chipGroupFavorites = findViewById(R.id.chipGroupFavorites);
//        btnSave = findViewById(R.id.btnSave);
//        radioGender = findViewById(R.id.radioGender);
//        editName = findViewById(R.id.editName);
//        editBio = findViewById(R.id.editBio);
//        spinnerYearOfBirth = findViewById(R.id.spinnerYearOfBirth);
//
//        // Tạo list năm sinh từ 1950 đến 2009
//        List<String> yearList = new ArrayList<>();
//        for (int year = 1950; year <= 2009; year++) {
//            yearList.add(String.valueOf(year));
//        }
//
//        // Tạo adapter và gán vào spinner
//        ArrayAdapter<String> yearOfBirthAdapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_spinner_item,
//                yearList);
//        yearOfBirthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinnerYearOfBirth.setAdapter(yearOfBirthAdapter);
//
//        // Khởi tạo Firebase
//        db = FirebaseFirestore.getInstance();
//        mAuth = FirebaseAuth.getInstance();
//
//        // Kiểm tra người dùng đã đăng nhập chưa
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, MSG_NOT_LOGGED_IN, Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//            return;
//        }
//        currentUserId = currentUser.getUid();
//
//        // Cấu hình RecyclerView cho ảnh đã chọn
//        selectedImagesForDisplay = new ArrayList<>(); // Khởi tạo list mới
//        finalImageUrlsToSave = new ArrayList<>(); // Khởi tạo list URLs để lưu
//
//        adapter = new SelectedImagesAdapter(this, selectedImagesForDisplay, position -> {
//            // Logic khi người dùng click nút xóa ảnh
//            ImageItem removedItem = selectedImagesForDisplay.remove(position);
//            adapter.notifyItemRemoved(position);
//            adapter.notifyItemRangeChanged(position, selectedImagesForDisplay.size());
//
//            // Nếu là ảnh đã tải lên (có URL), xóa nó khỏi finalImageUrlsToSave
//            if (!removedItem.isNew() && removedItem.getImageUrl() != null) {
//                finalImageUrlsToSave.remove(removedItem.getImageUrl());
//            }
//            // Nếu là ảnh mới chọn (có Uri), không cần làm gì với finalImageUrlsToSave ngay
//            // vì nó sẽ được tạo lại sau khi upload
//        });
//        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//        recyclerView.setAdapter(adapter);
//
//        // TẢI DỮ LIỆU HỒ SƠ HIỆN CÓ (NẾU CÓ)
//        loadProfileData();
//
//        // Lắng nghe sự kiện click cho nút "Thêm ảnh"
//        btnAddImg.setOnClickListener(v -> {
//            TedImagePicker.with(this)
//                    .max(MAX_IMAGES, MSG_MAX_IMAGES)
//                    .startMultiImage(uriList -> {
//                        // Clear các ảnh cũ và thêm các ảnh mới chọn
//                        selectedImagesForDisplay.clear();
//                        for (Uri uri : uriList) {
//                            selectedImagesForDisplay.add(new ImageItem(uri));
//                        }
//                        adapter.notifyDataSetChanged();
//                        // finalImageUrlsToSave sẽ được cập nhật sau khi upload
//                    });
//        });
//
//        // Lắng nghe sự kiện click cho nút "Chọn sở thích"
//        btnFavorite.setOnClickListener(v -> showFavoritesDialog());
//
//        // Lắng nghe sự kiện click cho nút "Lưu Hồ Sơ"
//        btnSave.setOnClickListener(v -> {
//            // Kiểm tra xem có ảnh mới được chọn không
//            boolean hasNewImages = false;
//            for (ImageItem item : selectedImagesForDisplay) {
//                if (item.isNew()) { // Kiểm tra xem có bất kỳ ImageItem nào là ảnh mới (Uri) không
//                    hasNewImages = true;
//                    break;
//                }
//            }
//
//            if (hasNewImages) {
//                // Nếu có ảnh mới, tiến hành upload các ảnh đó
//                uploadImagesToCloudinary();
//            } else {
//                // Nếu không có ảnh mới được chọn (người dùng chỉ sửa text hoặc không thay đổi ảnh)
//                // và không có ảnh nào được hiển thị (tức là người dùng đã xóa hết ảnh cũ và không thêm mới)
//                if (selectedImagesForDisplay.isEmpty() && finalImageUrlsToSave.isEmpty()) {
//                    Toast.makeText(this, MSG_PLEASE_SELECT_IMAGES, Toast.LENGTH_SHORT).show();
//                } else {
//                    // Nếu không có ảnh mới và có ảnh cũ (hoặc người dùng đã xóa bớt ảnh cũ)
//                    // thì tiến hành lưu hồ sơ với list finalImageUrlsToSave hiện tại
//                    saveProfileToFirestore();
//                }
//            }
//        });
//    }
//
//    private void loadProfileData() {
//        db.collection("profiles").document(currentUserId).get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        editName.setText(documentSnapshot.getString("name"));
//                        editBio.setText(documentSnapshot.getString("bio"));
//
//                        String gender = documentSnapshot.getString("gender");
//                        if ("Nam".equals(gender)) {
//                            radioGender.check(R.id.radioMale);
//                        } else if ("Nữ".equals(gender)) {
//                            radioGender.check(R.id.radioFemale);
//                        }
//
//                        Long yearLong = documentSnapshot.getLong("age");
//                        if (yearLong != null) {
//                            String yearString = String.valueOf(yearLong);
//                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerYearOfBirth.getAdapter();
//                            if (adapter != null) {
//                                int spinnerPosition = adapter.getPosition(yearString);
//                                if (spinnerPosition != -1) {
//                                    spinnerYearOfBirth.setSelection(spinnerPosition);
//                                }
//                            }
//                        }
//
//                        List<String> favorites = (List<String>) documentSnapshot.get("favorites");
//                        if (favorites != null) {
//                            chipGroupFavorites.removeAllViews();
//                            for (String favorite : favorites) {
//                                addChip(favorite);
//                            }
//                        }
//
//                        // Tải ảnh đã upload từ Firestore vào cả hai list
//                        // 1. finalImageUrlsToSave: để lưu trữ các URL hiện có
//                        // 2. selectedImagesForDisplay: để hiển thị trên RecyclerView
//                        List<String> imgUrls = (List<String>) documentSnapshot.get("imgUrls");
//                        if (imgUrls != null) {
//                            finalImageUrlsToSave.clear(); // Xóa sạch để đảm bảo không bị trùng lặp
//                            finalImageUrlsToSave.addAll(imgUrls); // Thêm các URL cũ vào list sẽ lưu
//
//                            selectedImagesForDisplay.clear(); // Xóa sạch để hiển thị lại
//                            for (String url : imgUrls) {
//                                selectedImagesForDisplay.add(new ImageItem(url)); // Thêm ImageItem từ URL
//                            }
//                            adapter.notifyDataSetChanged(); // Cập nhật RecyclerView
//                            Log.d(TAG, "Loaded existing image URLs: " + finalImageUrlsToSave);
//                        }
//
//                        storedLatitude = documentSnapshot.getDouble("latitude");
//                        storedLongitude = documentSnapshot.getDouble("longitude");
//
//                        Log.d(TAG, "Đã tải dữ liệu hồ sơ: " + documentSnapshot.getData());
//                        Log.d(TAG, "Vị trí đã lưu: Lat=" + storedLatitude + ", Lng=" + storedLongitude);
//
//                    } else {
//                        Log.d(TAG, "Không có hồ sơ hiện có cho người dùng.");
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Lỗi khi tải hồ sơ: " + e.getMessage());
//                    Toast.makeText(this, "Lỗi khi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void uploadImagesToCloudinary() {
//        // Lọc ra chỉ các Uri mới cần upload
//        List<Uri> urisToUpload = new ArrayList<>();
//        for (ImageItem item : selectedImagesForDisplay) {
//            if (item.isNew() && item.getImageUri() != null) {
//                urisToUpload.add(item.getImageUri());
//            }
//        }
//
//        if (urisToUpload.isEmpty()) {
//            // Không có ảnh mới nào cần upload, chỉ lưu các ảnh cũ (đã được lọc qua hành động xóa)
//            saveProfileToFirestore();
//            return;
//        }
//
//        // Tạo một list tạm thời để chứa các URL ảnh mới tải lên
//        List<String> newlyUploadedUrls = new ArrayList<>();
//        // Sao chép các URL ảnh CŨ ĐANG CÓ để ghép với ảnh mới
//        // Điều này đảm bảo rằng các ảnh cũ KHÔNG bị xóa mà người dùng không tương tác sẽ được giữ lại
//        List<String> currentExistingUrls = new ArrayList<>();
//        for(ImageItem item : selectedImagesForDisplay) {
//            if (!item.isNew() && item.getImageUrl() != null) {
//                currentExistingUrls.add(item.getImageUrl());
//            }
//        }
//
//
//        final int totalImagesToUpload = urisToUpload.size();
//        final AtomicInteger uploadedCount = new AtomicInteger(0); // Sử dụng AtomicInteger cho biến đếm
//
//        Toast.makeText(this, MSG_UPLOADING_IMAGES, Toast.LENGTH_LONG).show();
//
//        for (Uri uri : urisToUpload) {
//            MediaManager.get().upload(uri)
//                    .callback(new UploadCallback() {
//                        @Override
//                        public void onStart(String requestId) { Log.d(TAG, "Upload started: " + requestId); }
//                        @Override
//                        public void onProgress(String requestId, long bytes, long totalBytes) {
//                            double progress = (double) bytes / totalBytes;
//                            Log.d(TAG, "Progress: " + (int) (progress * 100) + "%");
//                        }
//                        @Override
//                        public void onSuccess(String requestId, Map resultData) {
//                            uploadedCount.incrementAndGet();
//                            String imageUrl = (String) resultData.get("secure_url");
//                            newlyUploadedUrls.add(imageUrl);
//                            Log.d(TAG, "Upload success: " + imageUrl);
//                            checkIfAllImagesUploaded(totalImagesToUpload, uploadedCount.get(), currentExistingUrls, newlyUploadedUrls);
//                        }
//                        @Override
//                        public void onError(String requestId, ErrorInfo error) {
//                            uploadedCount.incrementAndGet();
//                            Log.e(TAG, ERROR_UPLOAD_IMAGE + error.getDescription());
//                            Toast.makeText(CreateProfileActivity.this, ERROR_UPLOAD_IMAGE + error.getDescription(), Toast.LENGTH_LONG).show();
//                            checkIfAllImagesUploaded(totalImagesToUpload, uploadedCount.get(), currentExistingUrls, newlyUploadedUrls);
//                        }
//                        @Override
//                        public void onReschedule(String requestId, ErrorInfo error) {
//                            Log.w(TAG, "Upload rescheduled: " + requestId + ": " + error.getDescription());
//                        }
//                    }).dispatch();
//        }
//    }
//
//    private void checkIfAllImagesUploaded(int totalImagesToUpload, int currentUploadedCount, List<String> existingUrls, List<String> newlyUploadedUrls) {
//        if (currentUploadedCount == totalImagesToUpload) {
//            // Ghép danh sách URL ảnh cũ (chưa bị xóa) và các URL ảnh mới tải lên
//            finalImageUrlsToSave.clear();
//            finalImageUrlsToSave.addAll(existingUrls);
//            finalImageUrlsToSave.addAll(newlyUploadedUrls);
//
//
//            if (newlyUploadedUrls.size() == totalImagesToUpload) {
//                Toast.makeText(EditUserActivity.this, MSG_UPLOAD_SUCCESS, Toast.LENGTH_SHORT).show();
//                Log.d(TAG, "Tất cả ảnh mới đã upload. Tổng số ảnh để lưu: " + finalImageUrlsToSave.toString());
//                saveProfileToFirestore();
//            } else {
//                String message = "Chỉ có " + newlyUploadedUrls.size() + " trên " + totalImagesToUpload + " ảnh mới được tải lên thành công. ";
//                if (!finalImageUrlsToSave.isEmpty()) { // Kiểm tra tổng số ảnh sau khi ghép
//                    message += "Bạn có muốn lưu hồ sơ với các ảnh này?";
//                } else {
//                    message = "Không có ảnh nào được tải lên thành công. Vui lòng thử lại.";
//                }
//                new AlertDialog.Builder(EditUserActivity.this)
//                        .setTitle("Lỗi tải ảnh")
//                        .setMessage(message)
//                        .setPositiveButton("Có", (dialog, which) -> saveProfileToFirestore()) // Nếu đồng ý, vẫn lưu
//                        .setNegativeButton("Không", null)
//                        .show();
//                Log.w(TAG, "Một số ảnh mới không được upload thành công.");
//            }
//        }
//    }
//
//
//    private List<String> getSelectedFavorites() {
//        List<String> favorites = new ArrayList<>();
//        for (int i = 0; i < chipGroupFavorites.getChildCount(); i++) {
//            Chip chip = (Chip) chipGroupFavorites.getChildAt(i);
//            favorites.add(chip.getText().toString());
//        }
//        return favorites;
//    }
//
//    private void saveProfileToFirestore() {
//        String bio = editBio.getText().toString().trim();
//        String name = editName.getText().toString().trim();
//        String gender = "";
//        int selectedId = radioGender.getCheckedRadioButtonId();
//        if (selectedId != -1) {
//            RadioButton selectedRadioButton = findViewById(selectedId);
//            gender = selectedRadioButton.getText().toString();
//        }
//
//        int age = 0;
//        if (spinnerYearOfBirth.getSelectedItem() != null) {
//            try {
//                age = Integer.parseInt(spinnerYearOfBirth.getSelectedItem().toString());
//            } catch (NumberFormatException e) {
//                Log.e(TAG, "Lỗi chuyển đổi năm sinh thành số nguyên: " + e.getMessage());
//                Toast.makeText(this, "Năm sinh không hợp lệ.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//        } else {
//            Toast.makeText(this, "Vui lòng chọn năm sinh.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        List<String> favorites = getSelectedFavorites();
//
//        Map<String, Object> profileData = new HashMap<>();
//        profileData.put("bio", bio);
//        profileData.put("favorites", favorites);
//        profileData.put("gender", gender);
//        profileData.put("imgUrls", finalImageUrlsToSave); // Sử dụng danh sách URL ảnh đã xử lý
//        profileData.put("name", name);
//        profileData.put("age", age);
//
//        if (storedLatitude != null && storedLongitude != null) {
//            profileData.put("latitude", storedLatitude);
//            profileData.put("longitude", storedLongitude);
//            Log.d(TAG, "Đã thêm vị trí (từ Firestore) vào profileData: Lat=" + storedLatitude + ", Lng=" + storedLongitude);
//        } else {
//            Log.d(TAG, "Không có vị trí đã lưu để thêm vào profileData.");
//        }
//
//        db.collection("profiles").document(currentUserId)
//                .set(profileData)
//                .addOnSuccessListener(aVoid -> {
//                    Toast.makeText(EditUserActivity.this, MSG_SAVE_PROFILE_SUCCESS, Toast.LENGTH_SHORT).show();
//                    Log.d(TAG, "Profile successfully saved for user: " + currentUserId);
//                    Intent i = new Intent(EditUserActivity.this, MainActivity.class);
//                    startActivity(i);
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(EditUserActivity.this, MSG_SAVE_PROFILE_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
//                    Log.e(TAG, "Error saving profile for user: " + currentUserId, e);
//                });
//    }
//
//    private void showFavoritesDialog() {
//        boolean[] selectedItems = new boolean[availableFavorites.length];
//        List<String> currentFavorites = getSelectedFavorites();
//
//        for (int i = 0; i < availableFavorites.length; i++) {
//            selectedItems[i] = currentFavorites.contains(availableFavorites[i]);
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(DIALOG_TITLE_FAVORITES);
//
//        builder.setMultiChoiceItems(availableFavorites, selectedItems, (dialog, which, isChecked) -> {
//            selectedItems[which] = isChecked;
//        });
//
//        builder.setPositiveButton(BUTTON_OK, (dialog, which) -> {
//            chipGroupFavorites.removeAllViews();
//            for (int i = 0; i < availableFavorites.length; i++) {
//                if (selectedItems[i]) {
//                    addChip(availableFavorites[i]);
//                }
//            }
//        });
//
//        builder.setNegativeButton(BUTTON_CANCEL, null);
//        builder.show();
//    }
//
//    private void addChip(String text) {
//        Chip chip = new Chip(this);
//        chip.setText(text);
//        chip.setCloseIconVisible(true);
//        chip.setOnCloseIconClickListener(v -> chipGroupFavorites.removeView(chip));
//
//        int iconResId = getIconForFavorite(text);
//        if (iconResId != 0) {
//            chip.setChipIconResource(iconResId);
//            chip.setChipIconTintResource(R.color.black);
//        }
//
//        chipGroupFavorites.addView(chip);
//    }
//
//    private int getIconForFavorite(String favorite) {
//        switch (favorite) {
//            case "Du lịch":
//                return R.drawable.ic_dulich;
//            case "Âm nhạc":
//                return R.drawable.ic_music;
//            case "Thể thao":
//                return R.drawable.ic_sports;
//            case "Ẩm thực":
//                    return R.drawable.ic_food;
//            case "Chơi game":
//                return R.drawable.ic_gaming;
//            case "Nấu ăn":
//                return R.drawable.ic_cooking;
//            default:
//                return 0;
//        }
    }
}