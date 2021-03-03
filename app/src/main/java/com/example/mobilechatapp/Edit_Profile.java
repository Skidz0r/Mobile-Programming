package com.example.mobilechatapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Edit_Profile extends AppCompatActivity {
    
    View screenView;
    Button clickMe;
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

    }
}