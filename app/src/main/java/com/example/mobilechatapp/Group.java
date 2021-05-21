package com.example.mobilechatapp;

import java.util.LinkedList;

public class Group{
    private final String id;

    private final LinkedList<User> userList;

    private Integer userCount = 0;

    private final LinkedList<MessageInfo> msgList;

    private static Integer groupCount = 0;

    public Group(String id, LinkedList<User> userList) {
        this.id = id;
        this.userList = userList;

        msgList = new LinkedList<>();

        groupCount++;
        userCount = userList.size();
    }

    public Group(String id, User user) {
        this.id = id;
        this.userList = new LinkedList<>();
        userList.add(user);

        msgList = new LinkedList<>();

        groupCount++;
        userCount = 1;
    }

    public static Integer getCount() {
        return groupCount;
    }

    public String getId() {
        return id;
    }

    public LinkedList<User> getUserList() {
        return userList;
    }

    public LinkedList<MessageInfo> getMsgList() {
        return msgList;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void incUserCount() {
        setUserCount(++userCount);
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }
}
