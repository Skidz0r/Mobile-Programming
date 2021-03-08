package com.example.mobilechatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btConnect = (Button)findViewById(R.id.BluetoothSettings);

        btConnect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent btConnectIntent = new Intent(getApplicationContext(), BluetoothConnection.class);
                startActivity(btConnectIntent);
            }
        });
    }
}