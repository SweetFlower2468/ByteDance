package com.edu.neu.homework02.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private String email;       // 主键
    private String password;
    private String username;
    private String signature;   // 个性签名
    private String avatarUri;   // 头像地址或Base64字符串

    // --- Parcelable Implementation ---
    protected User(Parcel in) {
        email = in.readString();
        password = in.readString();
        username = in.readString();
        signature = in.readString();
        avatarUri = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(password);
        dest.writeString(username);
        dest.writeString(signature);
        dest.writeString(avatarUri);
    }

    public User() {
    }

    public User(String email, String password, String username, String signature, String avatarUri) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.signature = signature;
        this.avatarUri = avatarUri;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public void setAvatarUri(String avatarUri) {
        this.avatarUri = avatarUri;
    }
}
