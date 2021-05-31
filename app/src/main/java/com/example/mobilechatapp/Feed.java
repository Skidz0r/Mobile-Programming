package com.example.mobilechatapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Feed extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FeedAdapter feedAdapter;
    private List<FeedPosts> Posts;
    Button postOnWall;
    User user;
    Context context = this;

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


        postOnWall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent writeWall = new Intent(Feed.this, PostOnWall.class);
                writeWall.putExtra("user",user);
                startActivity(writeWall);
            }
        });

        readFeed();
    }

    private void readFeed()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Feed");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Posts.clear();

                for(DataSnapshot snap : snapshot.getChildren())
                {
                    FeedPosts user = snap.getValue(FeedPosts.class);
                    Log.i("FeedFirst",user.getUsername()+user.getPostMessage()+snap.getKey());
                    assert user != null;
                    user.setHours(snap.getKey());
                    Posts.add(user);
                }
                Log.i("FeedDebug",Posts.get(0).getUsername());
                feedAdapter = new FeedAdapter(context , Posts);
                recyclerView.setAdapter(feedAdapter);
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }
}