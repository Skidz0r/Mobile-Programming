package com.example.mobilechatapp;

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
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class TestService extends AppCompatActivity implements BluetoothState {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_service);

        Button bindButton = (Button)findViewById(R.id.bindToService);
        Button unbindButton = (Button)findViewById(R.id.unbindToService);
        Button sayHelloButton = (Button)findViewById(R.id.sayHello);

        bindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doBindService();
            }
        });

        unbindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doUnbindService();
            }
        });

        sayHelloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( isBoundToService ) {
                    try {
                        Log.i(TAG0, "Client atemps to send test msg");
                        Message msg = Message.obtain(null, TEST_RECEIVE_MSG);
                        msg.replyTo = clientChannel;
                        serviceChannel.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /** Messenger for communicating with service. */
    Messenger serviceChannel = null;
    String TAG0 = "TestClient";
    boolean isBoundToService = false;

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case TEST_RECEIVE_MSG:
                    Log.i(TAG0, "Client received test msg");
                    break;
            }
        }
    }

    final Messenger clientChannel = new Messenger(new MessageHandler());

   private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceChannel = new Messenger(service);
            Log.i(TAG0, "Attached to service");
            isBoundToService = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null, REGISTER_CLIENT);
                msg.replyTo = clientChannel;
                serviceChannel.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
       @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceChannel = null;
            Log.i(TAG0, "Disconnected from service");
        }
    };

   void doBindService() {
       if (isBoundToService) {
           Log.i(TAG0, "Client already bound to service");
       } else {
           // Establish a connection with the service.  We use an explicit
           // class name because there is no reason to be able to let other
           // applications replace our component.
           Log.i(TAG0, "Attempting to bind to a service");
           bindService(new Intent(TestService.this, BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
       }
   }

    void doUnbindService() {
        if ( isBoundToService ) {
            Log.i(TAG0, "Unbinding");

            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if ( serviceChannel != null ) {
                try {
                    Message msg = Message.obtain(null, UNREGISTER_CLIENT);
                    msg.replyTo = clientChannel;
                    serviceChannel.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            // Detach our existing connection.
            unbindService(connection);
        }
        else {
            Log.i(TAG0, "No service to unbind. Client is not bound");
        }

        isBoundToService = false;
    }
}