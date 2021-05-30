package com.example.mobilechatapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.Model.UserChat;

import java.util.ArrayList;

public class DeviceRecyclerAdapter extends RecyclerView.Adapter<DeviceRecyclerAdapter.DeviceViewHolder> {

    private ArrayList<UserChat> mArrayDevice;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public TextView deviceName;
        public TextView deviceAddress;

        public DeviceViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);

            deviceName = itemView.findViewById(R.id.textView1);
            //deviceAddress = itemView.findViewById(R.id.textView2);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( listener != null ) {
                        int position = getAdapterPosition();

                        if ( position != RecyclerView.NO_POSITION ) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public DeviceRecyclerAdapter(ArrayList<UserChat> arr) {
        mArrayDevice = arr;
    }

    public void setArray(ArrayList<UserChat> arr) {
        mArrayDevice = arr;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        UserChat userChat = mArrayDevice.get(position);

        holder.deviceName.setText(userChat.getId());
        // holder.deviceAddress.setText(currentDevice.getAddress()); Adress is not needed.
    }

    @Override
    public int getItemCount() {
        return mArrayDevice.size();
    }
}
