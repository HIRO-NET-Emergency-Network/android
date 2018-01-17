package edu.neu.winesanh.hironet;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import android.Manifest;
import android.accounts.AccountManager;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.LinearLayout;



import com.bridgefy.sdk.client.BFEnergyProfile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.Config;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;






public class MainActivity extends AppCompatActivity {
//public class MainActivity extends AppCompatActivity
//    implements EasyPermissions.PermissionCallbacks {
    private RecyclerView hmesRecyclerView;
    private hiroMesAdapter hmesAdapter = new hiroMesAdapter(new ArrayList<String>());
    private RecyclerView.LayoutManager hmesLayoutManager;

    private RecyclerView hdevRecyclerView;
    private hiroDevAdapter hdevAdapter = new hiroDevAdapter(new ArrayList<hiroDevice>());
    private RecyclerView.LayoutManager hdevLayoutManager;

    private String consumer_key = "G75xOICupe3KRY7Cr58Z4sE21";
    private String consumer_secret = "glm5P2CgZAbhHnOReJV3gq0aLDtmj0ianSVMcn1RC8mnj9QsuD";
    private String oauth_token = "927302867353993216-VcECcHHWP170E1Qxs2zrM4SOlOQY1ex";
    private String oauth_token_secret = "WLszUF4GfyE4rzpVYOTSMaLnmHbkcE3iwRYDuH8lx3aPF";

    private Twitter twitter;

    public static final String EXTRA_MESSAGE = "edu.neu.winesanh.hironet.MESSAGE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.mainText)).setText("Registering...");
        //Always use the Application context to avoid leaks
        Bridgefy.initialize(getApplicationContext(), "368a4825-bb56-49db-9826-c09c624248c1", new RegistrationListener() {
            @Override
            public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
                // Bridgefy is ready to start

                ((TextView)findViewById(R.id.mainText)).setText("Registration Succeeded");
                Config.Builder builder = new Config.Builder();
                builder.setEnergyProfile(BFEnergyProfile.HIGH_PERFORMANCE);
                builder.setEncryption(false);
                Bridgefy.start(messageListener, stateListener, builder.build());
            }

            @Override
            public void onRegistrationFailed(int errorCode, String message) {
                // Something went wrong: handle error code, maybe print the message
                ((TextView)findViewById(R.id.mainText)).setText("Registration Failed");
            }});

        hmesRecyclerView = findViewById(R.id.my_recycler_view1);
        hmesRecyclerView.setHasFixedSize(true);
        hmesLayoutManager = new LinearLayoutManager(this);
        hmesRecyclerView.setLayoutManager(hmesLayoutManager);
        hmesRecyclerView.setAdapter(hmesAdapter);

        hdevRecyclerView = findViewById(R.id.my_recycler_view2);
        hdevRecyclerView.setHasFixedSize(true);
        hdevLayoutManager = new LinearLayoutManager(this);
        hdevRecyclerView.setLayoutManager(hdevLayoutManager);
        hdevRecyclerView.setAdapter(hdevAdapter);

        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(consumer_key, consumer_secret);
        twitter.setOAuthAccessToken(new AccessToken(oauth_token, oauth_token_secret));

    }

    private MessageListener messageListener = new MessageListener() {

        @Override
        public void onMessageReceived(Message message) {

            String mes_type = message.getContent().get("type").toString();
            //((TextView)findViewById(R.id.mainText3)).setText("Message: " + message.getContent().get("type"));
            if (mes_type.equals("Intro")) {
                hiroDevice new_dev = new hiroDevice(message.getContent().get("device_name").toString(), message.getSenderId(), true);

                hdevAdapter.addDevice(new_dev);
            } else if (mes_type.equals("direct_message")) {
                hmesAdapter.addMessage(hdevAdapter.getDeviceName(message.getSenderId()) + ": " + message.getContent().get("content").toString());
            } else if (mes_type.equals("twitter")) {
                postTweet(message.getSenderId(), message.getContent().get("content").toString());
            } else if (mes_type.equals("twitter_status")) {
                Toast.makeText(MainActivity.this, message.getContent().get("content").toString(), Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onBroadcastMessageReceived(Message message) {

        }

    };

    private StateListener stateListener = new StateListener() {

        @Override
        public void onDeviceConnected(final Device device, Session session) {
            // Do something with the found device
            //((TextView)findViewById(R.id.mainText2)).setText("Device Connected");

            Toast.makeText(MainActivity.this, "Connected to: " + device.getUserId(), Toast.LENGTH_SHORT).show();


            // Build a HashMap object
            HashMap<String, Object> msg = new HashMap<>();
            msg.put("type", "Intro");
            msg.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);

            // Send the message to the specified recipient
            device.sendMessage(msg);

        }

        @Override
        public void onDeviceLost(Device device) {
            // Let your implementation know that a device is no longer available
            //((TextView)findViewById(R.id.mainText2)).setText("Device Lost");
            hdevAdapter.removeDevice(device);
        }

        @Override
        public void onStartError(String message, int errorCode) {
            Toast.makeText(MainActivity.this, "Start Error: " + message, Toast.LENGTH_SHORT).show();
        }
    };

    public class hiroMesAdapter extends RecyclerView.Adapter<hiroMesAdapter.ViewHolder> {
        private ArrayList<String> messages;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView mTextView;
            public ViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public hiroMesAdapter(ArrayList<String> myDataset) {
            this.messages = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public hiroMesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
            // create a new view
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            //holder.mTextView.setText(messages.get(position));
            holder.mTextView.setText(messages.get(position));

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return messages.size();
        }

        public void addMessage(String msg) {
            this.messages.add(msg);
            this.notifyDataSetChanged();
        }
    }

    public class hiroDevAdapter extends RecyclerView.Adapter<hiroDevAdapter.ViewHolder> {
        private ArrayList<hiroDevice> hiro_devices;
        private String targeted_device_uuid = "MakeSureToPopulateThisBeforeSend";

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // each data item is just a string in this case
            public TextView mTextView;
            hiroDevice h_device;

            public ViewHolder(TextView v) {
                super(v);
                mTextView = v;
                v.setOnClickListener(this);
            }

            public void setHiroDevice(hiroDevice h_dev){
                this.h_device = h_dev;

                if (this.h_device.isConnected()){
                    this.mTextView.setTextColor(Color.BLACK);
                } else {
                    this.mTextView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                targeted_device_uuid = h_device.getDeviceUuid();
                Toast.makeText(MainActivity.this, "Clicked on: " + h_device.getDeviceName(), Toast.LENGTH_SHORT).show();
            }
        }

        public String getTargetedUuid(){
            return this.targeted_device_uuid;
        }
        // Provide a suitable constructor (depends on the kind of dataset)
        public hiroDevAdapter(ArrayList<hiroDevice> myDataset) {
            this.hiro_devices = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public hiroDevAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
            // create a new view
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.my_text_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            //holder.mTextView.setText(messages.get(position));
            holder.mTextView.setText(hiro_devices.get(position).getDeviceName());
            holder.setHiroDevice(hiro_devices.get(position));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return this.hiro_devices.size();
        }

        public void addDevice(hiroDevice dev) {
            int device_index = getDeviceIndex(dev.getDeviceUuid());

            if (device_index == -1) {
                this.hiro_devices.add(dev);
            } else {
                //this.hiro_devices.set(device_index, dev);
                this.hiro_devices.get(device_index).setIsConnected(true);
                this.notifyItemChanged(device_index);
            }

            this.notifyDataSetChanged();
        }

        public void removeDevice(Device dev) {
            int device_index = getDeviceIndex(dev.getUserId());

            if (device_index > -1) {
                this.hiro_devices.get(device_index).setIsConnected(false);
                this.notifyItemChanged(device_index);
                //this.hiro_devices.remove(device_index);
                //this.notifyItemRemoved(device_index);
            }
        }

        private int getDeviceIndex(String uuid) {
            for (int i = 0; i < this.hiro_devices.size(); i++) {
                if (this.hiro_devices.get(i).getDeviceUuid().equals(uuid)) return i;
            }

            return -1;
        }

        private String getDeviceName(String uuid){
            for (int i = 0; i < this.hiro_devices.size(); i++) {
                if (this.hiro_devices.get(i).getDeviceUuid().equals(uuid)) return this.hiro_devices.get(i).getDeviceName();
            }

            return "Unknown";
        }
    }

    private void postTweet(String user_uuid, String tweet_content) {
        new PostTweetAsyncTask().execute(user_uuid, tweet_content);
    }

    private class PostTweetAsyncTask extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... tweet_contents) {
            String tweet_content = tweet_contents[1];
            String result[] = {"", ""};

            result[0] = tweet_contents[0]; // user's uuid to send back the status
            try {
                twitter.updateStatus(tweet_content);
            } catch (TwitterException e) {
                result[1] = "Tweet error: " + e.getErrorMessage();
                return result;
            }

            result[1] = "Post Tweet Successfully";

            return result;
        }

        @Override
        protected void onPostExecute(String[] result) {
            //Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();

            // Send back the status to user
            // Build a HashMap object
            HashMap<String, Object> data = new HashMap<>();

            data.put("type", "twitter_status");
            data.put("content", result[1]);

            String rcver_uuid = result[0];

            // Create a message with the HashMap and the recipient's id
            //Message message = Bridgefy.createMessage(rcver_uuid, data);
            Message message = new Message.Builder().setContent(data).setReceiverId(rcver_uuid).build();

            // Send the message to the specified recipient
            Bridgefy.sendMessage(message);
        }
    }

    public void sendButtonClicked(View view) {

        String msg_content = ((EditText)findViewById(R.id.chat_box)).getText().toString();

        // Build a HashMap object
        HashMap<String, Object> msg = new HashMap<>();

        ToggleButton twitter_toggle = ((ToggleButton)(findViewById(R.id.twitter_toggle)));

        if (twitter_toggle.isChecked()) {
            msg.put("type", "twitter");
        } else {
            msg.put("type", "direct_message");
        }
        msg.put("content", msg_content);

        String rcver_uuid = hdevAdapter.getTargetedUuid();

        // Create a message with the HashMap and the recipient's id
        //Message message = Bridgefy.createMessage(rcver_uuid, msg);
        Message message = new Message.Builder().setContent(msg).setReceiverId(rcver_uuid).build();

        // Send the message to the specified recipient
        Bridgefy.sendMessage(message);

        if (!twitter_toggle.isChecked())
            hmesAdapter.addMessage("Me: " + msg_content);
        //((TextView)findViewById(R.id.mainText3)).setText("Message: " + msg);
        ((EditText)findViewById(R.id.chat_box)).getText().clear();
    }

    public void gmailButtonClicked(View view) {

        Intent intent = new Intent(this, GmailActivity.class);
        String message = hdevAdapter.getTargetedUuid();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);

    }

}
