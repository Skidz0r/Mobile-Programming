package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothChat extends AppCompatActivity {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;
    ArrayList<BluetoothDevice> btArrayDevice;

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecycleAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /* Debug Tags*/
    private final static String TAG0 = "ServerThread";
    private final static String TAG1 = "ClientThread";
    private final static String TAG2 = "ConnectedThread";
    private final static String TAG3 = "ItemClickListener";

    /* Used in client and server configurations.*/
    private final static String NAME = "MobileChatApp";
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    /* References to threads*/
    private ServerThread serverThread = null;
    private ClientThread clientThread = null;

    /* Message handler.Messages are done with threads. Java has a built in one*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);

        /* Create new object of BluetoothAdapter*/
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        listPairedDevices();
        testConnectionMechanics();
    }

    public void getPairedDevices() {
        Set<BluetoothDevice> temp = btAdapter.getBondedDevices();

        btArrayDevice = new ArrayList<>();
        btArrayDevice.addAll(temp);
    }

    public void listPairedDevices() {
        mRecyclerView = findViewById(R.id.pairedListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        getPairedDevices();
        mAdapter = new DeviceRecycleAdapter(btArrayDevice);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecycleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Log.d(TAG3, "Item clicked");

                clientThread = new ClientThread(btArrayDevice.get(position));
                clientThread.start();
            }
        });
    }

    /* Server thread. Accepts user connection. Passive agent*/
    private class ServerThread extends Thread {
        private final BluetoothServerSocket myServerSocket;

        public ServerThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e) {
                Log.e(TAG0, "Server Socket listen failed", e);
            }

            myServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            Log.i(TAG0, "Listening to connections");

            try {
                socket = myServerSocket.accept();
                Log.i(TAG0, "Connection success");
            } catch (IOException e) {
                Log.e(TAG0, "Server Socket accept failed", e);
            }

            this.cancel();
        }

        public void cancel() {
            try {
                myServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG0, "Server socket close failed", e);
            }
        }
    }

    /* Initiates device connection. Active agent*/
    private class ClientThread extends Thread {
        private final BluetoothSocket mySocket;

        public ClientThread(BluetoothDevice device) {
            BluetoothSocket temp = null;

            try {
                Log.d(TAG1, "Device Name: " + device.getName());
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG1, "Socket create failed", e);
            }

            mySocket = temp;
        }

        public void run() {
            btAdapter.cancelDiscovery();

            Log.i(TAG1, "Initiating connection");

            try {
                mySocket.connect();
                Log.i(TAG1, "Connection success");
            } catch (IOException e) {
                Log.e(TAG1, "Socket connection failed", e);
                this.cancel();
            }
        }

        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e1) {
                Log.e(TAG1, "Socket close failed", e1);
            }
        }
    }

    public void testConnectionMechanics() {
        serverThread = new ServerThread();
        serverThread.start();
    }
}