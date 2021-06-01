package com.example.mobilechatapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilechatapp.Adapter.FeedAdapter;
import com.example.mobilechatapp.Information.FeedPosts;
import com.example.mobilechatapp.Information.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.EventTarget;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE =1001;
    private RecyclerView recyclerView;
    private FeedAdapter feedAdapter;
    private List<FeedPosts> Posts;
    Button postOnWall;
    User user;
    Context context = this;
    DatabaseReference reference;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        recyclerView = findViewById(R.id.feed_view);
        postOnWall = findViewById(R.id.postOnWall);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Posts = new ArrayList<>();

        /* Get user that was passed */
        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra("user");


        postOnWall.setOnClickListener(v -> {
            Intent writeWall = new Intent(FeedActivity.this, PostOnWall.class);
            writeWall.putExtra("user",user);
            startActivity(writeWall);
        });

        askPermissions();
        readFeed();

    }


    /*
    * Acess branch "Feed" on the database and reads the posts
    * */
    private void readFeed()
    {
        reference = FirebaseDatabase.getInstance().getReference("Feed");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Posts.clear();

                for(DataSnapshot snap : snapshot.getChildren())
                {
                    Log.i("SNAPSIZE",""+snapshot.getChildrenCount());
                    FeedPosts user = snap.getValue(FeedPosts.class);
                    Log.i("FeedFirst",user.getUsername()+user.getPostMessage()+snap.getKey());
                    assert user != null;
                    user.setHours(snap.getKey());
                    Posts.add(user);
                }
                swapOrder(Posts);
                feedAdapter = new FeedAdapter(context , Posts);
                recyclerView.setAdapter(feedAdapter);

                // To avoid crashes on older androids we only refresh feed when opening it.
                reference.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }

        });
    }

    private void swapOrder(List<FeedPosts> Posts)
    {
        Collections.reverse(Posts);
        // Reverse order in linear time O(n)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void askPermissions()
    {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
        requestPermissions(permissions,PERMISSION_CODE);
    }
}