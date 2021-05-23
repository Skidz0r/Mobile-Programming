package com.example.mobilechatapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothService extends Service implements BluetoothState {
    /**
     * Bluetooth adapter
     */
    BluetoothAdapter btAdapter;

    /**
     * List of unbounded devices
     */
    ArrayList<BluetoothDevice> unboundedDevices = new ArrayList<>();
    /**
     * List of known devices.Unbounded and bounded
     */
    ArrayList<BluetoothDevice> knownDevices = new ArrayList<>();

    /**
     * Variable to keep track of the registered clients
     */
    ArrayList<Messenger> clients = new ArrayList<>();

    // Tag used in Logs
    final String TAG = "Service";
    final String TAG_REQUEST = "ServiceRequest";
    final String TAG_ACTION = "ServiceAction";
    final String TAG0 = "ServiceServerThread";
    final String TAG1 = "ServiceClientThread";
    final String TAG2 = "ServiceConnectionThread";

    /**
     * Handler of incoming messages from clients
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.replyTo == null) {
                Log.i(TAG, "reply to is null");
                return;
            }

            if (!btAdapter.isEnabled()) {
                Log.i(TAG, "BT is off. Turning it on. Request is ignored");
                btAdapter.enable();
                return;
            }

            switch (msg.what) {
                case REGISTER_CLIENT:
                    Log.i(TAG_REQUEST, "New client" + msg.toString());

                    clients.add(msg.replyTo);
                    Log.i(TAG_ACTION, "New client registered" + msg.toString());

                    sendSimpleMessage(msg.replyTo, REGISTER_CLIENT, null);
                    break;

                case UNREGISTER_CLIENT:
                    Log.i(TAG_REQUEST, "Remove client" + msg.toString());

                    clients.remove(msg.replyTo);
                    Log.i(TAG_ACTION, "Remove client " + msg.toString());

                    break;

                case BT_STATUS:
                    Log.i(TAG_ACTION, "Bluetooth status");

                    short response;

                    if (btAdapter == null)
                        response = BT_ERROR;

                    else if (btAdapter.isDiscovering())
                        response = BT_DISCOVER_ON;

                    else if (btAdapter.isEnabled())
                        response = BT_ON;

                    else
                        response = BT_OFF;

                    sendSimpleMessage(msg.replyTo, response, null);
                    break;

                case BT_START_DISCOVERY:
                    Log.i(TAG_REQUEST, "Start discovery mode");

                    if (btAdapter.isDiscovering()) {
                        Log.i(TAG, "Discovery mode is already running");
                    } else {
                        unboundedDevices.clear();
                        knownDevices.clear();
                        btAdapter.startDiscovery();
                    }
                    break;

                case BT_END_DISCOVERY:
                    Log.i(TAG_REQUEST, "Stop discovery mode");

                    if (btAdapter.isDiscovering()) {
                        btAdapter.cancelDiscovery();
                    }

                    break;

                case TEST_RECEIVE_MSG:
                    Log.i(TAG_ACTION, "Test message");
                    sendSimpleMessage(msg.replyTo, TEST_RECEIVE_MSG, null);
                    break;

                case BT_CREATE_BOUND:
                    Log.i(TAG_REQUEST, "Bound creation");
                    BluetoothDevice device = (BluetoothDevice) msg.obj;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        device.createBond();
                    }

                    break;

                case BT_GET_DEVICES:
                    Log.i(TAG_REQUEST, "List of known devices");
                    sendSimpleMessage(msg.replyTo, BT_GET_DEVICES, knownDevices);
                    break;

                case START_LISTENING:
                    Log.i(TAG_REQUEST, "Accept connections");

                    if (serverThread == null) {
                        serverThread = new ServerThread();
                        serverThread.start();
                    }

                    break;

                case CONNECT:
                    BluetoothDevice dev = (BluetoothDevice) msg.obj;
                    Log.i(TAG_REQUEST, "Connect with " + dev.getName());

                    if (!checkIfConnected(dev)) {
                        ClientThread temp = new ClientThread(dev);
                        temp.start();
                    } else {
                        Log.i(TAG, "Connection already exists");
                    }
                    break;

                case MESSAGE_WRITE:
                    Bundle bundle = msg.getData();
                    String str = bundle.getString("message");
                    String address = bundle.getString("deviceMac");

                    Log.i(TAG_REQUEST, "Send message to " + address);

                    ConnectionThread connectionThread = findConnectionThreadByMac(address);

                    if (connectionThread == null)
                        Log.i(TAG, "Connection thread not found");

                    else
                        connectionThread.write(str);

                    break;

                default:
                    Log.i(TAG, "Unprocessed flag: " + msg.what);
            }
        }
    }

    /**
     * Checks if a connection with a devices is formed
     *
     * @param device {@link BluetoothDevice} device to check
     * @return true if connection with device exists, otherwise false
     */
    boolean checkIfConnected(BluetoothDevice device) {
        ConnectionThread connectionThread = findConnectionThread(device);

        return connectionThread != null;
    }

    /**
     * Send message with an object, to a specific registered client
     *
     * @param destiny  {@link Messenger} client to send message
     * @param response {@link BluetoothState} flag to send
     * @param obj      object to send
     */
    void sendSimpleMessage(Messenger destiny, short response, Object obj) {
        Message msg = (obj == null) ? Message.obtain(null, response) :
                Message.obtain(null, response, obj);

        try {
            destiny.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send simple message to all the registered clients
     *
     * @param response {@link BluetoothState} flag
     * @param obj      Response object
     */
    void sendAllSimpleMessage(short response, Object obj) {
        for (Messenger client : clients)
            sendSimpleMessage(client, response, obj);
    }

    /**
     * Inform of a new message received.
     *
     * @param destiny Client to inform
     * @param str     Contents of the message
     * @param device  {@link BluetoothDevice} Source of the message
     */
    void informOfNewMessage(Messenger destiny, String str, BluetoothDevice device) {
        Bundle bundle = new Bundle();
        bundle.putString("message", str);
        bundle.putString("device", device.getName());

        Message msg = Message.obtain(null, MESSAGE_READ);
        msg.setData(bundle);

        try {
            destiny.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inform all the clients of a received message
     *
     * @param str    contents of the message
     * @param device {@link BluetoothDevice} source of the message
     */
    void informAllOfNewMessage(String str, BluetoothDevice device) {
        for (Messenger client : clients)
            informOfNewMessage(client, str, device);
    }

    /**
     * Send message to a specific registered client
     *
     * @param destiny    {@link Messenger} client to send message
     * @param response   response to send (In this case MESSAGE_READ which is 19).
     * @param message    The text message
     * @param deviceName What device sent us the text message {@param message}
     */
    void sendMessageToRead(Messenger destiny, short response, String message, String deviceName) {
        Message msg = (message == null) ? Message.obtain(null, response) : Message.obtain(null, response, message);
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        bundle.putString("device", deviceName);
        msg.setData(bundle);
        try {
            destiny.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send message with a text message associated to all registered clients
     */
    void sendMessageToReadShort(short response, String message, String deviceName) {
        for (Messenger client : clients)
            sendMessageToRead(client, response, message, deviceName);
    }


    /**
     * Target we publish for clients to send messages to handleMessage
     */
    final Messenger messenger = new Messenger(new MessageHandler());

    @Override
    public void onCreate() {
        Log.i(TAG, "Service creation");

        // Get default bt adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // Crete new filter
        IntentFilter filter = new IntentFilter();

        // Filters to add to our broadcast receiver
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        /* Register the receiver with the android system.*/
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destruction");

        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "New client bind");
        return messenger.getBinder();
    }

    /**
     * BroadCast receiver definition. Used to receive and handle notification for
     * android system
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Receives a broadcast that discovery mode has stated and informs
            // all the clients of this info
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG, "Discovery mode has started");

                sendAllSimpleMessage(BT_START_DISCOVERY, null);
            }
            // Receives a broadcast that discovery mode has ended
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Discovery mode has ended");

                if (knownDevices.size() == 0)
                    Log.i(TAG, "No devices were found.");

                sendAllSimpleMessage(BT_END_DISCOVERY, null);
            }

            // Service received info that the bluetooth state has changed and informs
            // all its clients
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                Log.i(TAG, "Bluetooth state changed");

                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        sendAllSimpleMessage(BT_ON, null);
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        sendAllSimpleMessage(BT_OFF, null);
                        break;

                    default:
                        break;
                }
            }

            // Discovery mode has found a device
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                Log.i(TAG, "Device found " + device.getName());

                if (!knownDevices.contains(device)) {
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        unboundedDevices.add(device);

                        sendAllSimpleMessage(BT_UNBOUND_DEVICE_FOUND, device);
                    }

                    knownDevices.add(device);

                    sendAllSimpleMessage(BT_DEVICE_FOUND, device);
                }
            }

            // Bluetooth has bonded or unbounded with some device
            else {
                assert action != null;
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    final int state = device == null ? -1 : device.getBondState();

                    switch (state) {
                        case BluetoothDevice.BOND_BONDED:
                            Log.i(TAG, "Device bonded");
                            unboundedDevices.remove(device);
                            sendAllSimpleMessage(BT_DEVICE_BOUND, device);
                            break;

                        case BluetoothDevice.BOND_NONE:
                            Log.i(TAG, "Device not bounded");
                            sendAllSimpleMessage(BT_DEVICE_UNBOUND, device);
                            break;

                        default:
                            break;
                    }
                }
            }
        }
    };

    /* Name of the app. Used in server thread, used to initialize a connection.*/
    private final static String NAME = "MobileChatApp";
    /* "Unique" UUID used in sever/client thread, used to initialize a connection*/
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    /**
     * Server thread used to listen to incoming connection request
     */
    private ServerThread serverThread = null;
    /**
     * Client thread used to connect to other devices
     */
    private ClientThread clientThread = null;

    /**
     * List of connections
     */
    public ArrayList<ConnectionThread> connectionThreads = new ArrayList<>();

    /**
     * Thread will listen for a bluetooth connection request.
     */
    private class ServerThread extends Thread {
        private BluetoothServerSocket myServerSocket;

        public ServerThread() {
            Log.i(TAG0, "Creating server thread");

            BluetoothServerSocket tmp = null;

            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG0, "BluetoothServerSocket failed", e);
            }

            myServerSocket = tmp;
        }

        public void run() {
            while (myServerSocket != null) {
                Log.i(TAG, "Waiting for a new connection request");

                BluetoothSocket socket;

                try {
                    socket = myServerSocket.accept();

                    createConnectionThread(socket);

                    Log.i(TAG0, "Connection attempt succeeded with " + socket.getRemoteDevice().getName());
                } catch (IOException e) {
                    Log.e(TAG0, "Connection attempt failed", e);
                }

            }

            Log.i(TAG0, "Server thread end");
        }

        public void cancel() {
            try {
                myServerSocket.close();
                myServerSocket = null;
            } catch (IOException e) {
                Log.e(TAG0, "Failed in closing server socket", e);
            }

            Log.i(TAG0, "Closing server thread");
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

            Log.i(TAG1, "Creating client thread");

            try {
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG1, "Failed to create socket", e);
            }

            mySocket = temp;
        }

        public void run() {
            try {
                Log.i(TAG1, "Connection attempt with " + mySocket.getRemoteDevice().getName());
                mySocket.connect();

                createConnectionThread(mySocket);

                Log.i(TAG1, "Connection attempt succeeded");
            } catch (IOException e) {
                Log.e(TAG1, "Connection attempt failed", e);
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
     * Initiates and saves connection channel between two devices
     *
     * @param mySocket {@link BluetoothSocket} connection socket
     */
    private void createConnectionThread(BluetoothSocket mySocket) {
        ConnectionThread connectionThread = new ConnectionThread(mySocket, mySocket.getRemoteDevice());

        connectionThreads.add(connectionThread);

        connectionThread.start();
    }

    /**
     * Given a {@link BluetoothDevice}, this method will return the corresponding connection thread
     *
     * @param device {@link BluetoothDevice} identifier
     * @return null if connection thread is not found, otherwise the {@link ConnectionThread}
     */
    private ConnectionThread findConnectionThread(BluetoothDevice device) {
        return findConnectionThreadByMac(device.getAddress());
    }

    /**
     * Find connectionThread by device mac address
     *
     * @param macAddress address of {@link BluetoothDevice}
     * @return Connection Thread that has the corresponding mac address, otherwise null
     */
    private ConnectionThread findConnectionThreadByMac(String macAddress) {
        for (ConnectionThread connectionThread : connectionThreads) {
            String otherMacAddress = connectionThread.getDevice().getAddress();

            if (connectionThread.getDevice().getAddress().equals(macAddress))
                return connectionThread;
        }

        return null;
    }

    /**
     * Thread will initiate input and output channels between the two devices in two
     * different thread, one for listening and the other for writing.
     * Warning in the current implementation, device is used as unique identifier between different
     * connection threads
     */
    private class ConnectionThread extends Thread {
        private final BluetoothSocket socket;

        private final BluetoothDevice device;

        private final Listen listen;
        private final Send send;

        ConnectionThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.i(TAG2, "Creating connection thread");

            this.socket = socket;
            this.device = device;

            listen = new Listen(socket, device);
            listen.start();

            send = new Send(socket, device);
            send.start();
        }

        void write(String message) {
            if (send != null) {
                send.write(message);
            } else
                terminateConnection();
        }

        void terminateConnection() {
            Log.i(TAG2, "Terminating connection channel");

            if (listen != null)
                listen.interrupt();

            if (send != null)
                send.interrupt();

            if (connectionThreads != null)
                connectionThreads.remove(this);

            this.interrupt();
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public class Listen extends Thread {
            private final InputStream myIn;
            private byte[] myBuffer;
            private static final int BUFFER_SIZE = 1024;

            private Listen(BluetoothSocket socket, BluetoothDevice device) {
                Log.i(TAG2, "Creating listening thread");

                InputStream tempIn = null;

                try {
                    tempIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG2, "Failed to get input stream");
                }

                myIn = tempIn;
            }

            public void run() {
                myBuffer = new byte[BUFFER_SIZE];

                while (true) {
                    try {
                        myIn.read(myBuffer);
                        String str = new String(myBuffer);

                        Log.i(TAG, "New message from " + device.getAddress());

                        // Device in Connection Thread
                        informAllOfNewMessage(str, device);
                    } catch (IOException e) {
                        Log.e(TAG2, "Input stream was disconnected");
                        terminateConnection();
                        break;
                    }
                }
            }
        }

        private class Send extends Thread {
            private final OutputStream myOut;

            private Send(BluetoothSocket mySocket, BluetoothDevice connectedDevice) {
                Log.i(TAG2, "Creating writing thread");

                OutputStream tempOut = null;

                try {
                    tempOut = mySocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG2, "Failed to get Output stream");
                }

                myOut = tempOut;
            }

            public void write(String message) {
                try {
                    Log.i(TAG2, "Trying to send message");
                    myOut.write(message.getBytes());
                    Log.i(TAG2, "Message send");
                } catch (IOException e) {
                    Log.e(TAG2, "Failed to send data");
                    terminateConnection();
                }
            }
        }
    }

}

