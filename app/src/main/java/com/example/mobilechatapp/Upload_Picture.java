package com.example.mobilechatapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Upload_Picture extends AppCompatActivity {

    ImageView mImageView;
    Button upload_pic;

    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload__picture);

        //VIEWS
        mImageView = findViewById(R.id.image_to_upload);
        upload_pic = findViewById(R.id.button_upload_image);

        //handles
        upload_pic.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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
    }
}
