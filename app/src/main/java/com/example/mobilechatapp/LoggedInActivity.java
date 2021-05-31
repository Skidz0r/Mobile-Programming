package com.example.mobilechatapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Fragments.ChatsFragment;
import com.example.mobilechatapp.Fragments.ProfileFragment;
import com.example.mobilechatapp.Fragments.UsersFragment;
import com.example.mobilechatapp.Information.BluetoothState;
import com.example.mobilechatapp.Information.User;
import com.example.mobilechatapp.Information.UserChat;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class LoggedInActivity extends AppCompatActivity implements BluetoothState {

    CircleImageView profilePicture;
    TextView username;
    User userToPass;

    FirebaseUser fireBaseUser;
    DatabaseReference reference;

    // Default android bluetooth adapter
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * List of devices that we are connected to
     */
    ArrayList<UserChat> userChatList = new ArrayList<>();

    /**
     * Buttons
     */
    Button discovery;
    Button feed;

    /**
     * Messenger for communicating with service.
     */
    Messenger serviceChannel = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean isBoundToService = false;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger clientChannel = new Messenger(new MessageHandler());

    /**
     * Tag used in Logs to identify class
     */
    final String TAG = "LoggedIn";


    /**
     * Handler of incoming messages from service.
     */
    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Message received: " + msg.what);

            switch (msg.what) {
                case REGISTER_CLIENT:
                    initialSetUp();
                    break;

                case NEW_USER:
                case REMOVE_USER:
                    sendMessageToService(GET_USER_LIST);
                    break;

                case GET_USER_LIST:
                    Log.i(TAG, "User list update");
                    userChatList = (ArrayList<UserChat>) msg.obj;

                    // Update fragment data
                    ((ChatsFragment) chatsFragment).updateUserChatList(userChatList);
                    break;

                case BT_END_DISCOVERY:
                    discovery.setText("Start discovery");
                    break;

                case BT_START_DISCOVERY:
                    discovery.setText("End discovery");
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceChannel = new Messenger(service);
            Log.i(TAG, "Attached to service");
            isBoundToService = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            sendMessageToService(REGISTER_CLIENT);
        }

        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceChannel = null;
            Log.i(TAG, "Disconnected from service");
        }
    };

    /**
     * Bind to service
     */
    void doBindService() {
        if (isBoundToService) {
            Log.i(TAG, "Client already bound to service");
        } else {
            // Establish a connection with the service.  We use an explicit
            // class name because there is no reason to be able to let other
            // applications replace our component.
            Log.i(TAG, "Attempting to bind to a service");
            bindService(new Intent(LoggedInActivity.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Send message to service with a identifier flag
     *
     * @param flag identifier
     */
    void sendMessageToService(short flag) {
        sendMessageToService(flag, null);
    }

    /**
     * Send message to service with a identifier flag and an object
     *
     * @param flag identifier
     * @param obj  object
     */
    void sendMessageToService(short flag, Object obj) {
        try {
            Message msg = Message.obtain(null, flag, obj);
            msg.replyTo = clientChannel;
            serviceChannel.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    Fragment chatsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        /* Get directory for firebase user*/
        Bundle directory = getIntent().getExtras();
        String userDirectory = directory.getString("Directory");
        discovery = findViewById(R.id.discovery);
        profilePicture = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        feed = findViewById(R.id.Feed);


        fireBaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userDirectory);



        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                userToPass = user; // To pass later on
                username.setText(userDirectory);
                String imageUrl = user.getImageUrl();
                if (imageUrl.equals("default")) {
                    profilePicture.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(LoggedInActivity.this).load(imageUrl).into(profilePicture);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        feed.setOnClickListener(v ->
        {
            Intent feed = new Intent(LoggedInActivity.this,Feed.class);
            feed.putExtra("user",userToPass);
            startActivity(feed);
        });

        /* Pass username to Profile fragment */
        Fragment profileFragment = new ProfileFragment();
        Bundle Directory = new Bundle();
        Directory.putString("Directory", userDirectory);
        profileFragment.setArguments(Directory);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        Log.i(TAG, "Passou===");
        chatsFragment = new ChatsFragment();

        viewPagerAdapter.addFragment(chatsFragment, "Chats");
        viewPagerAdapter.addFragment(new UsersFragment(), "Users");
        viewPagerAdapter.addFragment(profileFragment, "Profile");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        doBindService();
    }

    private final short REQUEST_ENABLE_DISCOVERY = 1;

    public void initialSetUp() {
        discovery.setOnClickListener(v ->
        {
            ActivityCompat.requestPermissions(LoggedInActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CONSTANT);
            ActivityCompat.requestPermissions(LoggedInActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_CONSTANT);

            if (!btAdapter.isDiscovering()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_DISCOVERY);
            }
            else {
                sendMessageToService(BT_END_DISCOVERY);
            }
        });

        sendMessageToService(START_LISTENING);

        askForUserChatList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_DISCOVERY) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Discovery request failed");
            } else {
                Log.i(TAG, "Discovery request accepted");
                sendMessageToService(BT_START_DISCOVERY);

                discovery.setText("Stop discovery");
            }
        }
    }

    public void askForUserChatList() {
        sendMessageToService(GET_USER_LIST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(LoggedInActivity.this, MainActivity.class));
                finish();
                return true;
        }
        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();

        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    public ArrayList<UserChat> getUserChatList() {
        return userChatList;
    }
}