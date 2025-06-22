package com.example.datingapp.model;

public class FavoriteCategorie {
    private String name;
    private int iconResId; // ID của icon (R.drawable.xxx)

    public FavoriteCategorie(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}
