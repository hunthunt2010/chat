package com.example.david.chat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;
import com.sinch.android.rtc.messaging.WritableMessage;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ListUsersActivity extends ActionBarActivity {

    public ArrayList<String> names;
    public ProgressDialog progressDialog;
    public BroadcastReceiver broadcastReceiver;
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private MessageService.MessageServiceInterface messageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_users);
        bindService(new Intent(this, MessageService.class), serviceConnection, BIND_AUTO_CREATE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        //broadcast receiver to listen for the broadcast
        //from MessageService
        broadcastReceiver = new MyBroadcast();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("com.fuckthis"));
        String currentUserId = ParseUser.getCurrentUser().getObjectId();
        names = new ArrayList<String>();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        //don't include yourself
        query.whereNotEqualTo("objectId", currentUserId);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < userList.size(); i++) {
                        names.add(userList.get(i).getUsername().toString());
                    }
                    ListView usersListView = (ListView) findViewById(R.id.usersListView);
                    ArrayAdapter<String> namesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
                            R.layout.user_list_item, names);
                    usersListView.setAdapter(namesArrayAdapter);
                    usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                            openConversation(names, i);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error loading user list",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        Button logoutButton = (Button) findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), MessageService.class));
                ParseUser.logOut();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStop() {
        if (messageService != null) {
            messageService.stopListening();
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        if (messageService != null) {
            messageService.stopListening();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if (messageService != null) {
            messageService.startListening();
        }
        super.onResume();
    }

    public class MyBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
                Log.v("myTag", "This thing got called");
                Boolean success = intent.getBooleanExtra("success", false);
                progressDialog.dismiss();
                //show a toast message if the Sinch
                //service failed to start
                if (!success) {
                    Toast.makeText(getApplicationContext(), "Messaging service failed to start", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "It worked", Toast.LENGTH_LONG).show();
                }
        }
    }

    public void openConversation(ArrayList<String> names, int pos) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", names.get(pos));
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            messageService = (MessageService.MessageServiceInterface) iBinder;
//            messageService.addMessageClientListener(messageClientListener);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messageService = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    private class MyMessageClientListener implements MessageClientListener {
//        //Notify the user if their message failed to send
//        @Override
//        public void onMessageFailed(MessageClient client, Message message,
//                                    MessageFailureInfo failureInfo) {
////            Toast.makeText(MessagingActivity.this, "Message failed to send.", Toast.LENGTH_LONG).show();
//            Toast.makeText(ListUsersActivity.this, failureInfo.getSinchError().getMessage(), Toast.LENGTH_LONG).show();
//        }
//        @Override
//        public void onIncomingMessage(MessageClient client, Message message) {
//            //Display an incoming message
//            if (message.getSenderId().equals(recipientId)) {
//                WritableMessage writableMessage = new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
//                messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_INCOMING);
//
//                NotificationCompat.Builder mBuilder =
//                        new NotificationCompat.Builder(MessagingActivity.this)
//                                .setSmallIcon(R.drawable.ic_launcher)
//                                .setContentTitle("My notification")
//                                .setContentText("Hello World!");
//                // Creates an explicit intent for an Activity in your app
//                Intent resultIntent = new Intent(MessagingActivity.this, MainActivity.class);
//
//// The stack builder object will contain an artificial back stack for the
//// started Activity.
//// This ensures that navigating backward from the Activity leads out of
//// your application to the Home screen.
//                TaskStackBuilder stackBuilder = TaskStackBuilder.create(MessagingActivity.this);
//// Adds the back stack for the Intent (but not the Intent itself)
//                stackBuilder.addParentStack(MainActivity.class);
//// Adds the Intent that starts the Activity to the top of the stack
//                stackBuilder.addNextIntent(resultIntent);
//                PendingIntent resultPendingIntent =
//                        stackBuilder.getPendingIntent(
//                                0,
//                                PendingIntent.FLAG_UPDATE_CURRENT
//                        );
//                mBuilder.setContentIntent(resultPendingIntent);
//                NotificationManager mNotificationManager =
//                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//// mId allows you to update the notification later on.
//                mNotificationManager.notify(1, mBuilder.build());
//            }
//        }
//        @Override
//        public void onMessageSent(MessageClient client, Message message, String recipientId) {
//            //Display the message that was just sent
//            //Later, I'll show you how to store the
//            //message in Parse, so you can retrieve and
//            //display them every time the conversation is opened
//            final WritableMessage writableMessage = new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
//            //only add message to parse database if it doesn't already exist there
//            ParseQuery<ParseObject> query = ParseQuery.getQuery("ParseMessage");
//            query.whereEqualTo("sinchId", message.getMessageId());
//            query.findInBackground(new FindCallback<ParseObject>() {
//                @Override
//                public void done(List<ParseObject> messageList, com.parse.ParseException e) {
//                    if (e == null) {
//                        if (messageList.size() == 0) {
//                            ParseObject parseMessage = new ParseObject("ParseMessage");
//                            parseMessage.put("senderId", currentUserId);
//                            parseMessage.put("recipientId", writableMessage.getRecipientIds().get(0));
//                            parseMessage.put("messageText", writableMessage.getTextBody());
//                            parseMessage.put("sinchId", writableMessage.getMessageId());
//                            parseMessage.saveInBackground();
//                            messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_OUTGOING);
//                        }
//                    }
//                }
//            });
//        }
//        //Do you want to notify your user when the message is delivered?
//        @Override
//        public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {}
//        //Don't worry about this right now
//        @Override
//        public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {
//            //get the id that is registered with Sinch
//            final String regId = new String(pushPairs.get(0).getPushData());
//            //use an async task to make the http request
//            class SendPushTask extends AsyncTask<Void, Void, Void> {
//                @Override
//                protected Void doInBackground(Void... voids) {
//                    HttpClient httpclient = new DefaultHttpClient();
//                    //url of where your backend is hosted, can't be local!
//                    HttpPost httppost = new HttpPost("https://pacific-island-2683.herokuapp.com?reg_id=" + regId);
//                    try {
//                        HttpResponse response = httpclient.execute(httppost);
//                        ResponseHandler<String> handler = new BasicResponseHandler();
//                        Log.d("HttpResponse", handler.handleResponse(response));
//                    } catch (ClientProtocolException e) {
//                        Log.d("ClientProtocolException", e.toString());
//                    } catch (IOException e) {
//                        Log.d("IOException", e.toString());
//                    }
//                    return null;
//                }
//            }
//            (new SendPushTask()).execute();
////            final WritableMessage writableMessage = new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
////
////            ParseQuery userQuery = ParseUser.getQuery();
////            userQuery.whereEqualTo("objectId", writableMessage.getRecipientIds().get(0));
////
////            ParseQuery pushQuery = ParseInstallation.getQuery();
////            pushQuery.whereMatchesQuery("user", userQuery);
////
////            // Send push notification to query
////            ParsePush push = new ParsePush();
////            push.setQuery(pushQuery); // Set our Installation query
////            push.setMessage("Testing" + " sent you a message");
////            push.sendInBackground();
//        }
//    }
}
