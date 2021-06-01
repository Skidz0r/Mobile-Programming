package com.example.mobilechatapp.Information;

import android.bluetooth.BluetoothDevice;

/**
 * Object is used to identify bluetooth connections
 */
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
        return user.getId().equals(id) && user.getDevice().equals(device);
    }

    public boolean sameMac(BluetoothDevice device) {
        return device != null && device.getAddress().equals(device.getAddress());
    }
}
