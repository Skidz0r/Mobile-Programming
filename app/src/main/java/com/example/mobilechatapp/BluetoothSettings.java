package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothSettings extends AppCompatActivity {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;
    ArrayList<BluetoothDevice> btArrayDevice;

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /* Image icon reference for bluetooth state*/
    ImageView btIcon;

    /* Buttons references*/
    Button btOnOff;
    Button btDiscovery;

    /* Log tags*/
    private final String TAG0 = "askUser";
    private final String TAG1 = "discovery";

    /* Request discovery code*/
    private final int REQUEST_ENABLE_DISCOVERY = 0;

    /* Filters to be used in broadcast receiving*/
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_settings);

        /* Get Button reference */
        btOnOff = (Button) findViewById(R.id.btOnOffButton);
        btDiscovery = (Button) findViewById(R.id.discoveryOnOff);

        /* Get image reference*/
        btIcon = (ImageView) findViewById(R.id.on_off_btIcon);

        /* Create new object of BluetoothAdapter*/
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        /* Crete new filter*/
        filter = new IntentFilter();

        deviceListMechanics();
        btInitialDisplayMechanics();
        btTurnOnOffMechanics();
        btDiscoveryMechanics();

        /* Register the receiver with the android system. The filters were added with previous
           Methods calls*/
        registerReceiver(receiver, filter);
    }

    /**
     * Method will initiate the necessary recycler view configurations. The recycler view
     * lists all devices available for pairing. An item clicker listener is created, to
     * chose the pairing device
     */
    private void deviceListMechanics() {
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        mRecyclerView = findViewById(R.id.deviceListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new DeviceRecyclerAdapter(btArrayDevice);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Checks if bt is discovery, if so cancel it
                if ( btAdapter.isDiscovering() )
                    btAdapter.cancelDiscovery();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    btArrayDevice.get(position).createBond();
                }
            }
        });
    }

    /**
     * Clears the data in the recycler view. Its used when a new discovery is enabled, since
     * old devices may have gone out of discovery.
     */
    private void resetRecyclerContent() {
        btArrayDevice.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Simple app notification, with a message.
     * @param text Notification message
     */
    private void showToast(CharSequence text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Method will configure the initial display of the activity.
     */
    private void btInitialDisplayMechanics() {
        /* Check if bluetooth is available*/
        if (btAdapter == null)
            btIsNotFound();

        // Check if is on
        else if (btAdapter.isEnabled())
            btIsOn();

        // Check if is off
        else
            btIsOff();

        // Check if bt is discovering
        if ( btAdapter.isDiscovering() )
            discoveryOn();

        else
            discoveryOff();
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
        resetRecyclerContent();
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
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        btOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
     * Creates a button listener, that will activate/deactivate discovery mode. It also add
     * a action filter to the broadcast receiver, in order to track change in the scan mode.
     */
    private void btDiscoveryMechanics() {
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        btDiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( btAdapter.isDiscovering() )
                    btAdapter.cancelDiscovery();

                else {
                    /* Ask for user permission*/
                    Log.i(TAG0, "Ask user for discovery permission");

                    Intent enableBtIntent =
                            new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_DISCOVERY);
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_ENABLE_DISCOVERY:
                if ( resultCode == RESULT_CANCELED ) {
                    Log.d(TAG0, "Discovery request failed");
                }

                else {
                    resetRecyclerContent();

                    Log.i(TAG0, "Discovery request accepted");
                    btAdapter.startDiscovery();
                }
                break;

            default:
                break;
        }
    }

    /**
     * Defines the broadcast receiver. This receiver will track certain bluetooth changes.
     * Since we register an action to receive warning when a new device is found, this receiver
     * is responsible for updating the list view, defined previously.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if ( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action) ) {
                showToast("Scanning for devices");
                discoveryOn();
            }

            else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) ) {
                showToast("Scanning completed");
                discoveryOff();
            }

            else if ( BluetoothDevice.ACTION_FOUND.equals(action) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.i(TAG1, "Device found: " + device.getName());

                // Check if device is not bonded and has a name
                if ( device.getBondState() ==
                        BluetoothDevice.BOND_NONE ) {
                    // Add device to array
                    btArrayDevice.add(device);
                    // Notify change
                    mAdapter.notifyDataSetChanged();
                }

                showToast("Device found:" + device.getName());
            }

            else if ( action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final int state = device.getBondState();

                switch(state) {
                    case BluetoothDevice.BOND_BONDED:
                        showToast("Bonded");
                        btArrayDevice.remove(device);
                        mAdapter.notifyDataSetChanged();
                        break;

                    case BluetoothDevice.BOND_BONDING:
                        showToast("Bonding");
                        break;

                    case BluetoothDevice.BOND_NONE:
                        showToast("Unbounded");
                        break;

                    default:
                        break;
                }
            }

            else if ( BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) ) {
                final int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch(state) {
                    case BluetoothAdapter.STATE_ON:
                        btIsOn();
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        btIsOff();
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        showToast("Turning on bluetooth");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        showToast("Turning off bluetooth");
                        break;

                    default:
                        showToast("Error");
                        break;
                }
            }
        }
    };

    /**
     * On activity destroy, we unregister the current broadcast receiver
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);

        super.onDestroy();
    }
}
