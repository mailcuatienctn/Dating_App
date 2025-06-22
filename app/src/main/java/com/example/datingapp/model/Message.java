package com.example.datingapp.model;

public class Message {
    private String content;
    private boolean isSentByUser;

    public Message(String content, boolean isSentByUser) {
        this.content = content;
        this.isSentByUser = isSentByUser;
    }

    public String getContent() { return content; }
    public boolean isSentByUser() { return isSentByUser; }
}

