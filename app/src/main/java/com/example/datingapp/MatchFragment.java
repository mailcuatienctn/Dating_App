package com.example.datingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.datingapp.adapter.MatchesAdapter;
import com.example.datingapp.adapter.ChatAdapter;
import com.example.datingapp.model.Match;
import com.example.datingapp.model.ChatPreview;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import java.util.*;

public class MatchFragment extends Fragment implements MatchesAdapter.OnItemClickListener, ChatAdapter.OnChatClickListener {

    private RecyclerView recyclerViewMatches, recyclerViewChats;
    private MatchesAdapter matchesAdapter;
    private ChatAdapter chatAdapter;
    private List<Match> matchList = new ArrayList<>();
    private List<ChatPreview> chatList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView textNoMatches, textNoChats;
    private final Map<String, ChatPreview> chatPreviewMap = new HashMap<>();
    private ListenerRegistration matchesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_match, container, false);

        recyclerViewMatches = view.findViewById(R.id.recycler_view_new_matches);
        recyclerViewChats = view.findViewById(R.id.recycler_view_chats);
        textNoMatches = view.findViewById(R.id.text_no_new_matches);
        textNoChats = view.findViewById(R.id.text_no_chats);

        db = FirebaseFirestore.getInstance();

        recyclerViewMatches.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));

        matchesAdapter = new MatchesAdapter(getContext(), matchList, this);
        chatAdapter = new ChatAdapter(getContext(), chatList, this);

        recyclerViewMatches.setAdapter(matchesAdapter);
        recyclerViewChats.setAdapter(chatAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        listenForMatches();
        fetchChats();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (matchesListener != null) {
            matchesListener.remove();
        }
    }

    private void listenForMatches() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", getContext().MODE_PRIVATE);
        String currentUserId = prefs.getString("uid", null);
        if (currentUserId == null) return;

        if (matchesListener != null) matchesListener.remove();

        db.collection("profiles").document(currentUserId).get().addOnSuccessListener(snapshot -> {
            final List<String> blockedUsers = Optional.ofNullable((List<String>) snapshot.get("blockedUsers")).orElse(new ArrayList<>());

            matchesListener = db.collection("matches")
                    .document(currentUserId)
                    .collection("matchedWith")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w("MatchFragment", "listenForMatches error", e);
                            return;
                        }
                        if (snapshots != null) {
                            matchList.clear();
                            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                            List<String> uids = new ArrayList<>();

                            for (DocumentSnapshot doc : snapshots) {
                                String uid = doc.getId();
                                if (!blockedUsers.contains(uid)) {
                                    uids.add(uid);
                                    tasks.add(db.collection("profiles").document(uid).get());
                                }
                            }

                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                for (int i = 0; i < results.size(); i++) {
                                    DocumentSnapshot profileDoc = (DocumentSnapshot) results.get(i);
                                    String uid = uids.get(i);
                                    if (profileDoc.exists()) {
                                        String name = profileDoc.getString("name");
                                        List<String> imgs = (List<String>) profileDoc.get("imgUrls");
                                        String avatar = (imgs != null && !imgs.isEmpty()) ? imgs.get(0) : null;
                                        matchList.add(new Match(uid, name, avatar));
                                    }
                                }
                                matchesAdapter.notifyDataSetChanged();
                                updateMatchesVisibility();
                            });
                        }
                    });
        });
    }

    private void fetchChats() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", getContext().MODE_PRIVATE);
        String currentUserId = prefs.getString("uid", null);
        if (currentUserId == null) return;

        db.collection("profiles").document(currentUserId).get().addOnSuccessListener(snapshot -> {
            final List<String> blockedUsers = Optional.ofNullable((List<String>) snapshot.get("blockedUsers")).orElse(new ArrayList<>());

            db.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w("MatchFragment", "fetchChats error", e);
                            return;
                        }
                        if (snapshots != null) {
                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                DocumentSnapshot doc = dc.getDocument();
                                String chatId = doc.getId();
                                String lastMessage = doc.getString("lastMessage");
                                Date timestampDate = doc.getDate("timestamp");
                                long timestamp = (timestampDate != null) ? timestampDate.getTime() / 1000 : 0;
                                List<String> participants = (List<String>) doc.get("participants");

                                String partnerId = participants.stream().filter(id -> !id.equals(currentUserId)).findFirst().orElse(null);
                                if (partnerId == null || blockedUsers.contains(partnerId)) continue;

                                db.collection("profiles").document(partnerId).get().addOnSuccessListener(profileDoc -> {
                                    if (profileDoc.exists()) {
                                        String name = profileDoc.getString("name");
                                        List<String> imgs = (List<String>) profileDoc.get("imgUrls");
                                        String avatarUrl = (imgs != null && !imgs.isEmpty()) ? imgs.get(0) : null;

                                        ChatPreview chat = new ChatPreview(chatId, partnerId, name, avatarUrl, lastMessage, timestamp);
                                        if (chatPreviewMap.containsKey(chatId)) {
                                            int index = findChatIndexById(chatId);
                                            chatList.set(index, chat);
                                            chatAdapter.notifyItemChanged(index);
                                        } else {
                                            chatList.add(chat);
                                            chatPreviewMap.put(chatId, chat);
                                            chatAdapter.notifyItemInserted(chatList.size() - 1);
                                        }
                                        updateChatsVisibility();
                                    }
                                });
                            }
                        }
                    });
        });
    }

    private int findChatIndexById(String chatId) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getChatId().equals(chatId)) {
                return i;
            }
        }
        return -1;
    }

    private void updateMatchesVisibility() {
        if (matchList.isEmpty()) {
            textNoMatches.setVisibility(View.VISIBLE);
            recyclerViewMatches.setVisibility(View.GONE);
        } else {
            textNoMatches.setVisibility(View.GONE);
            recyclerViewMatches.setVisibility(View.VISIBLE);
        }
    }

    private void updateChatsVisibility() {
        if (chatList.isEmpty()) {
            textNoChats.setVisibility(View.VISIBLE);
            recyclerViewChats.setVisibility(View.GONE);
        } else {
            textNoChats.setVisibility(View.GONE);
            recyclerViewChats.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(Match match) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("matchId", match.getUidMatcher());
        intent.putExtra("matchName", match.getNameMatcher());
        intent.putExtra("matchAvatarUrl", match.getUrlAvartar());
        startActivity(intent);
    }

    @Override
    public void onChatClick(ChatPreview chat) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("matchId", chat.getParticipantId());
        intent.putExtra("matchName", chat.getParticipantName());
        intent.putExtra("matchAvatarUrl", chat.getParticipantAvatarUrl());
        startActivity(intent);
    }
}
