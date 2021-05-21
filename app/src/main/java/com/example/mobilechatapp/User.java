package com.example.mobilechatapp;

import android.bluetooth.BluetoothDevice;

public class User {
    private final String id;
    private final BluetoothDevice device;

    public User(String id, BluetoothDevice device) {
        this.id = id;
        this.device = device;
    }

    public String getId() {
        return id;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String toString() {
        return id + ":" + device.getAddress();
    }

    public boolean equals(User user) {
        if ( user.getId().equals(id) && user.getDevice().equals(device) )
            return true;

        else
            return false;
    }

    public boolean sameMac(BluetoothDevice device) {
        return device != null && device.getAddress().equals(device.getAddress());
    }
}
