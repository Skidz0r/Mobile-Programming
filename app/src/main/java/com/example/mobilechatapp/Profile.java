package com.example.mobilechatapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Profile extends AppCompatActivity {

    ImageView mImageView;
    TextView profile_Name_To_Change,profile_Age_To_Change,profile_City_To_Change;
    String str;
    Button upload_pic , profile_settings_button;
    private static final int IMAGE_PICK_CODE = 1000 , PERMISSION_CODE =1001;
    Bitmap bitmap;
    SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mImageView = findViewById(R.id.image_to_upload); // icon profile picture
        profile_settings_button =(Button)findViewById(R.id.Profile_Settings);

        // Attach the variables to the TextView
        profile_Name_To_Change = findViewById(R.id.Profile_Name);
        profile_Age_To_Change = findViewById(R.id.Profile_Age);
        profile_City_To_Change = findViewById(R.id.Profile_City);
        //---

        // Load stored info in shared preferences
        Profile_Info_Check_Then_Load();


        // Button for Profile Settings
        profile_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profile_settings_button = new Intent(getApplicationContext(), Profile_Settings.class);
                startActivity(profile_settings_button);
            }
        });

        // Making the ImageView a clickable button so we can upload our own picture.
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
        // Grab image from gallery ( or other sources )
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

    // Introduction to how Image Uploading / Saving works
    // To save a image and upload it in the user profile, we first turn the Image to Bitmap
    // (1) Image -> Bitmap
    // Then we transform the Bitmap into a string
    // (2)  Bitmap -> String
    // Since its a string, we can now store it into SharedPreferences.
    // To get it from SharedPreferences see function Profile_Info_Check_Then_Load()

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == RESULT_OK && requestCode==IMAGE_PICK_CODE)
        {
            Uri imageUri = data.getData();  // Store the Image in a URI
            Bitmap bitmap = null;           // Initialize bitmap
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri); // Image -> Bitmap (1)
            } catch (IOException e) {
                e.printStackTrace();
                // In case we can't get the image , there's really not much we can do
                // probably image is corruped or other external problem we have no control of.
            }

            mImageView.setImageBitmap(bitmap); // Update to the new uploaded image

            // Save bitmap encoded to shared preferences so we can load it when access Profile
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("User_Profile_Picture", Bitmap_To_String(bitmap)); // Call to encode Bitmap -> String (2)
            // Since a key is a string we have to find a way to transform Bitmap -> String
            // We call the method Bitmap_To_String
            editor.commit();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        Profile_Info_Check_Then_Load();
    }

    public void Profile_Info_Check_Then_Load()
    {
        // This is useful when the user first installs the application , since it has no keys stored
        // text may not appear , that's why we check first if the key exists.
        // The rest is pretty self-explanatory  , it loads the information stored in the Shared Preferences .

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getString("Name_Key","")!="") {     // Check if Name Key exists
            profile_Name_To_Change.setText(sp.getString("Name_Key","")); }

        if(sp.getString("Age_Key","")!="") {      // Check if Age Key exists
            profile_Age_To_Change.setText(sp.getString("Age_Key",""));   }

        if((sp.getString("City_Key",""))!="") {      // Check if City Key exists
            profile_City_To_Change.setText(sp.getString("City_Key","")); }

        if( (sp.getString("User_Profile_Picture","")) != "" ) { // Check if Profile Picture Key exists
            mImageView.setImageBitmap(String_To_Bitmap(sp.getString("User_Profile_Picture","")));
            // We use setImageBitmap() and we get the string from the key we previously stored
            // Since the key is a string we use String_To_Bitmap directly to obtain the image.
        }
    }



    // This method encodes Bitmap -> String
    public static String Bitmap_To_String(Bitmap image) {
        Bitmap immage = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

        Log.d("Image Log:", imageEncoded);
        return imageEncoded;
    }

    // This method encodes String -> Bitmap
    public static Bitmap String_To_Bitmap(String input) {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory
                .decodeByteArray(decodedByte, 0, decodedByte.length);
    }


}