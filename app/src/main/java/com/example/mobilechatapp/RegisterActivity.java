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

import com.example.mobilechatapp.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    EditText username, email, password;
    Button registerButton;
    FirebaseAuth auth;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Set views
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        registerButton = findViewById(R.id.registerButton);
        auth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernameText = username.getText().toString();
                String emailText = email.getText().toString();
                String passwordText = password.getText().toString();

                if(TextUtils.isEmpty(usernameText) || TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText))
                {
                    Toast.makeText(RegisterActivity.this, "All fields are required.",Toast.LENGTH_SHORT).show();
                }
                else if(passwordText.length()<6)
                {
                    Toast.makeText(RegisterActivity.this, "Password must have at least 6 characters", Toast.LENGTH_SHORT).show();
                }
                else {
                    register(usernameText, emailText, passwordText);
                }

            }
        });
    }

    private void register(String username, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener( new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            assert firebaseUser!=null;
                            String userId = firebaseUser.getUid();
                            reference = FirebaseDatabase
                                    .getInstance()
                                    .getReference("Users").child(username);

                            /* Save in database*/
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("UserId", userId);
                            hashMap.put("Username", username);
                            hashMap.put("ImageUrl", "default");
                            hashMap.put("Email",email);
                            reference.setValue(hashMap).addOnCompleteListener( new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        Toast.makeText(RegisterActivity.this, "Account created with sucess!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, LoggedInActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("Directory",username);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            });
                        }
                        else
                        {
                            Toast.makeText(RegisterActivity.this , "You can't register with that email , username or password",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}