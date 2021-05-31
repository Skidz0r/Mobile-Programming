package com.example.mobilechatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.ProfileView.ProfileViewer;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostOnWall extends AppCompatActivity {

    private static final int PERMISSION_CODE =1001;

    User user;
    CircleImageView ProfileImage;
    TextView Username;
    EditText PostInput;
    Button Post;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_on_wall);

        ProfileImage = findViewById(R.id.imageFeed);
        Username = findViewById(R.id.ProfileNameFeed);
        PostInput = findViewById(R.id.PostInput);
        Post = findViewById(R.id.PostButton);

        /* Get user that was passed */
        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra("user");

        Log.i("PostOnWallDebug","Username:"+user.getUsername());
        setProfileImage();
        setUsername();
        setButton();
    }

    private void setUsername()
    {
        Username.setText(user.getUsername());
    }

    private void setProfileImage()
    {
        /* Set Image */
        if(user.getImageUrl().equals("default"))
        {
            ProfileImage.setImageResource(R.mipmap.ic_launcher);
        }
        else {
            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
            if (Build.VERSION.SDK_INT >= 23) {
                if (ActivityCompat.checkSelfPermission(PostOnWall.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions,PERMISSION_CODE);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageUrl()).into(ProfileImage);
                }
            }
            Glide.with(getApplicationContext()).load(user.getImageUrl()).into(ProfileImage);
        }
    }

    private void setButton()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        String time = sdf.format(new Date());
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Feed").child(time);
        Post.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String Username = user.getUsername();
                String ImageUrl = user.getImageUrl();
                String PostMessage;
                if(PostInput.getText().toString() != "" && PostInput.getText().toString() != null)
                {
                    PostMessage = PostInput.getText().toString();
                    reference.child("Username").setValue(Username);
                    reference.child("ImageUrl").setValue(ImageUrl);
                    reference.child("PostMessage").setValue(PostMessage);
                    Toast.makeText( context , "Your post was successful.", Toast.LENGTH_LONG);
                    finish();
                }
                else Toast.makeText( context , "Post not allowed , try again.", Toast.LENGTH_LONG);

            }
        });
    }
}