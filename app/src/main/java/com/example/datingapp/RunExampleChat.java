package com.example.datingapp;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.type.DateTime;

import java.util.HashMap;
import java.util.Map;

public class RunExampleChat extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_run_example_chat);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference chatRef = db.collection("chats").document("user_uid_123_user_uid_456");

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("user1Id", "user_uid_123");
        chatData.put("user2Id", "user_uid_456");
        Map<String, Object> lastMessage = new HashMap<>();
        lastMessage.put("senderId", "user_uid_123");
        lastMessage.put("text", "Chào bạn!");
        DateTime Timestamp;
        lastMessage.put("timestamp", FieldValue.serverTimestamp());
        chatData.put("lastMessage", lastMessage);
        chatData.put("timestamp", FieldValue.serverTimestamp());

        chatRef.set(chatData).addOnSuccessListener(aVoid -> {
            CollectionReference messagesRef = chatRef.collection("messages");
            Map<String, Object> msg1 = new HashMap<>();
            msg1.put("senderId", "user_uid_123");
            msg1.put("receiverId", "user_uid_456");
            msg1.put("text", "Chào bạn!");
            msg1.put("timestamp", FieldValue.serverTimestamp());
            msg1.put("isRead", true);
            messagesRef.document("msg_id_1").set(msg1);

            Map<String, Object> msg2 = new HashMap<>();
            msg2.put("senderId", "user_uid_456");
            msg2.put("receiverId", "user_uid_123");
            msg2.put("text", "Mình cũng chào bạn! Rất vui được match.");
            msg2.put("timestamp", FieldValue.serverTimestamp());
            msg2.put("isRead", false);
            messagesRef.document("msg_id_2").set(msg2);

            Log.d("Firestore", "✅ Chat data imported successfully.");
        });

    }
}