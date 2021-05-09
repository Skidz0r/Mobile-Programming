package com.example.mobilechatapp;

public interface BluetoothState {
    static final short REGISTER_CLIENT = 0;
    static final short UNREGISTER_CLIENT = 1;
    static final short TEST_RECEIVE_MSG = 2;

    static final short BT_STATUS = 3;
    static final short BT_ON = 4;
    static final short BT_OFF = 5;
    static final short BT_ERROR = 6;
    static final short BT_DISCOVER_ON = 8;
    static final short BT_DISCOVER_OFF = 9;

    static final short BT_START_DISCOVERY = 10;
    static final short BT_END_DISCOVERY = 11;

    static final short BT_UNBOUND_DEVICE_FOUND = 12;
    static final short BT_DEVICE_BOUND = 13;
    static final short BT_DEVICE_UNBOUND = 14;

    static final short BT_GET_UNBOUNDED_DEVICE = 15;

    static final short BT_CREATE_BOUND = 16;

    static final short BT_GET_DEVICES = 17;
    static final short BT_DEVICE_FOUND = 18;

    static final short MESSAGE_READ = 19;
    static final short MESSAGE_WRITE = 20;

    static final short START_LISTENING = 21;
    static final short CONNECT = 22;
}
