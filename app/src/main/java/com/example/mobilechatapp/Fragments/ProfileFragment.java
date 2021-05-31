package com.example.mobilechatapp.Fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {
    private static final int IMAGE_PICK_CODE = 1000 , PERMISSION_CODE =1001;
    CircleImageView profileImage;
    TextView Username;
    DatabaseReference reference;
    String userDirectory;
    User user;
    Button apply;
    EditText ageEdit, cityEdit, genderEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Bundle Directory = getArguments();
        userDirectory = Directory.getString("Directory");

        profileImage = view.findViewById(R.id.profile_image);
        Username = view.findViewById(R.id.Username);
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userDirectory);
        cityEdit = view.findViewById(R.id.cityEdit);
        ageEdit = view.findViewById(R.id.ageEdit);
        genderEdit = view.findViewById(R.id.genderEdit);


        apply = (Button) view.findViewById(R.id.Apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                String ageToApply = ageEdit.getText().toString();
                String cityToApply = cityEdit.getText().toString();
                String genderToApply = genderEdit.getText().toString();

                if(ageToApply!=null && !ageToApply.isEmpty() &&
                cityToApply!=null & !cityToApply.isEmpty() && checkGenderConditions(genderToApply))
                {
                    reference.child("Age").setValue(ageToApply);
                    reference.child("City").setValue(cityToApply);
                    reference.child("Gender").setValue(genderToApply);
                    user.setAge(ageToApply);
                    user.setCity(cityToApply);
                    user.setGender(genderToApply);
                }

            }
        });

        InitializeUser();
        setProfile();


        return view;
    }


    private boolean checkGenderConditions(String gender)
    {
        if(gender.equals("Male")  ||
          gender.equals("male")   ||
          gender.equals("Female") ||
          gender.equals("female")
        ){ return true;}
        return false;
    }


    private void setProfile()
    {
        /* Set Username */
        Username.setText(userDirectory);

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // check runtime permissions
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == RESULT_OK && requestCode==IMAGE_PICK_CODE)
        {
            Uri imageUri = data.getData();  // Store the Image in a URI
            String imageUrl = imageUri.toString(); // Store the image in String to save it in Firebase.
            Glide.with(getActivity()).load(imageUrl).into(profileImage);

            /* Save to Database */
            reference.child("ImageUrl").setValue(imageUrl);
            user.setImageUrl(imageUrl);

        }
    }


    private void InitializeUser()
    {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                /* Set user */
                user = snapshot.getValue(User.class);
                String imageUrl = user.getImageUrl();
              Glide.with(getActivity()).load(imageUrl).into(profileImage);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
            }
        });
    }
}