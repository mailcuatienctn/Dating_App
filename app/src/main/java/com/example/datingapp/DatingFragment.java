package com.example.datingapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Thêm import TextView nếu cần hiển thị thông báo "No likes"
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datingapp.adapter.LikerAdapter;
import com.example.datingapp.model.Liker;
import com.google.firebase.Timestamp; // Sử dụng Timestamp từ Firebase
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DatingFragment extends Fragment implements LikerAdapter.OnItemClickListener {

    private static final String TAG = "DatingFragment";

    private FirebaseFirestore db;
    private ListenerRegistration incomingLikesListener;
    private SharedPreferences sharedPreferences;
    private String currentUserId;

    // SharedPreferences keys
    public static final String PREF_NAME = "DatingAppPrefs";
    public static final String KEY_UNREAD_LIKES_COUNT = "unread_likes_count";

    // For RecyclerView
    private RecyclerView likersRecyclerView;
    private LikerAdapter likerAdapter;
    private List<Liker> likerList;
    private TextView tvNoLikersFound; // Để hiển thị thông báo khi không có likers

    public DatingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Lấy UID từ SharedPreferences (đã lưu khi đăng nhập)
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("uid", null);
        if (currentUserId == null) {
            Log.e(TAG, "Current User ID is null. Cannot proceed with Firebase operations.");
            // Có thể hiển thị Toast hoặc chuyển hướng người dùng nếu UID không có
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem lượt thích.", Toast.LENGTH_LONG).show();
            // TODO: Chuyển hướng người dùng về LoginActivity hoặc một màn hình phù hợp
        } else {
            Log.d(TAG, "Current User ID: " + currentUserId);
        }

        // SharedPreferences để lưu số lượng likes chưa đọc
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Khởi tạo danh sách cho RecyclerView
        likerList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dating, container, false);

        // Ánh xạ RecyclerView và cài đặt LayoutManager & Adapter
        likersRecyclerView = view.findViewById(R.id.likersRecyclerView);
        likersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        likerAdapter = new LikerAdapter(getContext(), likerList, this);
        likersRecyclerView.setAdapter(likerAdapter);

        // Ánh xạ TextView thông báo
//        tvNoLikersFound = view.findViewById(R.id.tvNoLikersFound); // Đảm bảo ID này có trong fragment_dating.xml

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setupIncomingLikesListener(); // Vẫn giữ listener để cập nhật badge và realtime list
        resetUnreadLikesCount(); // Reset badge khi người dùng vào tab
    }

    @Override
    public void onStop() {
        super.onStop();
        if (incomingLikesListener != null) {
            incomingLikesListener.remove();
            Log.d(TAG, "Incoming likes listener has been removed.");
        }
    }

    /**
     * Lắng nghe các lượt thích mới đến trong thời gian thực.
     * Khi có lượt thích mới, sẽ cập nhật badge và hiển thị Toast.
     * Cũng sẽ kích hoạt loadLikers() để cập nhật danh sách hiển thị.
     */
    private void setupIncomingLikesListener() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot set up likes listener: currentUserId is null");
            return;
        }

        // Loại bỏ listener cũ nếu có, để tránh tạo nhiều listener
        if (incomingLikesListener != null) {
            incomingLikesListener.remove();
        }

        incomingLikesListener = db.collection("profiles")
                .document(currentUserId)
                .collection("likes_received")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Error listening for likes:", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String likerUid = dc.getDocument().getId();
                                Timestamp likeTimestamp = dc.getDocument().getTimestamp("timestamp"); // Lấy timestamp của lượt thích
                                Log.d(TAG, "New like from UID: " + likerUid + " at " + likeTimestamp);

                                incrementUnreadLikesCount(); // Cập nhật badge

                                // Lấy thông tin người thích và hiển thị Toast
                                db.collection("profiles").document(likerUid).get()
                                        .addOnSuccessListener(profileDoc -> {
                                            if (profileDoc.exists()) {
                                                String name = profileDoc.getString("name");
                                                if (name != null && getContext() != null) {
                                                    Toast.makeText(getContext(),
                                                            name + " đã thích bạn! ❤",
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(error ->
                                                Log.e(TAG, "Failed to fetch liker info for Toast: " + error.getMessage()));
                            }
                            // Bạn có thể xử lý DocumentChange.Type.MODIFIED và DocumentChange.Type.REMOVED nếu cần
                        }
                        // Luôn tải lại toàn bộ danh sách để đảm bảo tính nhất quán sau mỗi thay đổi
                        // (Thêm, Xóa, Sửa - nếu bạn xử lý các loại thay đổi đó)
                        loadLikers();
                    }
                });

        Log.d(TAG, "Listener for incoming likes has been set up.");
    }

    /**
     * Tải danh sách người đã thích bạn từ Firestore.
     * Cập nhật RecyclerView và hiển thị/ẩn thông báo "No likers".
     */
    private void loadLikers() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot load likers: currentUserId is null");
//            tvNoLikersFound.setVisibility(View.VISIBLE); // Hiển thị thông báo
            likersRecyclerView.setVisibility(View.GONE); // Ẩn RecyclerView
            return;
        }

        db.collection("profiles")
                .document(currentUserId)
                .collection("likes_received")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sắp xếp theo thời gian mới nhất
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Liker> newLikers = new ArrayList<>();
                    List<String> likerUidsToFetch = new ArrayList<>();
                    // Map để lưu trữ timestamp của mỗi lượt thích theo UID
                    final java.util.Map<String, Timestamp> likerTimestamps = new java.util.HashMap<>();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String likerUid = document.getId();
                            Timestamp likeTimestamp = document.getTimestamp("timestamp"); // Lấy timestamp
                            likerUidsToFetch.add(likerUid);
                            if (likeTimestamp != null) {
                                likerTimestamps.put(likerUid, likeTimestamp);
                            }
                        }
                        Log.d(TAG, "Found " + likerUidsToFetch.size() + " liker UIDs from likes_received.");

                        // Lấy thông tin chi tiết của từng người thích từ collection "profiles"
                        if (!likerUidsToFetch.isEmpty()) {
                            // Firestore không có truy vấn "WHERE IN" trực tiếp cho collection.
                            // Cách hiệu quả nhất cho số lượng lớn UID là dùng array-contains-any hoặc nhiều get()
                            // Ở đây, chúng ta sẽ thực hiện nhiều get() và chờ tất cả hoàn thành.
                            final int[] fetchedCount = {0}; // Đếm số lượng liker đã fetch profile
                            for (String likerUid : likerUidsToFetch) {
                                db.collection("profiles").document(likerUid).get()
                                        .addOnSuccessListener(profileDoc -> {
                                            if (profileDoc.exists()) {
                                                String name = profileDoc.getString("name");
                                                List<String> imgUrls = (List<String>) profileDoc.get("imgUrls");
                                                String avatarUrl = null;
                                                if (imgUrls != null && !imgUrls.isEmpty()) {
                                                    avatarUrl = imgUrls.get(0);
                                                }
                                                Timestamp date = likerTimestamps.get(likerUid); // Lấy timestamp đã lưu

                                                Liker liker = new Liker(likerUid, name, date, avatarUrl);
                                                newLikers.add(liker);
                                            } else {
                                                Log.w(TAG, "Profile not found for liker UID: " + likerUid);
                                            }
                                            fetchedCount[0]++; // Tăng bộ đếm

                                            // Khi tất cả profile đã được fetch
                                            if (fetchedCount[0] == likerUidsToFetch.size()) {
                                                likerList.clear();
                                                likerList.addAll(newLikers);
                                                likerAdapter.notifyDataSetChanged();
                                                Log.d(TAG, "Loaded " + likerList.size() + " likers into RecyclerView.");
                                                updateUiVisibility(); // Cập nhật UI
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to fetch profile for liker UID: " + likerUid, e);
                                            fetchedCount[0]++; // Vẫn tăng bộ đếm ngay cả khi lỗi
                                            if (fetchedCount[0] == likerUidsToFetch.size()) {
                                                updateUiVisibility(); // Cập nhật UI
                                            }
                                        });
                            }
                        } else {
                            likerList.clear();
                            likerAdapter.notifyDataSetChanged();
                            updateUiVisibility(); // Cập nhật UI
                            Log.d(TAG, "No liker UIDs to fetch profiles for.");
                        }

                    } else {
                        likerList.clear();
                        likerAdapter.notifyDataSetChanged();
                        updateUiVisibility(); // Cập nhật UI
                        Log.d(TAG, "No likes received in total.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading likers: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Lỗi tải danh sách người thích bạn.", Toast.LENGTH_SHORT).show();
                    updateUiVisibility(); // Cập nhật UI ngay cả khi có lỗi
                });
    }

    /**
     * Cập nhật trạng thái hiển thị của RecyclerView và TextView thông báo.
     */
    private void updateUiVisibility() {
        if (likerList.isEmpty()) {
//            tvNoLikersFound.setVisibility(View.VISIBLE);
            likersRecyclerView.setVisibility(View.GONE);
        } else {
//            tvNoLikersFound.setVisibility(View.GONE);
            likersRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void incrementUnreadLikesCount() {
        if (sharedPreferences != null) {
            int current = sharedPreferences.getInt(KEY_UNREAD_LIKES_COUNT, 0);
            int updated = current + 1;
            sharedPreferences.edit().putInt(KEY_UNREAD_LIKES_COUNT, updated).apply();
            Log.d(TAG, "Unread likes count updated to: " + updated);
            notifyActivityToUpdateBadge(updated);
        }
    }

    private void resetUnreadLikesCount() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putInt(KEY_UNREAD_LIKES_COUNT, 0).apply();
            Log.d(TAG, "Unread likes count reset to 0");
            notifyActivityToUpdateBadge(0);
        }
    }

    public interface OnBadgeUpdateListener {
        void onUpdateBadge(int count);
    }

    private OnBadgeUpdateListener badgeUpdateListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBadgeUpdateListener) {
            badgeUpdateListener = (OnBadgeUpdateListener) context;
        } else {
            Log.e(TAG, context.toString() + " must implement OnBadgeUpdateListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        badgeUpdateListener = null;
    }

    private void notifyActivityToUpdateBadge(int count) {
        if (badgeUpdateListener != null) {
            badgeUpdateListener.onUpdateBadge(count);
        }
    }

    @Override
    public void onItemClick(Liker liker) {
        if (liker != null && getContext() != null) {
            Intent intent = new Intent(getContext(), ProfileDetailActivity.class);
            // Truyền UID của người thích qua Intent
            intent.putExtra(ProfileDetailActivity.EXTRA_USER_ID, liker.getUid());
            // Mở ProfileDetailActivity
            startActivity(intent);
        }
    }

}