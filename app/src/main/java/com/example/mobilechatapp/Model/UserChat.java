package com.example.mobilechatapp.Model;

import android.bluetooth.BluetoothDevice;

public class UserChat {
    private final String id;
    private final BluetoothDevice device;

    public UserChat(String id, BluetoothDevice device) {
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

    public boolean equals(UserChat user) {
        if (user.getId().equals(id) && user.getDevice().equals(device))
            return true;

        else
            return false;
    }

    public boolean sameMac(BluetoothDevice device) {
        return device != null && device.getAddress().equals(device.getAddress());
    }
}
