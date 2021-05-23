package com.example.mobilechatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.example.mobilechatapp.Fragments.ChatsFragment;
import com.example.mobilechatapp.Fragments.ProfileFragment;
import com.example.mobilechatapp.Fragments.UsersFragment;
import com.example.mobilechatapp.Model.User;
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

    FirebaseUser fireBaseUser;
    DatabaseReference reference;

    // Default android bluetooth adapter
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    // Holds a list of known devices
    ArrayList<BluetoothDevice> knownDevices = new ArrayList<>();

    /* Recycler stuff */
    RecyclerView mRecyclerView;
    DeviceRecyclerAdapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    /**
     * Buttons
     */
    Button discovery;

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
    final String TAG = "BluetoothChat";


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

                case BT_GET_DEVICES:
                    knownDevices = (ArrayList<BluetoothDevice>) msg.obj;
                    mAdapter.setArray(knownDevices);
                    mAdapter.notifyDataSetChanged();
                    break;

                case BT_END_DISCOVERY:
                    sendMessageToService(BT_GET_DEVICES);
                    break;

                case BT_START_DISCOVERY:
                    resetRecyclerContent();
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

    void sendMessageToService(short flag) {
        sendMessageToService(flag, null);
    }

    void sendMessageToService(short flag, Object obj) {
        try {
            Message msg = Message.obtain(null, flag, obj);
            msg.replyTo = clientChannel;
            serviceChannel.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged_in);

        Toolbar toolbar= findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        /* Get directory for firebase user*/
        Bundle directory = getIntent().getExtras();
        String userDirectory = directory.getString("Directory");
        discovery = findViewById(R.id.discovery);
        profilePicture = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        fireBaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(userDirectory);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                User user = snapshot.getValue(User.class);
                username.setText(userDirectory);
                String imageUrl = user.getImageUrl();
                if(imageUrl.equals("default"))
                {
                    profilePicture.setImageResource(R.mipmap.ic_launcher);
                }
                else
                {
                    Glide.with(LoggedInActivity.this).load(imageUrl).into(profilePicture);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });

        /* Pass username to Profile fragment */
        Fragment profileFragment = new ProfileFragment();
        Bundle Directory = new Bundle();
        Directory.putString("Directory",userDirectory);
        profileFragment.setArguments(Directory);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);

        ViewPagerAdapter viewPagerAdapter= new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new ChatsFragment(),"Chats");
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
            ActivityCompat.requestPermissions(LoggedInActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_CONSTANT);
            ActivityCompat.requestPermissions(LoggedInActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSION_REQUEST_CONSTANT);
            if (!btAdapter.isDiscovering())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_DISCOVERY);
            }
            else
            {
                sendMessageToService(BT_END_DISCOVERY);
            }
        });

        askForKnownDevices();

        initiateRecyclerView();

        sendMessageToService(START_LISTENING);
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
            }
        }
    }

    /**
     * Get the info of the currently known devices, into an array list
     */
    public void askForKnownDevices() {
        sendMessageToService(BT_GET_DEVICES);
    }

    /**
     * Clears the data in the recycler view. Its used when a new discovery is enabled, since
     * old devices may have gone out of discovery.
     */
    private void resetRecyclerContent()
    {
        knownDevices = new ArrayList<>();
        mAdapter.setArray(knownDevices);
        mAdapter.notifyDataSetChanged();
    }


    /**
     * Method will initiate the necessary mumbo jumbo of the recycler view, it will
     * then create a list paired devices, ready to connect and chat.
     * An item clicker listener is created, that creates a connection between devices
     */
    public void initiateRecyclerView() {
        mRecyclerView = findViewById(R.id.pairedListView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);

        mAdapter = new DeviceRecyclerAdapter(knownDevices);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(position -> {
            Log.i(TAG, "Item clicked");
            sendMessageToService(CONNECT, knownDevices.get(position));
            Intent openChat = new Intent(LoggedInActivity.this, BluetoothChatMessages.class);
            openChat.putExtra("device", knownDevices.get(position));
            startActivity(openChat);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.logout:FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(LoggedInActivity.this,MainActivity.class));
                finish();
                return true;
        }
        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter
    {
        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(FragmentManager fm )
        {
            super(fm);
            this.fragments= new ArrayList<>();
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

        public void addFragment(Fragment fragment, String title)
        {
            fragments.add(fragment);
            titles.add(title);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

}