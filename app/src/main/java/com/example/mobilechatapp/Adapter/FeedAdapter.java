package com.example.mobilechatapp.Adapter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.FeedActivity;
import com.example.mobilechatapp.Information.FeedPosts;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.R;
import com.google.firebase.database.ValueEventListener;


import java.util.List;

import static androidx.core.app.ActivityCompat.requestPermissions;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder>  {

    private Context context;
    private List<FeedPosts> posts;



    public FeedAdapter(Context context, List<FeedPosts> posts)
    {
        this.context = context;
        this.posts = posts;
    }


    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public ImageView profile_image;
        public TextView post;
        public TextView time;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.usernameFeed);
            profile_image = itemView.findViewById(R.id.profile_image_feed);
            post = itemView.findViewById(R.id.postFeed);
            time = itemView.findViewById(R.id.timeFeed);
        }
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull  ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.feed_item, parent , false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position)
    {

        FeedPosts user = posts.get(position);
        holder.username.setText(user.getUsername());
        holder.post.setText(user.getPostMessage());
        Log.i("FeedAdapterDebug",user.getUsername()+user.getPostMessage()+user.getImageUrl());
        /* Time Settings */
        String time = user.getHours();
        String Year = time.substring(0,4);
        String Month = time.substring(4,6);
        String Day = time.substring(6,8);
        String Hour = time.substring(9,11);
        String Minutes = time.substring(11,13);
        holder.time.setText(Day+"-"+Month+"-"+Year+" "+Hour+":"+Minutes);

        /* Set Image */
        if(user.getImageUrl().equals("default"))
        {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else
        {
            Glide.with(context).load(user.getImageUrl()).into(holder.profile_image);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

}
