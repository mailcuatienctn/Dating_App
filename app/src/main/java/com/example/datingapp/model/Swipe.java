package com.example.datingapp.model;

public class Swipe {
    private String fromUserId;
    private String toUserId;
    private boolean liked;

    public Swipe() {}

    public Swipe(String fromUserId, String toUserId, boolean liked) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.liked = liked;
    }

    // Getter + Setter
}

