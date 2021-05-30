package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.Model.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class BluetoothChat extends AppCompatActivity {
    // Default android bluetooth adapter
    BluetoothAdapter btAdapter;
    // Holds a list of paired devices
    ArrayList<BluetoothDevice> btArrayDevice;

    /**
     * List of chat users
     */

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    BluetoothDevice device;


    private final static String TAG3 = "ItemClickListener";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();
        initiateRecyclerView();

    }

    /**
     * Get the info of the currently paired devices, into an array list
     */
    public void getPairedDevices() {
        Set<BluetoothDevice> temp = btAdapter.getBondedDevices();

        btArrayDevice = new ArrayList<>();
        btArrayDevice.addAll(temp);
    }

    /**
     * Method will initiate the necessary mumbo jumbo of the recycler view, it will
     * then create a list paired devices, ready to connect and chat.
     * An item clicker listener is created, that creates a connection between devices
     */
    public void initiateRecyclerView() {
        mRecyclerView = findViewById(R.id.pairedListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        getPairedDevices();
        mAdapter = new DeviceRecyclerAdapter(btArrayDevice);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Log.d(TAG3, "Item clicked");
                Intent BluetoothChatMessages = new Intent(BluetoothChat.this , BluetoothChatMessages.class);
                device = btArrayDevice.get(position);
                BluetoothChatMessages.putExtra("btdevice",device);
                startActivity(BluetoothChatMessages);
            }
        });
    }
}