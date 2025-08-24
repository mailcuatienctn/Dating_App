package com.example.datingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View; // ⭐ Thêm import này ⭐
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout; // ⭐ Thêm import này ⭐
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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
import java.util.Calendar; // ⭐ Thêm import này cho Calendar ⭐
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale; // ⭐ Thêm import này cho Locale (nếu dùng cho Spinner) ⭐

import gun0912.tedimagepicker.builder.TedImagePicker;

public class CreateProfileActivity extends AppCompatActivity {

    private EditText editName, editBio;
    private RadioGroup radioGender;
    private RecyclerView recyclerSelectedImages;
    private SelectedImagesAdapter adapter;
    private List<Uri> selectedUris;
    private Button btnAddImg;
    private Button btnFavorite;
    private Button btnSave;
    private Spinner spinnerYearOfBirth, spinnerHeight;
    private List<String> finalImageUrlsToSave = new ArrayList<>();
    private ChipGroup chipGroupFavorites;
    private ScrollView scrollViewRoot;
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
    private static final String BUTTON_OK = "Xác nhận";
    private static final String BUTTON_CANCEL = "Hủy";
    private static final int MAX_IMAGES = 6;
    private static final String MSG_MAX_IMAGES = "Chỉ được chọn tối đa " + MAX_IMAGES + " ảnh";


    private String[] availableFavorites = {
            "Đọc sách", "Chơi game", "Du lịch", "Nấu ăn", "Tập gym", "Nghe nhạc",
            "Xem phim", "Bơi lội", "Hội họa", "Chụp ảnh", "Ca hát", "Nhảy múa",
            "Tìm hiểu công nghệ", "Thiền", "Yoga", "Câu cá", "Đạp xe"
    };

    private LinearLayout step1Layout, step2Layout, step3Layout, step4Layout, step5Layout, step6Layout;
    private Button btnNext1, btnBack1, btnNext2, btnBack2, btnNext3, btnBack3, btnNext4, btnBack4, btnNext5, btnBack5;

    private int currentStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, MSG_NOT_LOGGED_IN, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        scrollViewRoot = findViewById(R.id.scrollViewRoot);

        // Ánh xạ các thành phần UI theo từng bước
        // Bước 1: Tên
        step1Layout = findViewById(R.id.step1_name_layout);
        editName = findViewById(R.id.editName);
        btnNext1 = findViewById(R.id.btnNext1);

        // Bước 2: Năm sinh
        step2Layout = findViewById(R.id.step2_birthyear_layout);
        spinnerYearOfBirth = findViewById(R.id.spinnerYearOfBirth);
        btnBack1 = findViewById(R.id.btnBack1);
        btnNext2 = findViewById(R.id.btnNext2);

        // Bước 3: Giới thiệu bản thân
        step3Layout = findViewById(R.id.step3_bio_layout);
        editBio = findViewById(R.id.editBio);
        btnBack2 = findViewById(R.id.btnBack2);
        btnNext3 = findViewById(R.id.btnNext3);

        // Bước 4: Giới tính và Chiều cao
        step4Layout = findViewById(R.id.step4_gender_height_layout);
        radioGender = findViewById(R.id.radioGender);
        spinnerHeight = findViewById(R.id.spinnerHeight);
        // radioMale và radioFemale không cần ánh xạ lại nếu chỉ dùng thông qua radioGender
        btnBack3 = findViewById(R.id.btnBack3);
        btnNext4 = findViewById(R.id.btnNext4);

        // Bước 5: Sở thích
        step5Layout = findViewById(R.id.step5_favorites_layout);
        btnFavorite = findViewById(R.id.btnFavorite);
        chipGroupFavorites = findViewById(R.id.chipGroupFavorites);
        btnBack4 = findViewById(R.id.btnBack4);
        btnNext5 = findViewById(R.id.btnNext5);

        // Bước 6: Ảnh
        step6Layout = findViewById(R.id.step6_images_layout);
        btnAddImg = findViewById(R.id.btnAddImg);
        recyclerSelectedImages = findViewById(R.id.recyclerSelectedImages); // Đảm bảo ID này khớp XML
        btnBack5 = findViewById(R.id.btnBack5);
        btnSave = findViewById(R.id.btnSave); // Nút lưu hồ sơ cuối cùng

        // Cấu hình Spinner năm sinh
        setupYearSpinner();
        // Cấu hình Spinner chiều cao
        setupHeightSpinner();

        // Cấu hình RecyclerView cho ảnh đã chọn
        selectedUris = new ArrayList<>();
        adapter = new SelectedImagesAdapter(this, selectedUris);
        recyclerSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerSelectedImages.setAdapter(adapter);


        // Thiết lập listeners cho nút "Tiếp tục" và "Quay lại"
        btnNext1.setOnClickListener(v -> nextStep());
        btnBack1.setOnClickListener(v -> previousStep());
        btnNext2.setOnClickListener(v -> nextStep());
        btnBack2.setOnClickListener(v -> previousStep());
        btnNext3.setOnClickListener(v -> nextStep());
        btnBack3.setOnClickListener(v -> previousStep());
        btnNext4.setOnClickListener(v -> nextStep());
        btnBack4.setOnClickListener(v -> previousStep());
        btnNext5.setOnClickListener(v -> nextStep()); // Nút này sẽ dẫn đến bước cuối cùng (ảnh)
        btnBack5.setOnClickListener(v -> previousStep());

        storedLatitude = getIntent().getDoubleExtra("latitude", 0.0);
        storedLongitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Lắng nghe sự kiện click cho nút "Thêm ảnh"
        btnAddImg.setOnClickListener(v -> {
            TedImagePicker.with(this)
                    .max(MAX_IMAGES, MSG_MAX_IMAGES)
                    .startMultiImage(uriList -> {
                        selectedUris.clear(); // Xóa các Uri hiện có để thay thế bằng lựa chọn mới
                        selectedUris.addAll(uriList);
                        adapter.notifyDataSetChanged();
                    });
        });

        // Lắng nghe sự kiện click cho nút "Chọn sở thích"
        btnFavorite.setOnClickListener(v -> showFavoritesDialog());

        // Lắng nghe sự kiện click cho nút "Lưu Hồ Sơ" (chỉ ở bước cuối cùng)
        btnSave.setOnClickListener(v -> {
            // Validate bước cuối cùng trước khi lưu
            if (validateCurrentStep()) {
                if (!selectedUris.isEmpty()) {
                    uploadImagesToCloudinary();
                } else {
                    // Nếu không có ảnh mới được chọn, nhưng có ảnh cũ, hãy lưu hồ sơ
                    // Nếu không có cả ảnh mới lẫn ảnh cũ, hiển thị lỗi
                    if (finalImageUrlsToSave.isEmpty()) {
                        Toast.makeText(this, MSG_PLEASE_SELECT_IMAGES, Toast.LENGTH_SHORT).show();
                    } else {
                        // Trường hợp người dùng không chọn ảnh mới, nhưng đã có ảnh cũ từ Firestore
                        saveProfileToFirestore();
                    }
                }
            }
        });

        // Hiển thị bước đầu tiên khi Activity được tạo
        displayCurrentStep();
    }

    // Hàm xử lý chuyển bước - hiển thị/ẩn LinearLayout
    private void displayCurrentStep() {
        // Ẩn tất cả các layout
        step1Layout.setVisibility(View.GONE);
        step2Layout.setVisibility(View.GONE);
        step3Layout.setVisibility(View.GONE);
        step4Layout.setVisibility(View.GONE);
        step5Layout.setVisibility(View.GONE);
        step6Layout.setVisibility(View.GONE);

        // Hiển thị layout của bước hiện tại
        switch (currentStep) {
            case 1:
                step1Layout.setVisibility(View.VISIBLE);
                break;
            case 2:
                step2Layout.setVisibility(View.VISIBLE);
                break;
            case 3:
                step3Layout.setVisibility(View.VISIBLE);
                break;
            case 4:
                step4Layout.setVisibility(View.VISIBLE);
                break;
            case 5:
                step5Layout.setVisibility(View.VISIBLE);
                break;
            case 6:
                step6Layout.setVisibility(View.VISIBLE);
                break;
        }
        // Cuộn lên đầu ScrollView để người dùng thấy rõ bước mới
        // (Điều này yêu cầu layout gốc là ScrollView)
        findViewById(R.id.scrollViewRoot).post(() ->
                findViewById(R.id.scrollViewRoot).setScrollX(View.FOCUS_UP));
    }

    // Hàm chuyển đến bước tiếp theo
    private void nextStep() {
        // Validate dữ liệu của bước hiện tại trước khi chuyển tiếp
        if (validateCurrentStep()) {
            if (currentStep < 6) { // Tổng cộng có 6 bước
                currentStep++;
                displayCurrentStep();
            } else {
                // Đã ở bước cuối cùng (bước 6), nút "Tiếp tục" ở đây sẽ gọi lưu hồ sơ
                // Mặc dù chúng ta đã có btnSave riêng ở bước 6, nhưng đây là một dự phòng
                // (Thực tế, nút btnNext5 sẽ chuyển sang step 6, và btnSave ở step 6 sẽ là nút cuối cùng)
                saveProfile(); // Gọi hàm lưu hồ sơ nếu ở bước cuối
            }
        }
    }

    // Hàm quay lại bước trước
    private void previousStep() {
        if (currentStep > 1) {
            currentStep--;
            displayCurrentStep();
        }
    }

    // Hàm Validate dữ liệu của từng bước
    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1: // Tên
                String name = editName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Vui lòng nhập tên của bạn.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2: // Năm sinh
                if (spinnerYearOfBirth.getSelectedItem() == null || spinnerYearOfBirth.getSelectedItem().toString().equals("Chọn năm sinh") || spinnerYearOfBirth.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Vui lòng chọn năm sinh của bạn.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 3: // Giới thiệu bản thân
                String bio = editBio.getText().toString().trim();
                if (TextUtils.isEmpty(bio)) {
                    Toast.makeText(this, "Vui lòng giới thiệu bản thân.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 4: // Giới tính và Chiều cao
                if (radioGender.getCheckedRadioButtonId() == -1) {
                    Toast.makeText(this, "Vui lòng chọn giới tính của bạn.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (spinnerHeight.getSelectedItem() == null || spinnerHeight.getSelectedItem().toString().equals("Chọn chiều cao") || spinnerHeight.getSelectedItemPosition() == 0) {
                    Toast.makeText(this, "Vui lòng chọn chiều cao của bạn.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 5: // Sở thích
                if (chipGroupFavorites.getChildCount() == 0) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất một sở thích.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 6: // Ảnh (Đây là bước cuối cùng, validation cuối cùng sẽ xảy ra khi nhấn nút "Lưu hồ sơ")
                // Validation ảnh sẽ được xử lý trong hàm saveProfile(), có thể bỏ qua ở đây
                // hoặc thêm kiểm tra selectedUris.isEmpty() nếu bạn muốn người dùng phải chọn ảnh trước khi bấm next
                // (mặc dù họ sẽ lại nhấn btnSave ở cuối cùng)
                return true; // Để cho phép chuyển đến bước 6
            default:
                return true;
        }
    }

    private void setupYearSpinner() {
        List<String> yearList = new ArrayList<>();
        yearList.add("Chọn năm sinh"); // Placeholder
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear - 18; year >= 1950; year--) { // Từ 18 tuổi trở lên
            yearList.add(String.valueOf(year));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearOfBirth.setAdapter(adapter);
    }


    private void setupHeightSpinner() {
        List<String> heightList = new ArrayList<>();
        heightList.add("Chọn chiều cao"); // Placeholder
        for (int height = 140; height <= 220; height++) { // Từ 140cm đến 220cm
            heightList.add(height + " cm");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, heightList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHeight.setAdapter(adapter);
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

                        Long yearLong = 2025 - documentSnapshot.getLong("age");
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

                        Long heightLong = documentSnapshot.getLong("height");
                        if (heightLong != null) {
                            String heightStringWithUnit = heightLong + " cm";
                            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerHeight.getAdapter();
                            if (adapter != null) {
                                int spinnerPosition = adapter.getPosition(heightStringWithUnit);
                                if (spinnerPosition != -1) {
                                    spinnerHeight.setSelection(spinnerPosition);
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

                        List<String> imgUrls = (List<String>) documentSnapshot.get("imgUrls");
                        if (imgUrls != null) {
                            finalImageUrlsToSave.clear();
                            finalImageUrlsToSave.addAll(imgUrls);
                            Log.d(TAG, "Loaded existing image URLs: " + finalImageUrlsToSave);
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
        List<String> newUploadedUrls = new ArrayList<>();
        final int totalImages = selectedUris.size();
        final int[] uploadedCount = {0};

        if (totalImages == 0) {
            Toast.makeText(this, MSG_PLEASE_SELECT_IMAGES, Toast.LENGTH_SHORT).show();
            return;
        }

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
                            newUploadedUrls.add(imageUrl);
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

    private void checkIfAllImagesUploaded(int totalImages, int uploadedCount, List<String> newUploadedUrls) {
        if (uploadedCount == totalImages) {
            if (newUploadedUrls.size() == totalImages) {
                Toast.makeText(CreateProfileActivity.this, MSG_UPLOAD_SUCCESS, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Tất cả ảnh đã upload: " + newUploadedUrls.toString());
                finalImageUrlsToSave.clear(); // Xóa các URL ảnh cũ (nếu có)
                finalImageUrlsToSave.addAll(newUploadedUrls); // Thêm các URL ảnh mới
                saveProfileToFirestore();
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
                            finalImageUrlsToSave.clear(); // Xóa các URL ảnh cũ (nếu có)
                            finalImageUrlsToSave.addAll(newUploadedUrls); // Thêm các URL ảnh đã tải thành công
                            saveProfileToFirestore();
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

    private void saveProfile() {
        saveProfileToFirestore();
    }

    private void saveProfileToFirestore() {
        String name = editName.getText().toString().trim();
        String bio = editBio.getText().toString().trim();

        String gender = "";
        int selectedId = radioGender.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) { // Đảm bảo ID này đúng trong XML của bạn
            gender = "Nam";
        } else if (selectedId == R.id.radioFemale) { // Đảm bảo ID này đúng trong XML của bạn
            gender = "Nữ";
        }

        int yearOfBirth = 0;
        if (spinnerYearOfBirth.getSelectedItem() != null && spinnerYearOfBirth.getSelectedItemPosition() != 0) {
            try {
                yearOfBirth = Integer.parseInt(spinnerYearOfBirth.getSelectedItem().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi chuyển đổi năm sinh: " + e.getMessage());
                Toast.makeText(this, "Năm sinh không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Vui lòng chọn năm sinh.", Toast.LENGTH_SHORT).show();
            return;
        }

        int height = 0;
        if (spinnerHeight.getSelectedItem() != null && spinnerHeight.getSelectedItemPosition() != 0) {
            try {
                String selectedHeightString = spinnerHeight.getSelectedItem().toString();
                height = Integer.parseInt(selectedHeightString.replace(" cm", "").trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi chuyển đổi chiều cao: " + e.getMessage());
                Toast.makeText(this, "Chiều cao không hợp lệ.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Toast.makeText(this, "Vui lòng chọn chiều cao.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> favorites = getSelectedFavorites();

        // Tạo đối tượng User
        User userProfile = new User(
                currentUserId,
                name,
                gender,
                bio,
                2025 - yearOfBirth,
                favorites,
                finalImageUrlsToSave,
                storedLatitude,
                storedLongitude,
                "",
                height
        );

        // Lưu lên Firestore
        db.collection("profiles").document(currentUserId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CreateProfileActivity.this, MSG_SAVE_PROFILE_SUCCESS, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Profile successfully saved for user: " + currentUserId);
                    Intent i = new Intent(CreateProfileActivity.this, MainActivity.class);
                    i.putExtra("OPEN_FRAGMENT", "UserFragment"); // Báo MainActivity mở UserFragment
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateProfileActivity.this, MSG_SAVE_PROFILE_ERROR + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error saving profile for user: " + currentUserId, e);
                });
    }

    private void showFavoritesDialog() {
        boolean[] selectedItems = new boolean[availableFavorites.length];
        List<String> currentFavorites = getSelectedFavorites();

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
            chip.setChipIconTintResource(R.color.black); // Đảm bảo màu này được định nghĩa
        }

        chipGroupFavorites.addView(chip);
    }

    private int getIconForFavorite(String favorite) {
        // Bạn cần thay thế bằng các drawable icon thực tế của bạn
        switch (favorite) {
            case "Du lịch":
                return R.drawable.ic_heart;
            case "Âm nhạc":
                return R.drawable.ic_heart;
            case "Thể thao":
                return R.drawable.ic_heart;
            case "Ẩm thực":
                return R.drawable.ic_heart;
            case "Chơi game":
                return R.drawable.ic_heart;
            case "Nấu ăn":
                return R.drawable.ic_heart;
            case "Đọc sách":
                return R.drawable.ic_heart;
            case "Tập gym":
                return R.drawable.ic_heart;
            case "Nghe nhạc":
                return R.drawable.ic_heart;
            case "Xem phim":
                return R.drawable.ic_heart;
            case "Bơi lội":
                return R.drawable.ic_heart;
            case "Hội họa":
                return R.drawable.ic_heart;
            case "Chụp ảnh":
                return R.drawable.ic_heart;
            case "Ca hát":
                return R.drawable.ic_heart;
            case "Nhảy múa":
                return R.drawable.ic_heart;
            case "Tìm hiểu công nghệ":
                return R.drawable.ic_heart;
            case "Thiền":
                return R.drawable.ic_heart;
            case "Yoga":
                return R.drawable.ic_heart;
            case "Câu cá":
                return R.drawable.ic_heart;
            case "Đạp xe":
                return R.drawable.ic_heart;
            default:
                return 0;
        }
    }
}