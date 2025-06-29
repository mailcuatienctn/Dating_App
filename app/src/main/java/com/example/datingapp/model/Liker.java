package com.example.datingapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

public class Liker implements Parcelable {
    private String uid;
    private String name;
    private Timestamp date;
    private String avatarUrl;

    public Liker() {
    }

    public Liker(String uid, String name, Timestamp date, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.date = date;
        this.avatarUrl = avatarUrl;
    }

    // Parcelable constructor
    protected Liker(Parcel in) {
        uid = in.readString();
        name = in.readString();
        avatarUrl = in.readString();
        long timeMillis = in.readLong();
        date = new Timestamp(new java.util.Date(timeMillis));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(name);
        dest.writeString(avatarUrl);
        dest.writeLong(date != null ? date.toDate().getTime() : 0L);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Liker> CREATOR = new Creator<Liker>() {
        @Override
        public Liker createFromParcel(Parcel in) {
            return new Liker(in);
        }

        @Override
        public Liker[] newArray(int size) {
            return new Liker[size];
        }
    };

    // Getters and setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
