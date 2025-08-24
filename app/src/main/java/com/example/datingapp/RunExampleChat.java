package com.example.datingapp;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class RunExampleChat extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_run_example_chat);

        db = FirebaseFirestore.getInstance();

        String currentUserId = "CQZSXRnrrqPfyDffAyceR2LE1GI3"; // UID người test
        String swiperId = "5hLgv8298LbkRKKoer1oPRK8qKJ2";    // UID người muốn kiểm tra


        getAllSwiperIds(swiperIds -> {
            Log.d("debug123", "Tổng swiperId lấy được: " + swiperIds.size());
            for (String id : swiperIds) {
                Log.d("debug123", "SwiperId: " + id);
            }
        });


    }

    private void getAllLikesFromSwiper(String swiperId, String currentUserId, OnCheckLikeListener listener) {
        db.collection("swipes")
                .document(swiperId)
                .collection("likes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("AllLikes", "--- Likes by " + swiperId + " ---");
                    boolean check = false;
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("AllLikes", "Không có lượt thích nào từ " + swiperId);
                    } else {
                        for (QueryDocumentSnapshot likedDoc : queryDocumentSnapshots) {
                            String likedUserId = likedDoc.getId();
                            Log.d("AllLikes", "Người được thích bởi " + swiperId + ": " + likedUserId);
                            if (likedUserId.equals(currentUserId)) {
                                check = true;
                                break;
                            }
                        }
                    }
                    listener.onResult(check, swiperId);
                })
                .addOnFailureListener(e -> {
                    Log.e("AllLikes", "Lỗi khi lấy tất cả lượt thích từ " + swiperId, e);
                    listener.onResult(false, swiperId);
                });
    }


    private void getAllSwiperIds(OnSwiperIdsLoadedListener listener) {
        db.collection("swipes")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d("FirestoreTest", "Không có dữ liệu trong swipes");
                    } else {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Log.d("FirestoreTest", "Document ID: " + doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreTest", "Lỗi Firestore: ", e);
                });

    }

    public interface OnSwiperIdsLoadedListener {
        void onSwiperIdsLoaded(List<String> swiperIds);
    }

    public interface OnCheckLikeListener {
        void onResult(boolean isLiked, String swiperId);
    }


}
