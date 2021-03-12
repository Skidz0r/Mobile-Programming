package com.example.mobilechatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class ShowPairedDevices extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> btDevice;

    ListView pairedListView;
    TextView pairedInfoTextView;

    String[] deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_paired_devices);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedListView = (ListView) findViewById(R.id.pairedListView);
        pairedInfoTextView = (TextView) findViewById(R.id.pairedInfoTextView);

        getPairedInfoToList();
    }

    private void getPairedInfoToList() {
        if ( btAdapter == null || !btAdapter.isEnabled() ){
            pairedInfoTextView.setText("Paired Devices: 0");
            return;
        }

        getPairedInfo();
        pairedInfoTextView.setText("Paired Devices: " + btDevice.size());


        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceName);
        pairedListView.setAdapter(arrayAdapter);
    }

    private void getPairedInfo() {
        btDevice = btAdapter.getBondedDevices();

        deviceName = new String[btDevice.size()];

        if ( btDevice.size() > 0 ) {
            int i = 0;

            for( BluetoothDevice device: btDevice ) {
                deviceName[i] = device.getName();
                i++;
            }
        }
    }
}