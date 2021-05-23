package com.example.mobilechatapp;

import com.example.mobilechatapp.Model.User;

public class MessageInfo {
    private final User fromUser;
    private final User toUser;
    private final String content;

    public MessageInfo(User fromUser, User toUser, String content) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }
}
