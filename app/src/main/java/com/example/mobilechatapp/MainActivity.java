package com.example.mobilechatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            Button btConnect = (Button)findViewById(R.id.bluetoothSettingsButton);
            Button profile = (Button)findViewById(R.id.profileButton);
            Button chat = (Button)findViewById(R.id.chatButton);
            Button register = findViewById(R.id.registerButton);
            Button login = findViewById(R.id.loginButton);


        btConnect.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent btConnectIntent = new Intent(getApplicationContext(), BluetoothSetting.class);
                    startActivity(btConnectIntent);
                }
            });

            profile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent profile_button = new Intent(getApplicationContext(), Profile.class);
                    startActivity(profile_button);
                }
            });

            chat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent chatIntent = new Intent(getApplicationContext(), BluetoothChat.class);
                    startActivity(chatIntent);
                }
            });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(getApplicationContext(), RegisterActivity.class );
                startActivity(registerIntent);
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class );
                startActivity(loginIntent);
            }
        });
    }
}