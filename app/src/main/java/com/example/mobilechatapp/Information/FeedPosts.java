package com.example.mobilechatapp.Information;

import android.bluetooth.BluetoothDevice;

public class FeedPosts {

    private String username;
    private String Post;

    public FeedPosts(String username, String post) {
        this.username = username;
        this.Post = post;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPost() {
        return Post;
    }

    public void setPost(String post) {
        this.Post = post;
    }
}
