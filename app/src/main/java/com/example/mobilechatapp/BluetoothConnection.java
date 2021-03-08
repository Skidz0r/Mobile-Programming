package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class BluetoothConnection extends AppCompatActivity {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;

    final int REQUEST_ENABLE_BT = 0;
    final int REQUEST_DISCOVERY_BT = 1;

    /* Image icon reference for bluetooth state*/
    ImageView btIcon;

    /* Buttons references*/
    Button btOnOff;
    Button btDiscovery;
    Button btShowPaired;

    /* Filters to be used in broadcast receiving*/
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        /* Get Button reference */
        btOnOff = (Button) findViewById(R.id.btOnOffButton);
        btDiscovery = (Button) findViewById(R.id.discoveryOnOff);
        btShowPaired = (Button) findViewById(R.id.showPaired);

        /* Get image reference*/
        btIcon = (ImageView) findViewById(R.id.on_off_btIcon);

        /* Create new object of BluetoothAdapter*/
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        /* Crete new filter*/
        filter = new IntentFilter();

        btInitialDisplayMechanics();
        btTurnOnOffMechanics();
        btDiscoveryMechanics();
        btShowPairedMechanics();

        registerReceiver(receiver, filter);
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
        btDiscovery.setVisibility(View.GONE);
        btShowPaired.setVisibility(View.GONE);
    }

    private void btIsOn() {
        btIcon.setImageResource(R.drawable.ic_action_bt_on);
        btOnOff.setText("Turn Off");
        btDiscovery.setVisibility(View.VISIBLE);
        btShowPaired.setVisibility(View.VISIBLE);
    }

    private void btIsOff() {
        btIcon.setImageResource(R.drawable.ic_action_bt_off);
        btOnOff.setText("Turn On");
        btDiscovery.setVisibility(View.GONE);
        btShowPaired.setVisibility(View.GONE);
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
                    //Por alguma razão não consigo pedir autorização ao user
                    //Intent enableDiscovery = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    //startActivity(enableDiscovery);

                    // Enquanto não resolvermos o problema de cima podemos usar esta linha para testar as coisas
                    btAdapter.startDiscovery();
                }
            }
        });
    }

    private void btShowPairedMechanics() {
        btShowPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ShowPairedDevices.class);
                startActivity(intent);
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
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();

                showToast("Device found:" + deviceName);
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
