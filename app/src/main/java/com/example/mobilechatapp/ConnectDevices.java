package com.example.mobilechatapp;

import android.Manifest;
import android.annotation.TargetApi;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ConnectDevices extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // Array list where new devices are gonna be
    public ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    // List that we can later click on each device
    public DeviceListAdapter DeviceListAdapter;

    // Adapter and List
    BluetoothAdapter mBluetoothAdapter;
    ListView New_Devices; // Later the ListView is passed to DeviceListAdapter class


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_devices);
        New_Devices = (ListView) findViewById(R.id.NewDevices);
        New_Devices.setOnItemClickListener(ConnectDevices.this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btnDiscover();    // Start searching for new devices
    }

    private void showToast(CharSequence text) {
        // Show message to User
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Create a onItemClick for each item on the List Of Devices
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // Once we click on a bluetooth device first thing to do is cancel discovery
        mBluetoothAdapter.cancelDiscovery();
        String Device_Name = mDevices.get(i).getName(); // Get recently discovered device name
        String Device_Address = mDevices.get(i).getAddress(); // and Address aswell (may be useful later)

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            showToast("Trying to pair with +"+Device_Name); // User message
            mDevices.get(i).createBond();
        }
    }

    private BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mDevices.add(device); // Add the device on our ArrayList
                // Create a List using the layout DeviceListAdapter
                DeviceListAdapter = new DeviceListAdapter(context, R.layout.activity_device_list_adapter, mDevices);
                // Store the new device on the list
                New_Devices.setAdapter(DeviceListAdapter);
            }
        }
    };

    // Once we click "Connect Device" Button , it will start searching for new devices in the area
    public void btnDiscover() {
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            checkBTPermissions(); //check BT permissions in manifest
            mBluetoothAdapter.startDiscovery(); // Start searching new devices
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver1, discoverDevicesIntent);
            // Once we discover a device we pass it to broad            // Once we discover a device we pass it to broadcastReceiver castReceiver
        }
        if(!mBluetoothAdapter.isDiscovering()){
            checkBTPermissions(); //check BT permissions in manifest
            mBluetoothAdapter.startDiscovery(); // Start searching new devices
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver1, discoverDevicesIntent);
            // Once we discover a device we pass it to broadcastReceiver
        }
    }

    // Ordinary function to check permissions
    private void checkBTPermissions() {
        //@TargetApi(Build.VERSION_CODES.M);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            // No need to check permissions, this means SDK version < LOLLIPOP
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        // In the end we destroy the Broadcast we created
    }
}