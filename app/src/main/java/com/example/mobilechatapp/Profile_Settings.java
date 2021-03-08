package com.example.mobilechatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Profile_Settings extends AppCompatActivity {


    View screenView;
    Button clickChangeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__settings);

        //Get shared preferences
        SharedPreferences prefs = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //---

        clickChangeName= (Button) findViewById(R.id.Apply_Changes);
        clickChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sp.edit(); // shared preferences editor
                // Save Name to string
                EditText name_edit = findViewById(R.id.My_Profile_Name);
                String name_input = name_edit.getText().toString();
                // Save Age to string
                EditText age_edit = findViewById(R.id.My_Profile_Age);
                String age_input = age_edit.getText().toString();
                // Save City to string
                EditText city_edit = findViewById(R.id.My_Profile_City);
                String city_input = city_edit.getText().toString();
                // Applying the changes
                editor.putString("Name_Key", name_input);
                editor.putString("Age_Key", age_input);
                editor.putString("City_Key", city_input);
                editor.commit();
                // Make a log
                //Log.d(tag:"info", input);

                // End of applying name change
            }
        });
    }

}