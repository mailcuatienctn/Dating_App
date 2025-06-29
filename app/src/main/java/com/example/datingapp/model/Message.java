package com.example.datingapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String messageId;   // ID duy nhất của tin nhắn (sẽ được Firestore tự tạo)
    private String senderId;    // UID của người gửi
    private String receiverId;  // UID của người nhận
    private String text;        // Nội dung tin nhắn
    private @ServerTimestamp Date timestamp; // Thời gian tin nhắn được gửi, tự động tạo bởi Firestore
    private boolean isRead;     // Trạng thái đã đọc hay chưa

    // Constructor rỗng bắt buộc cho Firestore
    public Message() {
    }

    public Message(String messageId, String senderId, String receiverId, String text, Date timestamp, boolean isRead) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters và Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}