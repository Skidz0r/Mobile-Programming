package com.example.mobilechatapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Edit_Profile extends AppCompatActivity {
    
    View screenView;
    Button clickMe;
    Button clickChangeName;
    int[] color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit__profile);
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
                int numero = random.nextInt(array_Length);
                screenView.setBackgroundColor(color[numero]);
            }
        });

        clickChangeName= (Button) findViewById(R.id.Change_Name_Button);
        clickChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText t = findViewById(R.id.My_Profile_Name);
                String input = t.getText().toString();
                ((TextView) findViewById(R.id.Profile_Name)).setText(input);
                // Make a log
                //Log.d(tag:"info", input);
            }
        });

    }

}