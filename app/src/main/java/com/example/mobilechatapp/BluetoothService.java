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
import android.nfc.Tag;
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
import java.util.Arrays;
import java.util.UUID;

public class BluetoothService extends Service implements BluetoothState {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;

    ArrayList<BluetoothDevice> unboundedDevices = new ArrayList<>();
    ArrayList<BluetoothDevice> knownDevices = new ArrayList<>();

    // Variable to keep track of the registered clients
    ArrayList<Messenger> clients = new ArrayList<>();

    // Tag used in Logs
    final String TAG = "Service";
    final String TAG0 = "ServiceServerThread";
    final String TAG1 = "ServiceClientThread";
    final String TAG2 = "ServiceConnectionThread";

    /**
     * Handler of incoming messages from clients
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if ( msg.replyTo == null )
                Log.i(TAG, "reply to is null");

            switch (msg.what) {
                case REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    Log.i(TAG, "New Client" + msg.toString());
                    sendSimpleMessage(msg.replyTo, REGISTER_CLIENT, null);
                    break;

                case UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    Log.i(TAG, "Remove client " + msg.toString());
                    break;

                case BT_STATUS:
                    Log.i(TAG, "Someone asked for bt status");

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
                    Log.i(TAG, "Someone asked to start discovery mode");

                    if ( btAdapter.isDiscovering() ) {
                        Log.i(TAG, "Discovery mode is already running");
                        btAdapter.cancelDiscovery();
                        Log.i(TAG, "Restarting discovery");
                        btAdapter.startDiscovery();
                    }
                        // We assume that however asked us to start discovering has
                        // user permission
                        unboundedDevices.clear();
                    Log.i("CLEAR BEFORE:",knownDevices.toString());
                        knownDevices.clear();
                        Log.i("CLEAR AFTER:",knownDevices.toString());
                        btAdapter.startDiscovery();
                    break;

                case BT_END_DISCOVERY:
                    Log.i(TAG,"Someone asked to stop discovery mode");

                    if ( btAdapter.isDiscovering() ) {
                        btAdapter.cancelDiscovery();
                    }
                    else
                        Log.i(TAG, "Discovery was not on");

                    break;

                case TEST_RECEIVE_MSG:
                    Log.i(TAG, "Service sends test message");
                    sendSimpleMessage(msg.replyTo, TEST_RECEIVE_MSG, null);
                    break;

                case BT_GET_UNBOUNDED_DEVICE:
                    Log.i(TAG, "Someone request list of unbounded devices");
                    sendSimpleMessage(msg.replyTo, BT_GET_UNBOUNDED_DEVICE, unboundedDevices);
                    break;

                case BT_CREATE_BOUND:
                    Log.i(TAG, "Someone request a bound creation");
                    BluetoothDevice device = (BluetoothDevice) msg.obj;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        device.createBond();
                    }

                    break;

                case BT_GET_DEVICES:
                    Log.i(TAG, "Someone request the known devices");
                    sendSimpleMessage(msg.replyTo, BT_GET_DEVICES, knownDevices);
                    for(BluetoothDevice deviceb : knownDevices)
                    {
                        Log.d(TAG,"THE DEVICE:"+deviceb);
                    }
                    break;

                case START_LISTENING:
                    Log.i(TAG, "Someone requested to start listening for connections");

                    if ( serverThread == null) {
                        serverThread = new ServerThread();
                        serverThread.start();
                    }

                    break;

                case CONNECT:
                    BluetoothDevice dev = (BluetoothDevice) msg.obj;

                    Log.i(TAG, "Connection request with device " + dev.getName());

                    ClientThread temp = new ClientThread(dev);
                    temp.start();

                    break;


                default:
                    Log.i(TAG, "Unprocessed flag: " + msg.what);
            }
        }
    }

    /**
     * Send message to a specific registered client
     *
     * @param destiny {@link Messenger} client to send message
     * @param response response to send
     * @param obj object to send
     */
    void sendSimpleMessage(Messenger destiny, short response, Object obj) {
        Message msg = (obj == null) ? Message.obtain(null, response) : Message.obtain(null, response, obj);
        try {
            destiny.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send message to a specific registered client
     *
     * @param destiny {@link Messenger} client to send message
     * @param response response to send (In this case MESSAGE_READ which is 19).
     * @param message The text message
     * @param deviceName What device sent us the text message {@param message}
     */
    void sendMessageToRead(Messenger destiny , short response , String message, String deviceName)
    {
        Message msg = (message == null) ? Message.obtain(null, response) : Message.obtain(null, response, message);
        Bundle bundle = new Bundle();
        bundle.putString("message",message);
        bundle.putString("device",deviceName);
        msg.setData(bundle);
        try {
            destiny.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send message to all the registered clients
     *
     * @param response {@link BluetoothState} identification
     * @param obj Response object
     */
    void sendAllSimpleMessage(short response, Object obj)
    {
        for (Messenger client : clients)
            sendSimpleMessage(client, response, obj);
    }


    /**
     * Send message with a text message associated to all registered clients
     */
    void sendMessageToReadShort(short response, String message, String deviceName)
    {
        for (Messenger client : clients)
            sendMessageToRead(client, response, message,deviceName);
    }


    /**
     * Target we publish for clients to send messages to handleMessage
     */
    final Messenger messenger = new Messenger(new MessageHandler());

    @Override
    public void onCreate() {
        Log.i(TAG, "Server creation");

        btAdapter = BluetoothAdapter.getDefaultAdapter();


        /* Crete new filter*/
        IntentFilter filter = new IntentFilter();

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
        Log.i(TAG, "Server destruction");
        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Client bind");
        return messenger.getBinder();
    }

    /**
     * BroadCast receiver definition. Used to receive notification from android system
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            // Receives a broadcast that discovery mode has stated and informs
            // all the clients of this info
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG, "Discovery mode has started");
                sendAllSimpleMessage(BT_START_DISCOVERY, null);
            }
            // Receives a broadcast that discovery mode has ended
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.i(TAG, "Discovery mode has ended");
                if(knownDevices.size()==0)Log.i(TAG,"No devices were found.");
                sendAllSimpleMessage(BT_END_DISCOVERY, null);
            }

            // Service received info that the bluetooth state has changed and informs
            // all its clients
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

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
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Device found " + device.getName());
                if(!knownDevices.contains(device))
                {

                    // Check if device is not bonded and has a name
                    if (device.getBondState() == BluetoothDevice.BOND_NONE)
                    {
                        // Add device to array
                        unboundedDevices.add(device);

                        //Notify all clients. They may want the info,
                        sendAllSimpleMessage(BT_UNBOUND_DEVICE_FOUND, device);
                    }
                    knownDevices.add(device);


                    sendAllSimpleMessage(BT_DEVICE_FOUND, device);
                }
            }

            // Bluetooth has bonded or unbounded with some device
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = device.getBondState();

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
    };

    /* Name of the app. Used in server thread, used to initialize a connection.*/
    private final static String NAME = "MobileChatApp";
    /* "Unique" UUID used in sever/client thread, used to initialize a connection*/
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    /* References to threads*/
    private ServerThread serverThread = null;
    private ClientThread clientThread = null;
    public static ArrayList<ListenThread> listenThread = new ArrayList<>();
    public static ArrayList<SendThread> sendThread = new ArrayList<>();

    /**
     * Thread will listen for a bluetooth connection request.
     * Returns, when connection either fails or succeeds.
     */
    private class ServerThread extends Thread {
        private BluetoothServerSocket myServerSocket;

        public ServerThread() {
            Log.i(TAG0, "Creating server thread");

            BluetoothServerSocket tmp = null;

            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG0, "Server Socket failed to listen", e);
            }

            myServerSocket = tmp;
        }

        public void run() {
            while(myServerSocket != null) {
                Log.i(TAG, "Listening to new connections");

                BluetoothSocket socket = null;

                try {
                    socket = myServerSocket.accept();

                    createListenSendThreads(socket, socket.getRemoteDevice());

                    //ConnectedThread temp = new ConnectedThread(socket, socket.getRemoteDevice());
                    //temp.start();
                    //connectedThread.add(temp);

                    Log.i(TAG0, "Connection success");
                }catch (IOException e) {
                    Log.e(TAG0, "Failed in accepting server socket", e);
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

            Log.i(TAG1, "Trying to connect with " + device.getName());

            try {
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG1, "Failed to create socket", e);
            }

            mySocket = temp;
        }

        public void run() {
            try {
                mySocket.connect();

                createListenSendThreads(mySocket, mySocket.getRemoteDevice());

                //ConnectedThread temp = new ConnectedThread(mySocket, mySocket.getRemoteDevice());
                //temp.start();
                //connectedThread.add(temp);

                Log.i(TAG1, "Connection success");
            } catch (IOException e) {
                Log.e(TAG1, "Failed to connect to socket", e);
                this.cancel();
            } finally {
                Log.i(TAG1, "Client thread end");
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

    private void createListenSendThreads(BluetoothSocket mySocket, BluetoothDevice device) {
        ListenThread tempL = new ListenThread(mySocket, mySocket.getRemoteDevice());
        tempL.start();

        SendThread tempS = new SendThread(mySocket, mySocket.getRemoteDevice());
        tempS.start();

        listenThread.add(tempL);
        sendThread.add(tempS);
    }

    public class ListenThread extends Thread {
        private final BluetoothSocket mySocket;
        private String deviceName;
        private final InputStream myIn;

        BluetoothDevice connectedDevice;

        private byte[] myBuffer;
        private static final int BUFFER_SIZE = 1024;

        private ListenThread(BluetoothSocket mySocket, BluetoothDevice device) {
            Log.i(TAG2, "Creating listening thread");
            this.mySocket = mySocket;
            this.connectedDevice = device;
            this.deviceName = device.getName();

            InputStream tempIn = null;

            try{
                tempIn = mySocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG2, "Failed to get input stream");
            }

            myIn = tempIn;
        }

        public void run() {
            int byteRead;
            myBuffer = new byte[BUFFER_SIZE];

            while (true) {
                try {
                    if(myBuffer!=null)
                    {
                        byteRead = myIn.read(myBuffer);
                        Log.i(TAG2, "New message: " + new String(myBuffer));
                        String messageReceived = new String(myBuffer);
                        sendMessageToReadShort(MESSAGE_READ,messageReceived,deviceName);
                        clearBytes();
                    }
                } catch(IOException e) {
                    Log.e(TAG2, "Input stream was disconnected");
                    listenThread.remove(this);
                    break;
                }
            }
        }

        private void clearBytes()
        {
            if(myBuffer!=null)
            {
                Arrays.fill(myBuffer,(byte)0);
                Log.i("ClearBytes:","Buffer was cleared.");
            }
        }


    }

    public class SendThread extends Thread {
        private final BluetoothSocket mySocket;

        private final OutputStream myOut;

        BluetoothDevice connectedDevice;

        private SendThread(BluetoothSocket mySocket, BluetoothDevice connectedDevice) {
            Log.i(TAG2, "Creating send thread");

            this.mySocket = mySocket;
            this.connectedDevice = connectedDevice;

            OutputStream tempOut = null;

            try{
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
            } catch(IOException e) {
                Log.e(TAG2, "Failed to send data");
            }
        }

    }

}

