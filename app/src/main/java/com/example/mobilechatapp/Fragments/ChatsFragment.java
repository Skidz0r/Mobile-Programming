package com.example.mobilechatapp.Fragments;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.DeviceRecyclerAdapter;
import com.example.mobilechatapp.Model.UserChat;
import com.example.mobilechatapp.R;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    BluetoothAdapter btAdapter;
    ArrayList<UserChat> btArrayDevice;
    DeviceRecyclerAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_bluetooth_chat, container, false);
        recyclerView = view.findViewById(R.id.pairedListView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btArrayDevice = new ArrayList<>();

        return view;
    }

    public void updateUserChatList(ArrayList<UserChat> userChatList) {
        mAdapter.setArray(userChatList);
        mAdapter.notifyDataSetChanged();
    }

}