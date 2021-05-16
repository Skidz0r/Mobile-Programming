package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothChatMessages extends AppCompatActivity implements BluetoothState {
    // Default android bluetooth adapter
    BluetoothAdapter btAdapter;
    // Holds a list of paired devices
    ArrayList<BluetoothDevice> btArrayDevice;

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /* MainChat Defines */
    private Context context;
    private ListView listMainChat;
    private EditText createMessage;
    private Button sendButton;
    private ArrayAdapter<String> adapterMainChat;

    /**
     * Messenger for communicating with service.
     * */
    Messenger serviceChannel = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean isBoundToService = false;
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger clientChannel = new Messenger(new BluetoothChatMessages.MessageHandler());

    /* Name of the app. Used in server thread, used to initialize a connection.*/
    private final static String NAME = "MobileChatApp";
    /* "Unique" UUID used in sever/client thread, used to initialize a connection*/
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    final String TAG = "BluetoothChat";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_chat_messages); // Using chat_messages.xml layout
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        /* Important stuff for main chat */
        context = BluetoothChatMessages.this;
        load_main_chat();
        doBindService();

        /* Start Conection to other device */
        BluetoothDevice device= getIntent().getExtras().getParcelable("btdevice"); // Gets the device from BluetoothChat.class
        String connectedDevice = device.getName(); // Get other device name so we later display on the chat
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

    void sendMessageToService(Handler handler,short flag, String message) {
        try {
            Log.i(TAG,"Handler:"+handler+"  Flag:"+flag+"  Message:"+message);
            Message msg = Message.obtain(handler, flag, message);
            msg.replyTo = clientChannel;
            serviceChannel.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void load_main_chat()
    {
        /* Main Chat */
        listMainChat = findViewById(R.id.list_conversation);
        createMessage = findViewById(R.id.Enter_Message);
        sendButton = findViewById(R.id.Send_Message);
        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = createMessage.getText().toString();
                if(!message.isEmpty())
                {
                    createMessage.setText("");
                    sendMessageToDevice(message);
                }
            }
        });
    }

    /**
     * Method to send message to the other devices throwing exceptions in case its not possible.
     */
    public void sendMessageToDevice(String message)
    {
        if(BluetoothService.sendThread.size()>0)
        {
            try {
                BluetoothService.sendThread.get(0).write(message); // More threads = get(i) (still need to work on that)
                adapterMainChat.add("Me: " + message);
            } catch(ArrayIndexOutOfBoundsException e)
            {
                Log.i("SendMessageMethod:","Devices were not connected thought a thread.");
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

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Message received: " + msg.what);

            switch(msg.what) {
                case REGISTER_CLIENT:
                    break;

                case MESSAGE_READ:
                    String message = msg.getData().getString("message");
                    String device = msg.getData().getString("device");
                    adapterMainChat.add(device+": "+message);
                    break;

                default:
                    break;
            }
        }
    }

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
            bindService(new Intent(BluetoothChatMessages.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

}