package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothConnection extends AppCompatActivity {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;
    ArrayList<BluetoothDevice> btArrayDevice;

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecycleAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /* Image icon reference for bluetooth state*/
    ImageView btIcon;

    /* Buttons references*/
    Button btOnOff;
    Button btDiscovery;

    /* Filters to be used in broadcast receiving*/
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

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

        btInitialDisplayMechanics();
        btTurnOnOffMechanics();
        btDiscoveryMechanics();
        deviceListMechanics();

        registerReceiver(receiver, filter);
    }

    private void deviceListMechanics() {
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        mRecyclerView = findViewById(R.id.deviceListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mAdapter = new DeviceRecycleAdapter(btArrayDevice);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecycleAdapter.OnItemClickListener() {
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

    private void showToast(CharSequence text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

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

    private void btIsNotFound() {
        btIcon.setImageResource(R.drawable.ic_action_bt_error);
        btOnOff.setText("Error");
        btDiscovery.setVisibility(View.INVISIBLE);
    }

    private void btIsOn() {
        btIcon.setImageResource(R.drawable.ic_action_bt_on);
        btOnOff.setText("Turn Off");
        btDiscovery.setVisibility(View.VISIBLE);
    }

    private void btIsOff() {
        btIcon.setImageResource(R.drawable.ic_action_bt_off);
        btOnOff.setText("Turn On");
        btDiscovery.setVisibility(View.INVISIBLE);
    }

    private void discoveryOff() {
        btDiscovery.setText("Enable discovery");
    }

    private void discoveryOn() {
        btDiscovery.setText("Disable discovery");
    }

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
                    /* Since we are searching for new devices, we will eliminate the current devices
                    * we have on hold, because some of them may not be available*/
                    btArrayDevice.clear();
                    mAdapter.notifyDataSetChanged();

                    /* Ask for user permission*/
                    //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    //startActivity(enableBtIntent);

                    btAdapter.startDiscovery();
                }
            }
        });
    }

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

                // Check if device is not bonded and has a name
                if ( device.getBondState() == BluetoothDevice.BOND_NONE && device.getName() != null) {
                    // Add device to array
                    btArrayDevice.add(device);
                    // Notify change
                    mAdapter.notifyDataSetChanged();
                }
                String deviceName = device.getName();

                showToast("Device found:" + deviceName);
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
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

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

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);

        super.onDestroy();
    }
}
