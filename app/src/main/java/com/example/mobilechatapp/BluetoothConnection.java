package com.example.mobilechatapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class BluetoothConnection extends AppCompatActivity {
    /* Bluetooth stuff*/
    BluetoothAdapter btAdapter;

    final int REQUEST_ENABLE_BT = 0;

    /* Image icon for bluetooth state*/
    ImageView btIcon;

    /* Buttons ids*/
    Button btOn;
    Button btOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        btOn = (Button) findViewById(R.id.btOnButton);
        btOff = (Button) findViewById(R.id.btOffButton);

        btIcon = (ImageView) findViewById(R.id.on_off_btIcon);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothIconSetUp();
        bluetoothSetUp();
    }

    private void bluetoothIconSetUp() {
        /* Check if bluetooth is available*/
        if (btAdapter == null) {
            btIcon.setImageResource(R.drawable.ic_action_bt_error);
        }

        // Check if is on
        else if (btAdapter.isEnabled()) {
            btIcon.setImageResource(R.drawable.ic_action_bt_on);
        }

        // Check if is off
        else {
            btIcon.setImageResource(R.drawable.ic_action_bt_off);
        }
    }

    private void bluetoothSetUp() {
        /* Turn on button event*/
        btOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Check if bluetooth is off, if so, ask to turn it on*/
                if (btAdapter != null && !btAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        });

        /* Turn on button event*/
        btOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Check if bluetooth is on, if so turn it off*/
                if (btAdapter != null && btAdapter.isEnabled()) {
                    btAdapter.disable();
                    btIcon.setImageResource(R.drawable.ic_action_bt_off);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if ( resultCode == RESULT_OK )
                    btIcon.setImageResource(R.drawable.ic_action_bt_on);

                else
                    btIcon.setImageResource(R.drawable.ic_action_bt_off);

                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
