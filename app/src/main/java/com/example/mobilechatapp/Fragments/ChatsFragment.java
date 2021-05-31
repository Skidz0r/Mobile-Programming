package com.example.mobilechatapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.Adapter.DeviceRecyclerAdapter;
import com.example.mobilechatapp.ChatActivity;
import com.example.mobilechatapp.Information.UserChat;
import com.example.mobilechatapp.R;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    // Holds a list of connected users
    ArrayList<UserChat> chatUserList = new ArrayList<>();

    String TAG = "ChatFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_bluetooth_chat, container, false);

        mRecyclerView = view.findViewById(R.id.pairedListView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getContext());

        mAdapter = new DeviceRecyclerAdapter(chatUserList);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(position -> {
            Log.i(TAG, "Item clicked in position " + position);
            Intent BluetoothChatMessages = new Intent(getContext(), ChatActivity.class);
            UserChat userChat = chatUserList.get(position);
            BluetoothChatMessages.putExtra("userChatId", userChat.getId());
            startActivity(BluetoothChatMessages);
        });

        return view;
    }

    public void updateUserChatList(ArrayList<UserChat> chatUserList) {
        Log.i(TAG, "Update chat list");
        this.chatUserList = chatUserList;

        if (mAdapter == null) {
            Log.i(TAG, "Its null");
        } else {
            Log.i(TAG, "Not null");
            mAdapter.setArray(chatUserList);
            mAdapter.notifyDataSetChanged();
        }
    }

}