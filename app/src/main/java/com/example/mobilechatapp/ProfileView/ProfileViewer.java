package com.example.mobilechatapp.ProfileView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.LoggedInActivity;
import com.example.mobilechatapp.Model.User;
import com.example.mobilechatapp.Profile;
import com.example.mobilechatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileViewer extends AppCompatActivity
{
    private static final int IMAGE_PICK_CODE = 1000 , PERMISSION_CODE =1001;
    DatabaseReference reference;
    TextView Username, Age , City, Gender;
    CircleImageView ProfileImage;
    String UsernameDirectory="";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_viewer);

        /* Profile Features */
        Username = findViewById(R.id.Profile_Name);
        Age = findViewById(R.id.Profile_Age);
        City = findViewById(R.id.Profile_City);
        ProfileImage = findViewById(R.id.image_to_upload);
        Gender = findViewById(R.id.Profile_Gender);

        /* Get directory */
        Bundle directory = getIntent().getExtras();
        UsernameDirectory = directory.getString("Directory");
        reference = FirebaseDatabase.getInstance().getReference("Users").child(UsernameDirectory);


        fillProfile();
    }

    private void fillProfile()
    {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                /* Set user */
                User user = snapshot.getValue(User.class);
                /* Set Username */
                Username.setText(UsernameDirectory);

                /* Set Image */
                if(user.getImageUrl().equals("default"))
                {
                    ProfileImage.setImageResource(R.mipmap.ic_launcher);
                }
                else {

                    String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (ActivityCompat.checkSelfPermission(ProfileViewer.this, Manifest.permission.READ_PHONE_STATE)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(permissions,PERMISSION_CODE);
                        } else {
                            Glide.with(getApplicationContext()).load(user.getImageUrl()).into(ProfileImage);
                        }
                    }
                    //requestPermissions(permissions,PERMISSION_CODE);
                    Glide.with(getApplicationContext()).load(user.getImageUrl()).into(ProfileImage);
                }

                /* Set City */
                if(user.getCity()!=null)
                {
                    City.setText(user.getCity());
                }
                else City.setText("");

                /* Set Age */
                if(user.getAge()!=null)
                {
                    Age.setText(user.getAge());
                }
                else Age.setText("");

                /*Set Gender*/
                if(user.getGender()!=null)
                {
                    Gender.setText(user.getGender());
                    applyGenderColors(user.getGender());
                }
                else Gender.setText("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });
    }

    private void applyGenderColors(String gender)
    {
        if(gender.equals("Male") || gender.equals("male"))
        {
            Gender.setTextColor(getResources().getColor(R.color.male));
        }
        if(gender.equals("Female") || gender.equals("female"))
        {
            Gender.setTextColor(getResources().getColor(R.color.female));
        }
    }

}