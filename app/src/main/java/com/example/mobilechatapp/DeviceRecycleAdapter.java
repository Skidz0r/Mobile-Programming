package com.example.mobilechatapp;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceRecycleAdapter extends RecyclerView.Adapter<DeviceRecycleAdapter.DeviceViewHolder> {

    private ArrayList<BluetoothDevice> mArrayDevice;
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
            deviceAddress = itemView.findViewById(R.id.textView2);

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

    public DeviceRecycleAdapter(ArrayList<BluetoothDevice> arr) {
        mArrayDevice = arr;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice currentDevice = mArrayDevice.get(position);

        holder.deviceName.setText(currentDevice.getName());
        holder.deviceAddress.setText(currentDevice.getAddress());
    }

    @Override
    public int getItemCount() {
        return mArrayDevice.size();
    }
}
