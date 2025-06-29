package com.example.datingapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datingapp.adapter.MessageAdapter;
import com.example.datingapp.model.Message;
import com.google.firebase.auth.FirebaseAuth; // Cần FirebaseAuth
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange; // Import DocumentChange
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Import Query
import com.google.firebase.firestore.ServerTimestamp; // Import ServerTimestamp (nếu bạn dùng nó trong model)

import java.util.ArrayList;
import java.util.Date; // Import Date
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private Toolbar toolbarChat;
    private ImageView backButton;
    private CircleImageView chatPartnerAvatar;
    private TextView chatPartnerName;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageView buttonSendMessage;

    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private String chatPartnerUid;
    private String chatRoomId; // ID của phòng chat

    private String chatPartnerNameStr;
    private String chatPartnerAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        // Apply window insets (for status bar/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
//        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID
        currentUserId = "user_uid_123"; // Example current user
        chatPartnerUid = "user_uid_456"; // Example chat partner
        chatPartnerNameStr = "Nguyễn Thu Hà"; // Example chat partner name
        chatPartnerAvatarUrl = "https://res.cloudinary.com/dmmf5ximm/image/upload/v1750683632/ij7dfrykrwndutqk7edv.jpg"; // Example URL

        if (chatPartnerUid == null) {
            Log.e(TAG, "Chat partner UID is null. Finishing activity.");
            Toast.makeText(this, "Không tìm thấy thông tin người chat.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate consistent chat room ID
//        chatRoomId = getChatRoomId(currentUserId, chatPartnerUid);
//        Log.d(TAG, "ChatRoomId: " + chatRoomId);
        chatRoomId = "user_uid_123_user_uid_456"; // Example static chat room ID

        // Initialize views
        toolbarChat = findViewById(R.id.toolbar_chat);
        backButton = findViewById(R.id.back_button);
        chatPartnerAvatar = findViewById(R.id.chat_partner_avatar);
        chatPartnerName = findViewById(R.id.chat_partner_name);
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSendMessage = findViewById(R.id.button_send_message);

        // Update UI with chat partner info
        chatPartnerName.setText(chatPartnerNameStr != null ? chatPartnerNameStr : "Người dùng");
        if (chatPartnerAvatarUrl != null && !chatPartnerAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(chatPartnerAvatarUrl)
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .into(chatPartnerAvatar);
        } else {
            chatPartnerAvatar.setImageResource(R.drawable.bg_avatar_circle);
        }

        // Configure RecyclerView for messages
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(false);  // Messages appear in chronological order
        layoutManager.setStackFromEnd(false);   // Messages are not forced to stack from bottom
        recyclerViewMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(this, messageList, chatPartnerAvatarUrl);
        recyclerViewMessages.setAdapter(messageAdapter);

        // Handle back button click
        backButton.setOnClickListener(v -> finish());

        // Handle send message button click
        buttonSendMessage.setOnClickListener(v -> sendMessage());

        // Start listening for messages
        fetchMessages();
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();

        if (!TextUtils.isEmpty(messageText)) {
            // Create a new Message object
            String messageId = db.collection("chats").document(chatRoomId).collection("messages").document().getId(); // Auto-generated ID
            Message newMessage = new Message(
                    messageId,
                    currentUserId,
                    chatPartnerUid,
                    messageText,
                    new Date(), // Timestamp is set here, or use @ServerTimestamp in model
                    false // Not read yet by receiver
            );

            // Save message to Firestore
            db.collection("chats")
                    .document(chatRoomId)
                    .collection("messages")
                    .document(messageId)
                    .set(newMessage)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Message sent successfully!");
                        editTextMessage.setText(""); // Clear input field
                        // Update last message in chat document
                        updateLastMessageInChat(messageText, new Date());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sending message: ", e);
                        Toast.makeText(ChatActivity.this, "Không thể gửi tin nhắn.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Optional: Vibrate or show a subtle message if input is empty
            Toast.makeText(this, "Tin nhắn không được trống.", Toast.LENGTH_SHORT).show();
        }
    }

    // Function to update the last message in the main chat document
    private void updateLastMessageInChat(String lastMessageText, Date timestamp) {
        // Create a map for the last message
        // Note: For simplicity, we are setting "lastMessage" field directly here.
        // A more robust solution might use a map for lastMessage to include sender, etc.
        db.collection("chats")
                .document(chatRoomId)
                .update("lastMessageText", lastMessageText,
                        "lastMessageSenderId", currentUserId,
                        "timestamp", timestamp)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Last message updated in chat document."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last message in chat document: ", e));
    }

    private void fetchMessages() {
        db.collection("chats")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING) // Order messages by time
                .addSnapshotListener((snapshots, e) -> { // Real-time listener
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        Toast.makeText(ChatActivity.this, "Lỗi khi tải tin nhắn.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    // A new message has been added
                                    Message message = dc.getDocument().toObject(Message.class);

                                    // Log the message data here
                                    Log.d(TAG, "New message added: " +
                                            "Sender ID: " + message.getSenderId() +
                                            ", Text: " + message.getText() +
                                            ", Timestamp: " + message.getTimestamp());

                                    // Add the message to the list and notify the adapter
                                    messageList.add(message);
                                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                                    recyclerViewMessages.scrollToPosition(messageList.size() - 1); // Scroll to bottom
                                    break;
                                case MODIFIED:
                                    // A message has been modified (e.g., isRead status changed)
                                    Log.d(TAG, "Message modified: " + dc.getDocument().getId());
                                    break;
                                case REMOVED:
                                    // A message has been removed
                                    Log.d(TAG, "Message removed: " + dc.getDocument().getId());
                                    break;
                            }
                        }
                    }
                });
    }


}


