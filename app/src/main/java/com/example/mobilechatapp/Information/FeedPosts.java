package com.example.mobilechatapp.Information;

import android.bluetooth.BluetoothDevice;

public class FeedPosts {

    private String Username;
    private String PostMessage;
    private String Hours;
    private String ImageUrl;

    public FeedPosts() {}

    public FeedPosts(String username, String postMessage, String hours, String imageUrl) {
        Username = username;
        PostMessage = postMessage;
        Hours = hours;
        ImageUrl = imageUrl;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getPostMessage() {
        return PostMessage;
    }

    public void setPostMessage(String postMessage) {
        PostMessage = postMessage;
    }

    public String getHours() {
        return Hours;
    }

    public void setHours(String hours) {
        Hours = hours;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }
}
