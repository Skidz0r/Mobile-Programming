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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BluetoothChat extends AppCompatActivity {
    // Default android bluetooth adapter
    BluetoothAdapter btAdapter;
    // Holds a list of paired devices
    ArrayList<BluetoothDevice> btArrayDevice;

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecycleAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /* Debug Tags,used for debugging/errors/info*/
    private final static String TAG0 = "ServerThread";
    private final static String TAG1 = "ClientThread";
    private final static String TAG2 = "ConnectedThread";
    private final static String TAG3 = "ItemClickListener";

    /* Name of the app. Used in server thread, used to initialize a connection.*/
    private final static String NAME = "MobileChatApp";
    /* "Unique" UUID used in sever/client thread, used to initialize a connection*/
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    /* References to threads*/
    private ServerThread serverThread = null;
    private ClientThread clientThread = null;
    private ConnectedThread connectedThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        initiateRecyclerView();
        newServerThread();
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
     */
    public void initiateRecyclerView() {
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

    /**
     * Thread will listen for a bluetooth connection request.
     * Returns, when connection either fails or succeeds. Only one connection per thread
     */
    private class ServerThread extends Thread {
        private final BluetoothServerSocket myServerSocket;

        public ServerThread() {
            BluetoothServerSocket tmp = null;

            Log.i(TAG0, "Listening to connections");

            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG0, "Server Socket failed to listen", e);
            }

            myServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            try {
                socket = myServerSocket.accept();
                Log.i(TAG0, "Connection success");
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();

                //basicTest();
            } catch (IOException e) {
                Log.e(TAG0, "Failed in accepting server socket", e);
            }

            this.cancel();
        }

        public void cancel() {
            try {
                myServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG0, "Failed in closing server socket", e);
            }
        }
    }

    /**
     * Thread will request a connection to a device. Returns when it fails or succeeds.
     * Only one request per thread
     */
    private class ClientThread extends Thread {
        private final BluetoothSocket mySocket;

        public ClientThread(BluetoothDevice device) {
            BluetoothSocket temp = null;

            Log.i(TAG1, "Trying to connect with " + device.getName());

            try {
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG1, "Failed to create socket", e);
            }

            mySocket = temp;
        }

        public void run() {
            btAdapter.cancelDiscovery();

            try {
                mySocket.connect();
                Log.i(TAG1, "Connection success");

                connectedThread = new ConnectedThread(mySocket);
                connectedThread.start();

                //basicTest();
            } catch (IOException e) {
                Log.e(TAG1, "Failed to connect to socket", e);
                this.cancel();
            }
        }

        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e1) {
                Log.e(TAG1, "Failed to close socket", e1);
            }
        }
    }

    /**
     * Method will create a new server thread, that will listen for upcoming connection requests
     */
    public void newServerThread() {
        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * Thread will handle receiving and sending messages.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mySocket;
        private final InputStream myIn;
        private final OutputStream myOut;

        private byte[] myBuffer;
        private static final int BUFFER_SIZE = 1024;

        public ConnectedThread(BluetoothSocket socket) {
            mySocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            Log.i(TAG2, "Create connected Thread");

            try{
                tempIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG2, "Failed to get input stream");
            }

            try{
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG2, "Failed to get output stream");
            }

            myIn = tempIn;
            myOut = tempOut;
        }

        public void run() {
            myBuffer = new byte[BUFFER_SIZE];
            int byteRead;

            while (true) {
                try {
                    byteRead = myIn.read(myBuffer);

                    Log.i(TAG2, new String(myBuffer));

                } catch(IOException e) {
                    Log.e(TAG2, "Input stream was disconnected");
                }
            }
        }

        public void write(byte[] message) {
            try {
                myOut.write(message);
                Log.i(TAG2, "Sending Message");
            } catch(IOException e) {
                Log.e(TAG2, "Failed to send data");
            }
        }

        public void cancel() {
            try {
                mySocket.close();
            } catch (IOException e) {
                Log.e(TAG2, "Failed to close socket");
            }
        }
    }

    /**
     * This method will test if the devices are correctly sending and receiving messages!
     * It should only be called, after making sure, the devices are connected.
     */
    public void basicTest() {
        final String message = btAdapter.getName() + " says hello!";

        while (true) {
            try {
                TimeUnit.SECONDS.sleep(1);
                connectedThread.write(message.getBytes());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}