package com.example.mobilechatapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    EditText email, password, username;
    Button loginButton;
    FirebaseAuth auth;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        auth= FirebaseAuth.getInstance();

        username = findViewById(R.id.Username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailText = email.getText().toString();
                String passwordText = password.getText().toString();
                String usernameText = username.getText().toString();
                if(TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText) || TextUtils.isEmpty(usernameText))
                {
                         Toast.makeText(LoginActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    reference = FirebaseDatabase.getInstance().getReference("Users").child(usernameText);
                    reference.addValueEventListener(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot)
                        {
                            if(snapshot.exists())
                            {
                                auth.signInWithEmailAndPassword(emailText,passwordText)
                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task)
                                            {
                                                if(task.isSuccessful())
                                                {
                                                    Intent intent = new Intent(LoginActivity.this, LoggedInActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    intent.putExtra("Directory",usernameText);
                                                    startActivity(intent);
                                                    finish();
                                                }
                                                else
                                                {
                                                    Toast.makeText(LoginActivity.this, "Authentication failed!",Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                            else
                            {
                                Toast.makeText(LoginActivity.this, "Username doesn't exist", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error)
                        {
                        }
                    });
                }
            }
        });
    }



}