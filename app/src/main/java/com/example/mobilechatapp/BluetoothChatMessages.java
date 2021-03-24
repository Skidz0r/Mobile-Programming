package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class BluetoothChatMessages extends AppCompatActivity {
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
    private int state;
    private String connectedDevice;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";


    /* Debug Tags,used for debugging/errors/info*/
    private final static String TAG0 = "ServerThread";
    private final static String TAG1 = "ClientThread";
    private final static String TAG2 = "ConnectedThread";

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
        setContentView(R.layout.activity_bluetooth_chat_messages); // Using chat_messages.xml layout
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        /* Important stuff for main chat */
        context = this;
        load_main_chat();

        /* Start Conection to other device */
        newServerThread();
        BluetoothDevice device= getIntent().getExtras().getParcelable("btdevice"); // Gets the device from BluetoothChat.class
        connectedDevice = device.getName(); // Get other device name so we later display on the chat
        clientThread = new ClientThread(device);
        clientThread.start();
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
                    connectedThread.write(message.getBytes());
                    // Will send the message to connectThread who will send it to the message handler.
                }
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
                    /* Receive input and pass it to handler */
                    byteRead = myIn.read(myBuffer);
                    handler.obtainMessage(MESSAGE_READ, byteRead, -1, myBuffer).sendToTarget();
                    Log.i(TAG2, new String(myBuffer));
                } catch(IOException e) {
                    Log.e(TAG2, "Input stream was disconnected");
                }
            }
        }

        public void write(byte[] message) {
            try {
                /* Read input and send message to handler */
                myOut.write(message);
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, message).sendToTarget();
                Log.i(TAG2, "Sending Message");
            } catch(IOException e) {
                Log.e(TAG2, "Failed to send data");
            }
        }

    }

    /**
     * This handler will deal will take care of the messages we receive/send.
     * Will also take care of the UI so we can see the messages we receive/send.
     */

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_WRITE:
                    /*Read our input and put in display(UI) as "Me: %message%" */
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    /*Receive input and put in display(UI) as "%user%: %message%" */
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    adapterMainChat.add(connectedDevice + ": " + inputBuffer);
                    break;
                case MESSAGE_DEVICE_NAME:
                    /*Show the connected device message*/
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    /* Show our message */
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    /**
     * Method will create a new server thread, that will listen for upcoming connection requests.
     */
    public void newServerThread() {
        serverThread = new ServerThread();
        serverThread.start();
    }

}