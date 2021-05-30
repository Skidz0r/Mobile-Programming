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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.mobilechatapp.Model.UserChat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class BluetoothService extends Service implements BluetoothState {
    /**
     * Bluetooth adapter
     */
    BluetoothAdapter btAdapter;

    /**
     * List of known devices.Unbounded and bounded
     */
    ArrayList<BluetoothDevice> knownDevices = new ArrayList<>();

    /**
     * List of devices that we are connected to
     */
    ArrayList<UserChat> userChatList = new ArrayList<>();

    /**
     * Small local memory
     */
    Map<UserChat, LinkedList<MessageInfo>> mem = new HashMap<>();

    /**
     * Maximum size of out local memory
     */
    int MAX_LOCAL_MEM = 10;

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

            MessageInfo messageInfo;
            UserChat userChat;
            BluetoothDevice device;

            switch (msg.what) {
                case TEST_RECEIVE_MSG:
                    Log.i(TAG_ACTION, "Test message");
                    sendSimpleMessage(msg.replyTo, TEST_RECEIVE_MSG, null);
                    break;

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

                // We assume that however made this request, has asked for user permission
                case BT_START_DISCOVERY:
                    Log.i(TAG_REQUEST, "Start discovery mode");

                    if (btAdapter.isDiscovering()) {
                        Log.i(TAG, "Discovery mode is already running");
                    } else {
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

                case BT_GET_DEVICES:
                    Log.i(TAG_REQUEST, "List of known devices");
                    sendSimpleMessage(msg.replyTo, BT_GET_DEVICES, knownDevices);
                    break;

                case GET_USER_LIST:
                    Log.i(TAG_REQUEST, "List of connected users");
                    sendSimpleMessage(msg.replyTo, GET_USER_LIST, userChatList);
                    break;

                case START_LISTENING:
                    Log.i(TAG_REQUEST, "Accept connections");

                    if (serverThread == null) {
                        serverThread = new ServerThread();
                        serverThread.start();
                    }
                    break;

                case CONNECT:
                    device = (BluetoothDevice) msg.obj;
                    Log.i(TAG_REQUEST, "Connect with " + device.getName());

                    tryToConnect(device);
                    break;

                case MESSAGE_WRITE:
                    messageInfo = (MessageInfo) msg.obj;
                    userChat = messageInfo.getToUser();

                    if (!checkIfConnected(userChat)) {
                        Log.i(TAG, "Connection not found with " + userChat.toString());
                    } else {
                        // Find the connected thread with that particular user and try
                        // to send a message
                        findConnectionThread(userChat).write(messageInfo);
                    }
                    break;

                /**
                 * Someone asked for a the history of messages between us and another user.
                 * We did not implement security features here. We assume that however sends
                 * the service request has the proper permission to see everything.
                 * This is dangerous and should be fixed if we have time to spare
                 */
                case GET_MESSAGE_HISTORY:
                    Log.i(TAG_REQUEST, "Message history");
                    userChat = (UserChat) msg.obj;

                    sendSimpleMessage(msg.replyTo, GET_MESSAGE_HISTORY, mem.get(userChat));
                    break;

                /**
                 * Get the object UserChat that contains the user name. This will be used
                 * by activities that deal directly with message exchange visualization.
                 */
                case GET_USER:
                    String name = (String) msg.obj;

                    Log.i(TAG_REQUEST, "Get user with id: " + name);

                    for (UserChat u : userChatList)
                        if (u.getId().equals(name))
                            sendSimpleMessage(msg.replyTo, GET_USER, u);

                    // add the case where we can't find the user.

                    break;

                default:
                    Log.i(TAG, "Unprocessed flag: " + msg.what);
            }
        }
    }

    /**
     * Go though our list of known bluetooth devices that we found and try to form a conection
     * with each of them.
     */
    void autoConnect() {
        for (BluetoothDevice device : knownDevices) {
            tryToConnect(device);
        }
    }

    /**
     * Try to form a connection with a bluetooth device. This method will call a new thread to
     * try to form the connection. If successfull thhe new connection will be added to our
     * local databa
     *
     * @param device {@link BluetoothDevice} to connect
     */
    void tryToConnect(BluetoothDevice device) {
        // If we don't have a confection with that particular device, then we will try to form one
        if (!checkIfConnected(device)) {
            ClientThread temp = new ClientThread(device);
            temp.start();
        } else {
            Log.i(TAG, "Connection with " + device.getName() + " already exists");
        }

    }

    /**
     * Checks if we are connected with a particular device, identified by {@link UserChat} object
     *
     * @param userChat {@link UserChat} identifier
     * @return true if a connection exists otherwise false
     */
    boolean checkIfConnected(UserChat userChat) {
        ConnectionThread connectionThread = findConnectionThread(userChat);

        return connectionThread != null;
    }

    /**
     * Checks if we are connected with a particular device, identified by {@link BluetoothDevice}
     *
     * @param device {@link BluetoothDevice} identifier
     * @return true if connection with device exists, otherwise false
     */
    boolean checkIfConnected(BluetoothDevice device) {
        ConnectionThread connectionThread = findConnectionThread(device);

        return connectionThread != null;
    }

    /**
     * Send identifier flag to a specific registered client
     *
     * @param destiny  {@link Messenger} client to send message
     * @param response {@link BluetoothState} flag to send
     */
    void sendSimpleMessage(Messenger destiny, short response) {
        sendSimpleMessage(destiny, response, null);
    }

    /**
     * Send identifier flag and an object to a specific registered client
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
     * Send a identifier flag to o all the registered clients
     *
     * @param response {@link BluetoothState} Identifier flag
     */
    void sendAllSimpleMessage(short response) {
        sendAllSimpleMessage(response, null);
    }

    /**
     * Send a identifier flag and an object to o all the registered clients
     *
     * @param response {@link BluetoothState} Identifier flag
     * @param obj      Response object
     */
    void sendAllSimpleMessage(short response, Object obj) {
        for (Messenger client : clients)
            sendSimpleMessage(client, response, obj);
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

    /**
     * Professor says that onDestroy method is not always called when the service is closed.
     * This a problem. We should do the contents of onDestroy in onStop. The catch is that
     * we onStop method does not mean the service is destroyed, so we the service was resumed we
     * needed to register the broadcast again and form the connection again.
     * For now we will have to use onDestroy, but if time is available we should change this
     */
    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destruction");

        // If service is destroyed then we close all connection thread
        for (ConnectionThread thread : connectionThreads)
            thread.terminateConnection();

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
        if (checkIfConnected(mySocket.getRemoteDevice())) {
            return;
        }

        BluetoothDevice device = mySocket.getRemoteDevice();
        UserChat userChat = new UserChat(device.getName(), device);

        ConnectionThread thread = new ConnectionThread(mySocket, userChat);
        connectionThreads.add(thread);

        thread.start();
    }

    /**
     * Given {@link UserChat} identifier this method will return the associated ConnectionThread
     *
     * @param user {@link UserChat} identifier
     * @return null if connection thread is not found, otherwise the {@link ConnectionThread}
     */
    private ConnectionThread findConnectionThread(UserChat user) {
        for (ConnectionThread thread : connectionThreads) {
            if (thread.getUser().equals(user))
                return thread;
        }

        return null;
    }

    /**
     * Given a {@link BluetoothDevice} identifier this method will return the associated
     * ConnectionThread
     *
     * @param device {@link BluetoothDevice} identifier
     * @return null if connection thread is not found, otherwise the {@link ConnectionThread}
     */
    private ConnectionThread findConnectionThread(BluetoothDevice device) {
        for (ConnectionThread thread : connectionThreads) {
            if (thread.getUser().getDevice().equals(device))
                return thread;
        }

        return null;
    }

    /**
     * Method will add A {@link MessageInfo} to our local memory.
     * We are suposed to use a proper database, but given the time restrictions we decided to
     * use a locak HashMap. In the future this need to change
     *
     * @param messageInfo {@link MessageInfo} to save
     */
    private void addToMemory(MessageInfo messageInfo) {
        UserChat userChat;

        if (messageInfo.getFromUser() == null) {
            userChat = messageInfo.getToUser();
        } else {
            userChat = messageInfo.getFromUser();
        }

        Log.i(TAG, "Memory: " + mem.toString());

        LinkedList<MessageInfo> list = mem.get(userChat);

        if (list == null) {
            Log.i(TAG, "List is null");

            list = new LinkedList<>();
            mem.put(userChat, list);
        } else {
            Log.i(TAG, "List is not null " + list);
        }

        while (list.size() >= MAX_LOCAL_MEM)
            list.removeFirst();

        list.addLast(messageInfo);

        Log.i(TAG, "list2 :" + list == null ? "Empty" : list.toString());
    }


    /**
     * Thread will initiate input and output channels between the two devices in two
     * different thread, one for listening and the other for writing.
     * Warning in the current implementation, device is used as unique identifier between different
     * connection threads
     */
    public class ConnectionThread extends Thread {
        private final BluetoothSocket socket;

        private final UserChat user;

        private final Listen listen;
        private final Send send;

        ConnectionThread(BluetoothSocket socket, UserChat user) {
            Log.i(TAG2, "Creating connection thread");

            this.socket = socket;
            this.user = user;

            listen = new Listen(socket);
            listen.start();

            send = new Send(socket);
            send.start();

            if (!userChatList.contains(user)) {
                userChatList.add(user);
                sendAllSimpleMessage(NEW_USER, user);
                mem.put(user, new LinkedList<>());
            }
        }

        void write(MessageInfo messageInfo) {
            if (send == null) {
                Log.i(TAG2, "Send thread is dead");
            } else if (messageInfo == null) {
                Log.i(TAG2, "messageInfo is null");
            } else {
                addToMemory(messageInfo);
                send.write(messageInfo.getContent());
            }
        }

        void terminateConnection() {
            Log.i(TAG2, "Terminating connection channel");

            if (listen != null)
                listen.interrupt();

            if (send != null)
                send.interrupt();

            if (connectionThreads != null)
                connectionThreads.remove(this);

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            userChatList.remove(user);
            sendAllSimpleMessage(REMOVE_USER, null);

            this.interrupt();
        }

        public UserChat getUser() {
            return user;
        }

        public class Listen extends Thread {
            private final InputStream myIn;
            private byte[] myBuffer;
            private static final int BUFFER_SIZE = 1024;

            private Listen(BluetoothSocket socket) {
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

                        Log.i(TAG, "New message from " + user);

                        MessageInfo messageInfo = new MessageInfo(user, null, str);

                        addToMemory(messageInfo);

                        // Device in Connection Thread
                        sendAllSimpleMessage(MESSAGE_READ, new MessageInfo(user, null, str));
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

            private Send(BluetoothSocket mySocket) {
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

