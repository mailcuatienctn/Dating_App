package com.example.datingapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.datingapp.adapter.MessageAdapter;
import com.example.datingapp.model.Message;
import com.example.datingapp.model.VideoCallRequest;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final int REQUEST_PICK_IMAGE = 1000;

    private ImageView backButton, buttonSendMessage, buttonSendImage, buttonVideoCall, buttonBlock;
    private CircleImageView chatPartnerAvatar;
    private TextView chatPartnerName;
    private EditText editTextMessage;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId, currentUserName, currentUserAvatarUrl;
    private String chatPartnerUid, chatPartnerAvatarUrl, chatPartnerNameStr, chatRoomId;
    private ProgressDialog progressDialog;
    private boolean isBlockedByPartner = false;
    private static final String RAILWAY_API_URL = "https://fcm-message-production.up.railway.app/send-notification";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.back_button);
        buttonSendMessage = findViewById(R.id.button_send_message);
        buttonSendImage = findViewById(R.id.button_send_image);
        buttonVideoCall = findViewById(R.id.button_video_call);
        buttonBlock = findViewById(R.id.button_block);
        chatPartnerAvatar = findViewById(R.id.chat_partner_avatar);
        chatPartnerName = findViewById(R.id.chat_partner_name);
        editTextMessage = findViewById(R.id.edit_text_message);
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("uid", null);

        chatPartnerUid = getIntent().getStringExtra("matchId");
        chatPartnerNameStr = getIntent().getStringExtra("matchName");
        chatPartnerAvatarUrl = getIntent().getStringExtra("matchAvatarUrl");

        chatRoomId = currentUserId.compareTo(chatPartnerUid) < 0 ? currentUserId + "_" + chatPartnerUid : chatPartnerUid + "_" + currentUserId;

        chatPartnerName.setText(chatPartnerNameStr);
        Glide.with(this).load(chatPartnerAvatarUrl).placeholder(R.drawable.bg_avatar_circle).into(chatPartnerAvatar);

        messageAdapter = new MessageAdapter(this, messageList, currentUserId, chatPartnerAvatarUrl);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        backButton.setOnClickListener(v -> finish());
        buttonSendMessage.setOnClickListener(v -> {
            if (!isBlockedByPartner) {
                sendTextMessage();
            } else {
                Toast.makeText(this, "Bạn đã bị chặn, không thể nhắn tin.", Toast.LENGTH_SHORT).show();
            }
        });
        buttonSendImage.setOnClickListener(v -> {
            if (!isBlockedByPartner) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Bạn đã bị chặn, không thể gửi ảnh.", Toast.LENGTH_SHORT).show();
            }
        });
        buttonVideoCall.setOnClickListener(v -> {
            if (!isBlockedByPartner) {
                initiateVideoCall();
            } else {
                Toast.makeText(this, "Bạn đã bị chặn, không thể gọi video.", Toast.LENGTH_SHORT).show();
            }
        });
        buttonBlock.setOnClickListener(v -> showBlockConfirmationDialog());
        chatPartnerAvatar.setOnClickListener(v -> openUserProfile());

        checkIfUserBlocked();
        checkIfBlockedByReceiver();
        fetchCurrentUserInfo(currentUserId);
        fetchMessages();
    }

    private void checkIfUserBlocked() {
        db.collection("blocked_users")
                .document(currentUserId)
                .collection("users")
                .document(chatPartnerUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Toast.makeText(this, "Bạn đã chặn người này, không thể nhắn tin.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void checkIfBlockedByReceiver() {
        db.collection("blocked_users")
                .document(chatPartnerUid)
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        isBlockedByPartner = true;
                        Toast.makeText(this, "Bạn đã bị người này chặn, không thể nhắn tin.", Toast.LENGTH_LONG).show();
                        buttonSendMessage.setEnabled(false);
                        buttonSendImage.setEnabled(false);
                        buttonVideoCall.setEnabled(false);
                        editTextMessage.setEnabled(false);
                        chatPartnerName.setText(chatPartnerNameStr + " (Đã chặn bạn)");
                    }
                });
    }

    private void openUserProfile() {
        Intent intent = new Intent(this, ProfileDetailActivity.class);
        intent.putExtra(ProfileDetailActivity.EXTRA_USER_ID, chatPartnerUid);
        intent.putExtra("EXTRA_HIDE_LIKE_DISLIKE", true); // Ẩn nút Like / Dislike
        startActivity(intent);
    }


    private void showBlockConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chặn người dùng?")
                .setMessage("Bạn có chắc chắn muốn chặn " + chatPartnerNameStr + " không? Sau khi chặn, bạn sẽ không thể nhắn tin với họ.")
                .setPositiveButton("Chặn", (dialog, which) -> blockUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void blockUser() {
        Map<String, Object> data = new HashMap<>();
        data.put("blockedAt", new Date());

        db.collection("blocked_users")
                .document(currentUserId)
                .collection("users")
                .document(chatPartnerUid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã chặn người dùng.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Chặn người dùng thất bại.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "blockUser error: ", e);
                });
    }


    private void sendTextMessage() {
        String text = editTextMessage.getText().toString().trim();
        sendMessageNotificationToReceiver(text);
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
        String messageId = db.collection("chats").document(chatRoomId).collection("messages").document().getId();
        Message message = new Message(messageId, currentUserId, chatPartnerUid, text, null, new Date(), false);
        db.collection("chats").document(chatRoomId).collection("messages").document(messageId).set(message);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text);
        chatUpdate.put("timestamp", new Date()); // ✅ dùng Date() thay cho serverTimestamp để cập nhật realtime ngay
        chatUpdate.put("participants", Arrays.asList(currentUserId, chatPartnerUid));

        db.collection("chats").document(chatRoomId).set(chatUpdate, com.google.firebase.firestore.SetOptions.merge());

        editTextMessage.setText("");
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToCloudinary(imageUri);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        progressDialog = ProgressDialog.show(this, "Đang gửi ảnh", "Vui lòng chờ...");
        MediaManager.get().upload(imageUri).callback(new UploadCallback() {
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onSuccess(String requestId, Map resultData) {
                progressDialog.dismiss();
                String imageUrl = (String) resultData.get("secure_url");
                sendImageMessage(imageUrl);
            }
            @Override public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss();
                Toast.makeText(ChatActivity.this, "Gửi ảnh thất bại", Toast.LENGTH_SHORT).show();
            }
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void sendImageMessage(String imageUrl) {
        sendMessageNotificationToReceiver("Đã gửi một hình ảnh 📷");
        String messageId = db.collection("chats").document(chatRoomId).collection("messages").document().getId();
        Message message = new Message(messageId, currentUserId, chatPartnerUid, null, imageUrl, new Date(), false);
        db.collection("chats").document(chatRoomId).collection("messages").document(messageId).set(message);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", "Hình ảnh 📷");
        chatUpdate.put("timestamp", new Date()); // ✅ tương tự: Date() thay serverTimestamp
        chatUpdate.put("participants", Arrays.asList(currentUserId, chatPartnerUid));

        db.collection("chats").document(chatRoomId).set(chatUpdate, com.google.firebase.firestore.SetOptions.merge());
    }

    private void fetchMessages() {
        db.collection("chats").document(chatRoomId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching messages", error);
                        return;
                    }
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                messageList.add(message);
                                messageAdapter.notifyItemInserted(messageList.size() - 1);
                                recyclerViewMessages.post(() ->
                                        recyclerViewMessages.scrollToPosition(messageList.size() - 1)
                                );
                            }
                        }
                    }
                });
    }

    private void initiateVideoCall() {
        String channelName = generateChannelName();
        VideoCallRequest callRequest = new VideoCallRequest(currentUserId, currentUserName, currentUserAvatarUrl, chatPartnerUid, "calling", new Date());
        db.collection("video_calls").document(channelName).set(callRequest)
                .addOnSuccessListener(unused -> {
                    sendVideoCallNotification(channelName);
                    Intent intent = new Intent(this, VideoCallActivity.class);
                    intent.putExtra("channelName", channelName);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể thực hiện cuộc gọi.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "initiateVideoCall error: ", e);
                });
    }

    private void sendVideoCallNotification(String channelName) {
        db.collection("profiles").document(chatPartnerUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String token = snapshot.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            try {
                                JSONObject dataPayload = new JSONObject();
                                dataPayload.put("token", token);
                                dataPayload.put("callerId", currentUserId);
                                dataPayload.put("callerName", currentUserName);
                                dataPayload.put("callerAvatarUrl", currentUserAvatarUrl);
                                dataPayload.put("type", "video_call");
                                dataPayload.put("channelName", channelName);

                                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, RAILWAY_API_URL, dataPayload,
                                        response -> Log.d(TAG, "FCM sent via Railway: " + response),
                                        error -> Log.e(TAG, "Error sending FCM: ", error));
                                Volley.newRequestQueue(this).add(request);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON Exception in sendVideoCallNotification", e);
                            }
                        }
                    }
                });
    }

    private void sendMessageNotificationToReceiver(String messageContent) {
        db.collection("profiles").document(chatPartnerUid).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String token = snapshot.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            try {
                                JSONObject payload = new JSONObject();
                                payload.put("token", token);
                                payload.put("type", "new_message");
                                payload.put("senderId", currentUserId);
                                payload.put("senderName", currentUserName);
                                payload.put("senderAvatarUrl", currentUserAvatarUrl);
                                payload.put("messageText", messageContent);
                                payload.put("chatId", chatRoomId);   // Nội dung tin nhắn

                                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, RAILWAY_API_URL, payload,
                                        response -> Log.d(TAG, "FCM sent: " + response),
                                        error -> Log.e(TAG, "FCM error: ", error));

                                Volley.newRequestQueue(this).add(request);

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON error: ", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching token: ", e));
    }


    private String generateChannelName() {
        String userPart = currentUserId.substring(Math.max(0, currentUserId.length() - 4));
        String partnerPart = chatPartnerUid.substring(Math.max(0, chatPartnerUid.length() - 4));
        String timestampHex = Long.toHexString(System.currentTimeMillis()).toUpperCase();
        if (timestampHex.length() > 8) {
            timestampHex = timestampHex.substring(timestampHex.length() - 8);
        }
        return userPart + partnerPart + timestampHex;
    }

    private void fetchCurrentUserInfo(String uid) {
        db.collection("profiles").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                currentUserName = snapshot.getString("name");
                currentUserAvatarUrl = snapshot.getString("firstImg");
            } else {
                currentUserName = "User";
                currentUserAvatarUrl = null;
            }
        }).addOnFailureListener(e -> {
            currentUserName = "User";
            currentUserAvatarUrl = null;
        });
    }
}








