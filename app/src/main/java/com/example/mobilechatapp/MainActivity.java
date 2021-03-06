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
        Button settings = (Button)findViewById(R.id.Settings);
        Button uploadImage = (Button)findViewById(R.id.Upload_Picture);

        btConnect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent btConnectIntent = new Intent(getApplicationContext(), BluetoothConnection.class);
                startActivity(btConnectIntent);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settings_button = new Intent(getApplicationContext(), Edit_Profile.class);
                startActivity(settings_button);
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent upload_picture = new Intent(getApplicationContext(), Upload_Picture.class);
                startActivity(upload_picture);
            }
        });
    }
}