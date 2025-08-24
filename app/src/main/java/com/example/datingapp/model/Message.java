package com.example.datingapp.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private String imageUrl;
    private @ServerTimestamp Date timestamp;
    private boolean isRead;
    public Message() {}

    // Constructor khi gửi tin nhắn văn bản
    public Message(String messageId, String senderId, String receiverId, String text, Date timestamp, boolean isRead) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Constructor khi gửi tin nhắn ảnh
    public Message(String messageId, String senderId, String receiverId, String text, String imageUrl, Date timestamp, boolean isRead) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
