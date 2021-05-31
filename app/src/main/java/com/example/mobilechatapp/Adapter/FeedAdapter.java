package com.example.mobilechatapp.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Information.FeedPosts;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.R;
import com.google.firebase.database.ValueEventListener;


import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

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

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.usernameFeed);
            profile_image = itemView.findViewById(R.id.profile_image_feed);
            post = itemView.findViewById(R.id.postFeed);
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
        holder.post.setText(user.getPost());
        /*if(user.getImageUrl().equals("default"))
        {
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        }
        else
        {
            Glide.with(context).load(user.getImageUrl()).into(holder.profile_image);
        }*/
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

}
