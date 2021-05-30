package com.example.mobilechatapp;

import com.example.mobilechatapp.Model.UserChat;

public class MessageInfo {
    private final UserChat fromUser;
    private final UserChat toUser;
    private final String content;

    public MessageInfo(UserChat fromUser, UserChat toUser, String content) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public UserChat getFromUser() {
        return fromUser;
    }

    public UserChat getToUser() {
        return toUser;
    }
}
