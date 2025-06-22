package com.example.datingapp.model;

import java.util.List;

public class User {
    private String uid;
    private String name;
    private String gender;
    private String bio;
    private int age; // năm sinh
    private List<String> favorites;
    private List<String> imgUrls;
    private Double latitude;
    private Double longitude;

    // Bắt buộc phải có constructor rỗng cho Firestore
    public User() {}

    public User(String name, String gender, String bio, int age, List<String> favorites,
                List<String> imgUrls, Double latitude, Double longitude) {
        this.name = name;
        this.gender = gender;
        this.bio = bio;
        this.age = age;
        this.favorites = favorites;
        this.imgUrls = imgUrls;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public User(String uid, String name, String gender, String bio, int age, List<String> favorites, List<String> imgUrls, Double latitude, Double longitude) {
        this.uid = uid;
        this.name = name;
        this.gender = gender;
        this.bio = bio;
        this.age = age;
        this.favorites = favorites;
        this.imgUrls = imgUrls;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // Getter & Setter (bắt buộc nếu dùng Firestore get/set)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public List<String> getFavorites() { return favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites; }

    public List<String> getImgUrls() { return imgUrls; }
    public void setImgUrls(List<String> imgUrls) { this.imgUrls = imgUrls; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", bio='" + bio + '\'' +
                ", age=" + age +
                ", favorites=" + favorites +
                ", imgUrls=" + imgUrls +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
