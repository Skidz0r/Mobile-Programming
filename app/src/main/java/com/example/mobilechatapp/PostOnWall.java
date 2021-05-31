package com.example.mobilechatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.mobilechatapp.Information.User;

public class PostOnWall extends AppCompatActivity {
    User user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_on_wall);

        /* Get user that was passed */
        Intent intent = getIntent();
        intent.getSerializableExtra("user");

        Log.i("PostOnWall","User got here:"+user.getUsername());
    }
}