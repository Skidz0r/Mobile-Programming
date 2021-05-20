package com.example.mobilechatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothSetting extends AppCompatActivity implements BluetoothState{
    /** Bluetooth stuff*/
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<BluetoothDevice> btArrayDevice = new ArrayList<>();

    /** Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /** Image icon reference for bluetooth state*/
    ImageView btIcon;

    /** Buttons references*/
    Button btOnOff;
    Button btDiscovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);

        /* Get Button reference */
        btOnOff = (Button) findViewById(R.id.btOnOffButton);
        btDiscovery = (Button) findViewById(R.id.discoveryOnOff);

        /* Get image reference*/
        btIcon = (ImageView) findViewById(R.id.on_off_btIcon);

        doBindService();
    }

    /**
     * Messenger for communicating with service.
     * */
    Messenger serviceChannel = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean isBoundToService = false;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger clientChannel = new Messenger(new MessageHandler());

    /** Tag used in Logs to identify class*/
    final String TAG = "BluetoothSetting";

    /**
     * Handler of incoming messages from service.
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG,"Message received: " + msg.what);

            switch(msg.what) {
                case TEST_RECEIVE_MSG:
                    Log.i(TAG, "Client received test msg");
                    break;

                case BT_DISCOVER_ON:
                    discoveryOn();
                    break;

                case BT_DISCOVER_OFF:
                    discoveryOff();
                    break;

                case BT_ON:
                    btIsOn();
                    break;

                case BT_OFF:
                    btIsOff();
                    break;

                case BT_ERROR:
                    btIsNotFound();
                    break;

                case BT_START_DISCOVERY:
                    resetRecyclerContent();
                    discoveryOn();
                    break;

                case BT_END_DISCOVERY:
                    sendMessageToService(BT_GET_UNBOUNDED_DEVICE);
                    discoveryOff();
                    break;

                case BT_UNBOUND_DEVICE_FOUND:
                    // Add device to array
                    btArrayDevice.add((BluetoothDevice) msg.obj);
                    // Notify change
                    mAdapter.notifyDataSetChanged();
                    break;

                case BT_DEVICE_BOUND:
                    sendMessageToService(BT_GET_UNBOUNDED_DEVICE);
                    break;

                case BT_GET_UNBOUNDED_DEVICE:
                    btArrayDevice = (ArrayList<BluetoothDevice>) msg.obj;
                    mAdapter.setArray(btArrayDevice);
                    mAdapter.notifyDataSetChanged();
                    break;

                case REGISTER_CLIENT:
                    startInitialSettings();
                    break;

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
            bindService(new Intent(BluetoothSetting.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbind to service
     */
    void doUnbindService() {
        if ( isBoundToService ) {
            Log.i(TAG, "Unbinding");

            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if ( serviceChannel != null ) {
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
        }
        else {
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

    void startInitialSettings() {
        deviceListMechanics();
        btInitialDisplayMechanics();
        btTurnOnOffMechanics();
        btDiscoveryMechanics();
    }

    /**
     * Set the initial display icons correctly, by calling the service for the bluetooth
     * status
     */
    private void btInitialDisplayMechanics() {
        sendMessageToService(BT_STATUS);
    }

    /**
     * If bt adapter is not found, ie device doesn't support bluetooth,
     * then we shall hide the discovery button and show an error
     */
    private void btIsNotFound() {
        btIcon.setImageResource(R.drawable.ic_action_bt_error);
        btOnOff.setText("Error");
        btDiscovery.setVisibility(View.INVISIBLE);
    }

    /**
     * Method is supposed to be called when bt if on, it will then set the display view
     * items accordingly
     */
    private void btIsOn() {
        btIcon.setImageResource(R.drawable.ic_action_bt_on);
        btOnOff.setText("Turn Off");
        btDiscovery.setVisibility(View.VISIBLE);
    }

    /**
     * Method is supposed to be called when bt if off, it will then set the display view
     * items accordingly
     */
    private void btIsOff() {
        btIcon.setImageResource(R.drawable.ic_action_bt_off);
        btOnOff.setText("Turn On");
        btDiscovery.setVisibility(View.INVISIBLE);
    }

    /**
     * Method is supposed to be called when discovery mode is active, it will then
     * set the display views items accordingly
     */
    private void discoveryOff() {
        btDiscovery.setText("Enable discovery");
    }

    /**
     * Method is supposed to be called when discovery mode is off, it will then
     * set the display views items accordingly
     */
    private void discoveryOn() {
        btDiscovery.setText("Disable discovery");
    }

    /**
     * Creates a button listener, that will turn bluetooth on/off, depending on
     * the bluetooth state. This method will also add a filter action to
     * the broadcast receiver, in order to track changes in the bluetooth state, ie on/off
     */
    private void btTurnOnOffMechanics() {
        btOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "On/off Button click");

                if (btAdapter != null && btAdapter.isEnabled()) {
                    btAdapter.disable();
                }

                else if (btAdapter != null && !btAdapter.isEnabled()) {
                    // Ask user permission
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                }
            }
        });
    }
    /**
     * Method will initiate the necessary recycler view configurations. The recycler view
     * lists all devices available for pairing. An item clicker listener is created, to
     * chose the pairing device
     */
    private void deviceListMechanics() {
        mRecyclerView = findViewById(R.id.deviceListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new DeviceRecyclerAdapter(btArrayDevice);

        sendMessageToService(BT_GET_UNBOUNDED_DEVICE);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sendMessageToService(BT_CREATE_BOUND, btArrayDevice.get(position));
                }
            }
        });
    }

    /**
     * Clears the data in the recycler view. Its used when a new discovery is enabled, since
     * old devices may have gone out of discovery.
     */
    private void resetRecyclerContent() {
        btArrayDevice = new ArrayList<>();
        mAdapter.setArray(btArrayDevice);
        mAdapter.notifyDataSetChanged();
    }

    final short REQUEST_ENABLE_DISCOVERY = 0;

    /**
     * Creates a button listener, that will activate/deactivate discovery mode. It also add
     * a action filter to the broadcast receiver, in order to track change in the scan mode.
     */
    private void btDiscoveryMechanics() {
        btDiscovery.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            if ( btAdapter.isDiscovering() )
                sendMessageToService(BT_END_DISCOVERY);

            else {
                /* Ask for user permission*/
                Log.i(TAG, "Ask user for discovery permission");

                Intent enableBtIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_DISCOVERY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_ENABLE_DISCOVERY:
                if ( resultCode == RESULT_CANCELED ) {
                    Log.d(TAG, "Discovery request failed");
                }

                else {
                    Log.i(TAG, "Discovery request accepted");
                    sendMessageToService(BT_START_DISCOVERY);
                }
                break;

            default:
                break;
        }
    }
}
