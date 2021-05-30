package com.example.mobilechatapp.Fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mobilechatapp.Adapter.UserAdapter;
import com.example.mobilechatapp.BluetoothChat;
import com.example.mobilechatapp.BluetoothChatMessages;
import com.example.mobilechatapp.DeviceRecyclerAdapter;
import com.example.mobilechatapp.LoggedInActivity;
import com.example.mobilechatapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    BluetoothAdapter btAdapter;
    ArrayList<BluetoothDevice> btArrayDevice;
    DeviceRecyclerAdapter mAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.activity_bluetooth_chat, container , false);
        recyclerView = view.findViewById(R.id.pairedListView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();
        find_bonded_devices();
        return view;
    }

    private void find_bonded_devices()
    {
        // If service hasn't searched yet for devices then the arrayList will be null
        ArrayList<BluetoothDevice> knownDevices = ((LoggedInActivity)getActivity()).getDevices();

        mAdapter = new DeviceRecyclerAdapter(knownDevices);

        recyclerView.setLayoutManager(recyclerView.getLayoutManager());
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new DeviceRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent BluetoothChatMessages = new Intent(getContext(), com.example.mobilechatapp.BluetoothChatMessages.class);
                BluetoothDevice device = btArrayDevice.get(position);
                BluetoothChatMessages.putExtra("btdevice",device);
                startActivity(BluetoothChatMessages);
            }
        });
    }


}