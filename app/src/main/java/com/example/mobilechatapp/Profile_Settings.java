package com.example.mobilechatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Profile_Settings extends AppCompatActivity {

    View screenView;
    Button clickMe;
    Button clickChangeName;
    int[] color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile__settings);
        color= new int[] // Available Colors
                {
                        Color.BLACK,
                        Color.GREEN,
                        Color.YELLOW,
                        Color.GRAY,
                        Color.BLUE
                };

        screenView = findViewById(R.id.rView);
        clickMe = (Button) findViewById(R.id.Change_Background);
        clickMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int array_Length = color.length;   // Can also implement with i=0 and goes on (i++) then reset
                Random random = new Random();
                int numero = random.nextInt(array_Length-1);
                screenView.setBackgroundColor(color[numero]);

                SharedPreferences sharedPref = Profile_Settings.this.getSharedPreferences("bgColorFile", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("color", color[numero]);
                editor.apply();
            }
        });

        clickChangeName= (Button) findViewById(R.id.Apply_Changes);
        clickChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Apply Name Change
                EditText t = findViewById(R.id.My_Profile_Name);
                String input = t.getText().toString();
                //((TextView) findViewById(R.id.Profile_Name)).setText(input);
                Intent i = new Intent(Profile_Settings.this,Profile.class);
                i.putExtra("key_change_name",input);
                // Make a log
                //Log.d(tag:"info", input);
                // End of applying name change


                // Back to profile
                startActivity(i);
            }
        });
    }
}