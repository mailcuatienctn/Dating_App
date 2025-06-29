package com.example.datingapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.datingapp.adapter.MatchesAdapter;
import com.example.datingapp.model.Match;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MatchFragment extends Fragment {

    private RecyclerView recyclerView;
    private MatchesAdapter matchesAdapter;
    private List<Match> matchList = new ArrayList<>();
    private FirebaseFirestore db;

    public MatchFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view_new_matches);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set up adapter
        matchesAdapter = new MatchesAdapter(getContext(), matchList);
        recyclerView.setAdapter(matchesAdapter);

        // Fetch data from Firestore
//        fetchMatches();

        return view;
    }

    private void fetchMatches() {
        // Lấy UID của người dùng hiện tại từ SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("uid", null);

        if (currentUserId == null) {
            Log.e("MatchFragment", "Current user ID is null");
            return;
        }

        db.collection("matches")
                .document(currentUserId)
                .collection("matchedWith")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            matchList.clear();  // Xóa danh sách cũ

                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                            List<String> matchedUids = new ArrayList<>();

                            // Duyệt các UID đã match
                            for (DocumentSnapshot document : querySnapshot) {
                                String matchedWithUid = document.getId();
                                matchedUids.add(matchedWithUid);
                                tasks.add(db.collection("profiles").document(matchedWithUid).get());
                            }

                            // Đợi tất cả truy vấn profile hoàn thành
                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                for (int i = 0; i < results.size(); i++) {
                                    DocumentSnapshot profileDoc = (DocumentSnapshot) results.get(i);
                                    String uid = matchedUids.get(i); // Lấy đúng UID theo thứ tự

                                    if (profileDoc != null && profileDoc.exists()) {
                                        String name = profileDoc.getString("name");

                                        List<String> images = (List<String>) profileDoc.get("imgUrls");
                                        String avatarUrl = (images != null && !images.isEmpty()) ? images.get(0) : null;

                                        Match match = new Match(uid, name, avatarUrl);
                                        matchList.add(match);
                                    } else {
                                        Log.w("MatchFragment", "Profile for " + uid + " not found.");
                                    }
                                }

                                matchesAdapter.notifyDataSetChanged(); // Cập nhật UI
                            }).addOnFailureListener(e -> {
                                Log.e("MatchFragment", "Error getting profiles: ", e);
                            });
                        } else {
                            Log.w("MatchFragment", "No matches found for user: " + currentUserId);
                        }
                    } else {
                        Log.e("MatchFragment", "Error getting matches: ", task.getException());
                    }
                });
    }

    @Override
    public void onResume(){
        super.onResume();
        fetchMatches();
    }

}
