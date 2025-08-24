package com.example.datingapp;

import static android.content.ContentValues.TAG;

import static com.example.datingapp.ProfileDetailActivity.EXTRA_USER_ID;


import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.datingapp.adapter.UserCardAdapter;
import com.example.datingapp.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoveryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FILTER = 100;

    public static final String EXTRA_INITIAL_GENDER_FILTER = "initial_gender_filter";
    private static final int REQUEST_CODE_DETAIL = 1001;
    private ImageView btn_filter;
    private FirebaseFirestore db;
    private List<User> userList = new ArrayList<>();
    private UserCardAdapter adapter;
    private String currentUserId;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    // Các biến lọc đầy đủ
    private String filterGender = "Bất kỳ";
    private int filterMinAge = 0, filterMaxAge = 0;
    private int filterMinHeight = 0, filterMaxHeight = 0;
    private int filterMaxDistance = 100;
    private List<String> filterFavorites = new ArrayList<>();

    private CardStackView cardStackView;
    private FloatingActionButton btnLike, btnDislike;
    private CardStackLayoutManager layoutManager;
    private TextView tvNoProfilesMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);

        btn_filter = findViewById(R.id.btn_filter);
        cardStackView = findViewById(R.id.card_stack_view);
        btnLike = findViewById(R.id.btn_like);
        btnDislike = findViewById(R.id.btn_dislike);
        tvNoProfilesMessage = findViewById(R.id.tv_no_profiles_message);
        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("uid", null);

        btn_filter.setOnClickListener(v -> {
            Intent i = new Intent(DiscoveryActivity.this, FilterActivity.class);
            // Truyền TẤT CẢ các bộ lọc HIỆN TẠI tới FilterActivity
            i.putExtra(FilterActivity.EXTRA_FILTER_GENDER, filterGender);
            i.putExtra(FilterActivity.EXTRA_FILTER_MIN_AGE, filterMinAge);
            i.putExtra(FilterActivity.EXTRA_FILTER_MAX_AGE, filterMaxAge);
            i.putExtra(FilterActivity.EXTRA_FILTER_MIN_HEIGHT, filterMinHeight);
            i.putExtra(FilterActivity.EXTRA_FILTER_MAX_HEIGHT, filterMaxHeight);
            i.putExtra(FilterActivity.EXTRA_FILTER_MAX_DISTANCE, filterMaxDistance);
            i.putStringArrayListExtra(FilterActivity.EXTRA_FILTER_FAVORITES, new ArrayList<>(filterFavorites));
            startActivityForResult(i, REQUEST_CODE_FILTER);
        });

        layoutManager = new CardStackLayoutManager(this, new CardStackListener() {
            @Override
            public void onCardSwiped(Direction direction) {
                if (layoutManager.getTopPosition() == adapter.getItemCount()) {
                    showNoProfilesMessage(true, "Bạn đã xem hết tất cả các hồ sơ hiện có!");
                    setSwipeButtonsVisibility(false);
                }
                handleSwipe(direction == Direction.Right);
            }
            @Override
            public void onCardDragging(Direction direction, float ratio) {}
            @Override public void onCardRewound() {}
            @Override public void onCardCanceled() {}
            @Override public void onCardAppeared(android.view.View view, int position) {}
            @Override public void onCardDisappeared(android.view.View view, int position) {}
        });

        cardStackView.setLayoutManager(layoutManager);

        btnLike.setOnClickListener(v -> {
            if (layoutManager.getTopPosition() < adapter.getItemCount()) {
                SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Right)
                        .setDuration(Duration.Normal.duration)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe();
            } else {
                Toast.makeText(DiscoveryActivity.this, "Không còn hồ sơ để vuốt.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDislike.setOnClickListener(v -> {
            if (layoutManager.getTopPosition() < adapter.getItemCount()) {
                SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Left)
                        .setDuration(Duration.Normal.duration)
                        .build();
                layoutManager.setSwipeAnimationSetting(setting);
                cardStackView.swipe();
            } else {
                Toast.makeText(DiscoveryActivity.this, "Không còn hồ sơ để vuốt.", Toast.LENGTH_SHORT).show();
            }
        });

        // Lấy vị trí của người dùng hiện tại từ Firestore trước khi tải hồ sơ khác
        fetchCurrentUserLocationAndLoadUsers();
    }

    private void handleSwipe(boolean isLiked) {
        int swipedPosition = layoutManager.getTopPosition() - 1;
        if (swipedPosition >= 0 && swipedPosition < adapter.getItemCount()) {
            User swipedUser = adapter.getItem(swipedPosition);
            saveSwipe(currentUserId, isLiked, swipedUser.getUid());
        }
    }

    private void fetchCurrentUserLocationAndLoadUsers() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước.", Toast.LENGTH_SHORT).show();
            showNoProfilesMessage(true, "Vui lòng đăng nhập để xem hồ sơ.");
            setSwipeButtonsVisibility(false);
            return;
        }

        db.collection("profiles").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null && currentUser.getLatitude() != null && currentUser.getLongitude() != null) {
                            currentLat = currentUser.getLatitude();
                            currentLng = currentUser.getLongitude();
                            Log.d(TAG, "Lấy vị trí người dùng hiện tại từ Firestore: " + currentLat + ", " + currentLng);
                        } else {
                            Log.w(TAG, "Không tìm thấy vị trí (kinh độ/vĩ độ) trong hồ sơ của người dùng hiện tại.");
                            Toast.makeText(this, "Không tìm thấy vị trí của bạn. Lọc khoảng cách có thể không chính xác.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.w(TAG, "Không tìm thấy hồ sơ của người dùng hiện tại trong Firestore.");
                        Toast.makeText(this, "Không tìm thấy hồ sơ của bạn. Vui lòng cập nhật hồ sơ để sử dụng tính năng lọc khoảng cách.", Toast.LENGTH_LONG).show();
                    }
                    // Dù có lấy được vị trí hay không, vẫn tiến hành tải các hồ sơ khác
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy hồ sơ người dùng hiện tại: " + e.getMessage());
                    Toast.makeText(this, "Lỗi khi lấy vị trí của bạn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Trường hợp lỗi, vẫn tiến hành tải các hồ sơ khác
                    loadUsers();
                });
    }

    private void loadUsers() {
        if (currentUserId == null) {
            return;
        }

        // Bước 1: Lấy UID đã like
        db.collection("swipes").document(currentUserId).collection("likes").get()
                .addOnSuccessListener(likesSnapshot -> {
                    List<String> likedUids = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : likesSnapshot) {
                        likedUids.add(doc.getId());
                    }

                    // Bước 2: Lấy UID đã dislike
                    db.collection("swipes").document(currentUserId).collection("dislikes").get()
                            .addOnSuccessListener(dislikesSnapshot -> {
                                List<String> dislikedUids = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : dislikesSnapshot) {
                                    dislikedUids.add(doc.getId());
                                }

                                // Bước 3: Lấy người đã like mình
                                loadUsersWhoLikedMe(likedYouUids -> {

                                    // Bước 4: Lấy danh sách profiles
                                    db.collection("profiles").get()
                                            .addOnSuccessListener(profilesSnapshot -> {
                                                List<User> tempFilteredProfiles = new ArrayList<>();
                                                for (QueryDocumentSnapshot doc : profilesSnapshot) {
                                                    User user = doc.toObject(User.class);
                                                    if (user == null || user.getUid().equals(currentUserId))
                                                        continue;

                                                    // Bỏ người mình đã like hoặc dislike
                                                    if (likedUids.contains(user.getUid()) || dislikedUids.contains(user.getUid()))
                                                        continue;

                                                    // Bỏ người đã like mình
                                                    if (likedYouUids.contains(user.getUid()))
                                                        continue;

                                                    if (applyFilters(user)) {
                                                        tempFilteredProfiles.add(user);
                                                    }
                                                }

                                                userList.clear();
                                                userList.addAll(tempFilteredProfiles);

                                                if (adapter == null) {
                                                    adapter = new UserCardAdapter(userList, this, currentLat, currentLng);
                                                    cardStackView.setAdapter(adapter);
                                                } else {
                                                    adapter.updateUsers(userList);
                                                }

                                                if (userList.isEmpty()) {
                                                    showNoProfilesMessage(true, "Không tìm thấy hồ sơ phù hợp với bộ lọc của bạn.");
                                                    setSwipeButtonsVisibility(false);
                                                } else {
                                                    showNoProfilesMessage(false, "");
                                                    setSwipeButtonsVisibility(true);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Lỗi khi tải hồ sơ: " + e.getMessage());
                                                Toast.makeText(DiscoveryActivity.this, "Lỗi khi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                showNoProfilesMessage(true, "Đã xảy ra lỗi khi tải hồ sơ. Vui lòng thử lại sau.");
                                                setSwipeButtonsVisibility(false);
                                            });
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi khi tải dislikes: " + e.getMessage());
                                Toast.makeText(DiscoveryActivity.this, "Lỗi khi tải dữ liệu swipe: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                showNoProfilesMessage(true, "Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại.");
                                setSwipeButtonsVisibility(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tải likes: " + e.getMessage());
                    Toast.makeText(DiscoveryActivity.this, "Lỗi khi tải dữ liệu swipe: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showNoProfilesMessage(true, "Đã xảy ra lỗi khi tải dữ liệu. Vui lòng thử lại.");
                    setSwipeButtonsVisibility(false);
                });
    }


    private boolean applyFilters(User user) {
        // Lọc trạng thái ghép đôi: chỉ hiển thị người đang bật ghép đôi
        if (user.getState() != null && !user.getState()) return false;

        // Lọc giới tính
        if (!filterGender.equals("Bất kỳ") && !filterGender.equalsIgnoreCase(user.getGender())) return false;

        // Lọc tuổi
        int age = user.getAge();
        if ((filterMinAge > 0 && age < filterMinAge) || (filterMaxAge > 0 && age > filterMaxAge)) return false;

        // Lọc chiều cao
        int height = user.getHeight();
        if ((filterMinHeight > 0 && height < filterMinHeight) || (filterMaxHeight > 0 && height > filterMaxHeight)) return false;

        // Lọc sở thích
        if (!filterFavorites.isEmpty() && user.getFavorites() != null) {
            boolean hasCommon = false;
            for (String fav : filterFavorites) {
                if (user.getFavorites().contains(fav)) {
                    hasCommon = true;
                    break;
                }
            }
            if (!hasCommon) return false;
        }

        // Lọc khoảng cách: Chỉ áp dụng nếu đã có vị trí của người dùng hiện tại và người dùng khác
        if (filterMaxDistance > 0 && currentLat != 0.0 && currentLng != 0.0 &&
                user.getLatitude() != null && user.getLongitude() != null) {
            float[] results = new float[1];
            Location.distanceBetween(currentLat, currentLng, user.getLatitude(), user.getLongitude(), results);
            if (results[0] / 1000f > filterMaxDistance) return false;
        } else if (filterMaxDistance > 0 && (currentLat == 0.0 || currentLng == 0.0)) {
            Log.w(TAG, "Không thể áp dụng bộ lọc khoảng cách: vị trí người dùng hiện tại không có sẵn.");
        }

        return true;
    }

    private void saveSwipe(String currentUid, boolean isLiked, String targetUid) {
        if (TextUtils.isEmpty(currentUid) || TextUtils.isEmpty(targetUid)) return;

        String collection = isLiked ? "likes" : "dislikes";
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());

        if (isLiked) {
            data.put("liked", 1);
        }

        // Vẫn lưu collection likes/dislikes
        db.collection("swipes").document(currentUid)
                .collection(collection).document(targetUid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (isLiked) {
                        checkForMatch(currentUid, targetUid);
                        Map<String, Object> likerData = new HashMap<>();
                        likerData.put("timestamp", FieldValue.serverTimestamp());

                        db.collection("profiles")
                                .document(targetUid)
                                .collection("likes_received")
                                .document(currentUid)
                                .set(likerData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã thêm " + currentUid + " vào likes_received của " + targetUid))
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi thêm likes_received: " + e.getMessage(), e));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu swipe.", Toast.LENGTH_SHORT).show());

        // Lưu field vào chính swipes/{currentUid}
        Map<String, Object> fieldUpdate = new HashMap<>();
        fieldUpdate.put(targetUid, 1);

        db.collection("swipes").document(currentUid)
                .set(fieldUpdate, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "Đã lưu field " + targetUid + " = 1 trong swipes/" + currentUid))
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lưu field vào swipes/" + currentUid, e));
    }



    private void checkForMatch(String currentUid, String targetUid) {
        db.collection("swipes").document(targetUid)
                .collection("likes").document(currentUid)
                .get()
                .addOnSuccessListener(docSnap -> {
                    if (docSnap.exists()) {
                        Toast.makeText(this, "Bạn đã match với nhau!", Toast.LENGTH_SHORT).show();
                        saveMatch(currentUid, targetUid);
                    }
                });
    }

    private void saveMatch(String uid1, String uid2) {
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("timestamp", FieldValue.serverTimestamp());
        matchData.put("user1", uid1);
        matchData.put("user2", uid2);
        String matchDocId = uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;

        db.collection("matches").document(matchDocId)
                .set(matchData)
                .addOnSuccessListener(unused -> Log.d(TAG, "Match saved between " + uid1 + " and " + uid2))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving match: " + e.getMessage()));
    }



    private void showNoProfilesMessage(boolean show, String message) {
        if (show) {
            tvNoProfilesMessage.setText(message);
            tvNoProfilesMessage.setVisibility(View.VISIBLE);
            cardStackView.setVisibility(View.GONE);
        } else {
            tvNoProfilesMessage.setVisibility(View.GONE);
            cardStackView.setVisibility(View.VISIBLE);
        }
    }

    private void setSwipeButtonsVisibility(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        btnLike.setVisibility(visibility);
        btnDislike.setVisibility(visibility);
    }

    private void loadUsersWhoLikedMe(OnLikedYouLoadedListener listener) {
        List<String> likedYouUids = new ArrayList<>();

        db.collection("swipes")
                .get()
                .addOnSuccessListener(swipesSnapshot -> {
                    if (swipesSnapshot.isEmpty()) {
                        Log.d(TAG, "Không có ai đã like ai.");
                        listener.onLikedYouLoaded(likedYouUids);
                    } else {
                        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                        for (QueryDocumentSnapshot swiperDoc : swipesSnapshot) {
                            String swiperId = swiperDoc.getId();
                            Task<QuerySnapshot> task = db.collection("swipes")
                                    .document(swiperId)
                                    .collection("likes")
                                    .get()
                                    .addOnSuccessListener(likesSnapshot -> {
                                        for (QueryDocumentSnapshot likedDoc : likesSnapshot) {
                                            String likedUserId = likedDoc.getId();
                                            if (likedUserId.equals(currentUserId)) {
                                                Log.d(TAG, swiperId + " đã thích bạn.");
                                                likedYouUids.add(swiperId);
                                            }
                                        }
                                    });
                            tasks.add(task);
                        }

                        Tasks.whenAllComplete(tasks).addOnCompleteListener(doneTask -> {
                            Log.d(TAG, "loadUsersWhoLikedMe: Tổng số người đã thích bạn: " + likedYouUids.size());
                            listener.onLikedYouLoaded(likedYouUids);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi lấy danh sách swiperId", e);
                    listener.onLikedYouLoaded(new ArrayList<>());
                });
    }

    public interface OnLikedYouLoadedListener {
        void onLikedYouLoaded(List<String> likedYouUids);
    }

    private void removeProfileByUid(String uid) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getUid().equals(uid)) {
                userList.remove(i);
                adapter.notifyItemRemoved(i);
                adapter.notifyItemRangeChanged(i, userList.size()); // đảm bảo sync stack
                Log.d(TAG, "Removed user with uid: " + uid + " from userList at position: " + i);
                break;
            }
        }

        // Nếu hết hồ sơ thì hiện thông báo
        if (userList.isEmpty()) {
            showNoProfilesMessage(true, "Bạn đã xem hết tất cả các hồ sơ hiện có!");
            setSwipeButtonsVisibility(false);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//         XỬ LÝ KHI NHẬN KẾT QUẢ TỪ FILTER
        if (requestCode == REQUEST_CODE_FILTER && resultCode == RESULT_OK && data != null) {
            filterGender = data.getStringExtra(FilterActivity.EXTRA_FILTER_GENDER);
            filterMinAge = data.getIntExtra(FilterActivity.EXTRA_FILTER_MIN_AGE, 0);
            filterMaxAge = data.getIntExtra(FilterActivity.EXTRA_FILTER_MAX_AGE, 0);
            filterMinHeight = data.getIntExtra(FilterActivity.EXTRA_FILTER_MIN_HEIGHT, 0);
            filterMaxHeight = data.getIntExtra(FilterActivity.EXTRA_FILTER_MAX_HEIGHT, 0);
            filterMaxDistance = data.getIntExtra(FilterActivity.EXTRA_FILTER_MAX_DISTANCE, 1000);
            filterFavorites = data.getStringArrayListExtra(FilterActivity.EXTRA_FILTER_FAVORITES);
            if (filterFavorites == null) filterFavorites = new ArrayList<>();

            Log.d(TAG, "Filters updated from FilterActivity");

            // Làm mới userList và load lại hồ sơ theo bộ lọc mới
            userList = new ArrayList<>();
            userList.clear();
            if (adapter != null) {
                adapter.updateUsers(userList);
            }
            loadUsers();
        }

        if (requestCode == REQUEST_CODE_DETAIL && resultCode == RESULT_OK) {
            String userId = data.getStringExtra(ProfileDetailActivity.EXTRA_USER_ID);
            Log.d("dalayduoctudetail", userId);


            if (data != null && data.hasExtra(ProfileDetailActivity.EXTRA_USER_ID)) {
                String viewedUserId = data.getStringExtra(ProfileDetailActivity.EXTRA_USER_ID);
                if (!TextUtils.isEmpty(viewedUserId)) {
                    int topPosition = layoutManager.getTopPosition();

                    if (topPosition < userList.size()) {
                        User currentTopUser = userList.get(topPosition);
                        if (currentTopUser.getUid().equals(viewedUserId)) {
                            userList.remove(topPosition);
                            adapter.notifyItemRemoved(topPosition);
                            adapter.notifyItemRangeChanged(topPosition, userList.size());
                            Log.d(TAG, "Removed user at topPosition: " + topPosition + " with uid: " + viewedUserId);
                        } else {
                            Log.d(TAG, "TopPosition user UID does not match viewedUserId, skip removal.");
                        }
                    } else {
                        Log.d(TAG, "TopPosition out of range, skip removal.");
                    }

                    // Kiểm tra nếu hết hồ sơ thì hiện thông báo
                    if (userList.isEmpty()) {
                        showNoProfilesMessage(true, "Bạn đã xem hết tất cả các hồ sơ hiện có!");
                        setSwipeButtonsVisibility(false);
                    }
                }
            }
        }
    }


}

