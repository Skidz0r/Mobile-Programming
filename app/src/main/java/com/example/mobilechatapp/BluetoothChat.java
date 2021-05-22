package com.example.mobilechatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedList;

public class BluetoothChat extends AppCompatActivity implements BluetoothState {
    // Default android bluetooth adapter
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    // Holds a list of known users
    ArrayList<User> userList = new ArrayList<>();

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /**
     * Buttons
     */
    Button discovery;

    /**
     * Messenger for communicating with service.
     */
    Messenger serviceChannel = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean isBoundToService = false;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger clientChannel = new Messenger(new MessageHandler());

    /**
     * Tag used in Logs to identify class
     */
    final String TAG = "BluetoothChat";

    /**
     * Handler of incoming messages from service.
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Message received: " + msg.what);

            User user;

            switch (msg.what) {
                case REGISTER_CLIENT:
                    initialSetUp();
                    break;

                case GET_USER_LIST:
                    userList = (ArrayList<User>) msg.obj;
                    Log.i(TAG, "LISt " + userList);
                    mAdapter.setArray(userList);
                    mAdapter.notifyDataSetChanged();
                    break;

                case NEW_USER:
                    user = (User) msg.obj;
                    userList.add(user);
                    mAdapter.setArray(userList);
                    mAdapter.notifyDataSetChanged();
                    break;

                case REMOVE_USER:
                    sendMessageToService(GET_USER_LIST);
                    break;

                case GET_MESSAGE_HISTORY:
                    LinkedList<MessageInfo> list = (LinkedList<MessageInfo>) msg.obj;

                    if ( list == null ) {
                        Log.i(TAG, "No message history");
                    }

                    else {

                    }

                default:
                    break;
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceChannel = new Messenger(service);
            Log.i(TAG, "Attached to service");
            isBoundToService = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            sendMessageToService(REGISTER_CLIENT);
        }

        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceChannel = null;
            Log.i(TAG, "Disconnected from service");
        }
    };

    /**
     * Bind to service
     */
    void doBindService() {
        if (isBoundToService) {
            Log.i(TAG, "Client already bound to service");
        } else {
            // Establish a connection with the service.  We use an explicit
            // class name because there is no reason to be able to let other
            // applications replace our component.
            Log.i(TAG, "Attempting to bind to a service");
            bindService(new Intent(BluetoothChat.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbind to service
     */
    void doUnbindService() {
        if (isBoundToService) {
            Log.i(TAG, "Unbinding");

            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (serviceChannel != null) {
                try {
                    Message msg = Message.obtain(null, UNREGISTER_CLIENT);
                    msg.replyTo = clientChannel;
                    serviceChannel.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(connection);
        } else {
            Log.i(TAG, "No service to unbind. Client is not bound");
        }

        isBoundToService = false;
    }

    void sendMessageToService(short flag) {
        sendMessageToService(flag, null);
    }

    void sendMessageToService(short flag, Object obj) {
        try {
            Message msg = Message.obtain(null, flag, obj);
            msg.replyTo = clientChannel;
            serviceChannel.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat);

        discovery = findViewById(R.id.DiscoverButton);

        doBindService();
    }

    private final short REQUEST_ENABLE_DISCOVERY = 1;

    public void initialSetUp() {
        discovery.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            if (!btAdapter.isDiscovering()) {
                Intent enableBtIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_DISCOVERY);
            } else {
                sendMessageToService(BT_END_DISCOVERY);
            }
        });

        askForKnownDevices();

        initiateRecyclerView();

        sendMessageToService(START_LISTENING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_DISCOVERY) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Discovery request failed");
            } else {
                Log.i(TAG, "Discovery request accepted");
                sendMessageToService(BT_START_DISCOVERY);
            }
        }
    }


    /**
     * Get the info of the currently known devices, into an array list
     */
    public void askForKnownDevices() {
        sendMessageToService(BT_GET_DEVICES);
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

        mAdapter = new DeviceRecyclerAdapter(userList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(position -> {
            Log.i(TAG, "Item clicked");
            Intent openChat = new Intent(BluetoothChat.this, BluetoothChatMessages.class);
            openChat.putExtra("userName", userList.get(position).getId());
            startActivity(openChat);
        });
    }

    /**
     * Clears the data in the recycler view. Its used when a new discovery is enabled, since
     * old devices may have gone out of discovery.
     */
    private void resetRecyclerContent() {
        userList = new ArrayList<>();
        mAdapter.setArray(userList);
        mAdapter.notifyDataSetChanged();
    }
}