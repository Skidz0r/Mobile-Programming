package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.Adapter.MessageListAdapter;
import com.example.mobilechatapp.Information.BluetoothState;
import com.example.mobilechatapp.Information.MessageInfo;
import com.example.mobilechatapp.Information.UserChat;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity implements BluetoothState {
    // Default android bluetooth adapter
    BluetoothAdapter btAdapter;
    // Connected device
    private BluetoothDevice device;

    /* MainChat Defines */
    private Context context;
    private EditText createMessage;
    private Button sendButton;
    private TextView username;

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
     * Name of the app. Used in server thread, used to initialize a connection.
     */
    private final static String NAME = "MobileChatApp";

    /**
     * "Unique" UUID used in sever/client thread, used to initialize a connection
     */
    private final static UUID MY_UUID = UUID.fromString("b885d9a0-b9a7-4a2a-b05d-b3aae45c9192");

    String userName;
    UserChat userChat;

    final String TAG = "BluetoothChat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        /* Important stuff for main chat */
        context = ChatActivity.this;
        loadMainChat();
        doBindService();

        /* Start Connection to other device */
        userName = getIntent().getStringExtra("userChatId");
    }

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<MessageInfo> messageList = new LinkedList<>();

    public void loadMainChat() {
        mMessageRecycler = (RecyclerView) findViewById(R.id.recycler_list_chat);
        mMessageAdapter = new MessageListAdapter(this, messageList);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        sendButton = findViewById(R.id.button_send);

        createMessage = findViewById(R.id.edit_message);

        username = findViewById(R.id.username);

        sendButton.setOnClickListener(v -> {
            String message = createMessage.getText().toString();

            if (!message.isEmpty()) {
                createMessage.setText(null);
                Log.i(TAG, "TRY -> " + message);
                sendMessageToDevice(message);
            }
        });
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
            Log.i(TAG, "Attempting to bind to a service");
            bindService(new Intent(ChatActivity.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
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

    /**
     * Method to send message to the other devices
     */
    public void sendMessageToDevice(String message) {
        if (userChat == null) {
            Log.i(TAG, "User is null. Message ignored");
            return;
        }

        MessageInfo messageInfo = new MessageInfo(null, userChat, message);

        sendMessageToService(MESSAGE_WRITE, messageInfo);
        messageList.add(messageInfo);
        mMessageAdapter.notifyDataSetChanged();
    }

    /**
     * Send simple message to service
     *
     * @param flag {@link BluetoothState} flag
     */
    void sendMessageToService(short flag) {
        sendMessageToService(flag, null);
    }

    /**
     * Send simple message to server with an object attached
     *
     * @param flag {@link BluetoothState} flag
     * @param obj  Object to send
     */
    void sendMessageToService(short flag, Object obj) {
        try {
            Message msg = Message.obtain(null, flag, obj);
            msg.replyTo = clientChannel;
            serviceChannel.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler of incoming messages from service.
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Message received: " + msg.what);

            MessageInfo messageInfo;

            switch (msg.what) {
                case MESSAGE_READ:
                    messageInfo = (MessageInfo) msg.obj;

                    if (messageInfo.getFromUser().equals(userChat)) {

                        messageList.add(messageInfo);
                        mMessageAdapter.notifyDataSetChanged();
                    }
                    break;

                case GET_MESSAGE_HISTORY:
                    resetRecyclerContent((LinkedList<MessageInfo>) msg.obj);
                    break;

                case REGISTER_CLIENT:
                    sendMessageToService(GET_USER, userName);
                    break;

                case GET_USER:
                    userChat = (UserChat) msg.obj;
                    Log.i(TAG, "User is " + userChat);
                    username.setText(userChat.getId());
                    sendMessageToService(GET_MESSAGE_HISTORY, userChat);
                    break;

                /**
                 * Service informed us that a user was removed, i.e we lost connection to one
                 * previously known user. This user might be the one we are currently talking, if
                 * so we need to close activity. If time is available we could try to save the
                 * message until a connection is reestablished
                 */
                case REMOVE_USER:
                    UserChat user = (UserChat) msg.obj;

                    if (user.equals(userChat)) {
                        Log.i(TAG, "User disconnected");
                        finish();
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Reset the content os the recycler view
     * @param list List of exchanged messages
     */
    private void resetRecyclerContent(LinkedList<MessageInfo> list) {
        if (list == null)
            return;

        mMessageAdapter.setList(list);
        mMessageAdapter.notifyDataSetChanged();
    }
}