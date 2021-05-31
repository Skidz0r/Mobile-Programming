package com.example.mobilechatapp.Fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mobilechatapp.Adapter.UserAdapter;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.ProfileView.ProfileViewer;
import com.example.mobilechatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> mUsers;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_users, container , false);

        recyclerView = view.findViewById(R.id.users_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mUsers= new ArrayList<>();

        readUsers();


        return view;
    }

    private void readUsers()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
               mUsers.clear();

                for(DataSnapshot snap : snapshot.getChildren()) // Cycle throughout the users
                {
                    User user = snap.getValue(User.class);
                    assert user != null;
                    mUsers.add(user);
                }
                userAdapter = new UserAdapter(getContext() ,mUsers);
                recyclerView.setAdapter(userAdapter);
                userAdapter.setOnItemClickListener(new UserAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        Intent profileView = new Intent(getActivity(), ProfileViewer.class);
                        profileView.putExtra("Directory",getUsername(position));
                        startActivity(profileView);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

    }

    public String getUsername(int position)
    {
        return mUsers.get(position).getUsername();
    }

    public String getId(int position)
    {
        return mUsers.get(position).getUserId();
    }
}