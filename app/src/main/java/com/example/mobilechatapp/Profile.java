package com.example.mobilechatapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Profile extends AppCompatActivity {

    ImageView mImageView;
    TextView profile_Name_To_Change;
    TextView profile_Age_To_Change;
    TextView profile_City_To_Change;
    String str;
    Button upload_pic;
    Button profile_settings_button;
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //upload picture views
        mImageView = findViewById(R.id.image_to_upload);
        //
        profile_settings_button =(Button)findViewById(R.id.Profile_Settings);

        SharedPreferences sharedPref = getSharedPreferences("bgColorFile", Context.MODE_PRIVATE);
        int colorValue = sharedPref.getInt("color", 0);
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(colorValue);
        profile_Name_To_Change = findViewById(R.id.Profile_Name);
        profile_Age_To_Change = findViewById(R.id.Profile_Age);
        profile_City_To_Change = findViewById(R.id.Profile_City);

        // Check if key has value
        Intent intent = getIntent();
        if (intent.hasExtra("key_change_name")) {
            str = getIntent().getExtras().getString("key_change_name");
            profile_Name_To_Change.setText(str);
        }
        if (intent.hasExtra("key_change_age")) {
            str = getIntent().getExtras().getString("key_change_age");
            profile_Age_To_Change.setText(str);
        }
        if (intent.hasExtra("key_change_city")) {
            str = getIntent().getExtras().getString("key_change_city");
            profile_City_To_Change.setText(str);
        }


        profile_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profile_settings_button = new Intent(getApplicationContext(), Profile_Settings.class);
                startActivity(profile_settings_button);
            }
        });

        final ImageView image_to_upload_button = (ImageView) findViewById(R.id.image_to_upload);
        image_to_upload_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // check runtime permissions
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_DENIED)
                    {
                        // permission not guaranted  , request it
                        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        // show popup for runtime permission acess external storage
                        requestPermissions(permissions,PERMISSION_CODE);
                    }
                    else
                    {
                        // permission already garanted
                        pickImageFromGallery();
                    }
                }
                else
                {
                    // system os is less then marshmellow
                    pickImageFromGallery();
                }
            }
        });
    }

    private void pickImageFromGallery()
    {
        // pick image
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_CODE);
    }


    // handle result of runtime permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case PERMISSION_CODE:
            {
                if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    //permission was garanted
                    pickImageFromGallery();
                }
                else
                {
                    // permissions was denied
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode==IMAGE_PICK_CODE)
        {
            // set image to image view
            mImageView.setImageURI(data.getData());
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

}