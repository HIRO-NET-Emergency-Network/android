package edu.wineslab.hiro_net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.bridgefy.sdk.client.BFEngineProfile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.Message.Builder;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.wineslab.hiro_net.Entities.DirectMessage;
import edu.wineslab.hiro_net.Entities.Me;
import edu.wineslab.hiro_net.Entities.Peer;
import edu.wineslab.hiro_net.Entities.RoutingTable;

import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_MSG;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_NAME;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_UUID;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_LOCATION;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_NUM_HOPS;

public class ChatActivity extends AppCompatActivity {
    private String TAG = "ChatActivity";

    private String destinationName;
    private String destinationID;
    private String destinationLocation;
    private int destinationNumHopsFrom;

    @BindView(R.id.chat_txtMessage)
    EditText txtMessage;

    MessagesRecyclerViewAdapter messagesAdapter =
            new MessagesRecyclerViewAdapter(new ArrayList<DirectMessage>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        // Recover information on destination peer
        Gson gson = new Gson();
        destinationName = getIntent().getStringExtra(INTENT_EXTRA_NAME);
        destinationID = getIntent().getStringExtra(INTENT_EXTRA_UUID);
        destinationLocation = getIntent().getStringExtra(INTENT_EXTRA_LOCATION);
        destinationNumHopsFrom = getIntent().getIntExtra(INTENT_EXTRA_NUM_HOPS, 1);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(destinationName);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Register receiver to listen for incoming directMessages
        LocalBroadcastManager.getInstance(getBaseContext())
                .registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        DirectMessage directMessage = new DirectMessage(
                                destinationID,
                                destinationName, destinationLocation,
                                null, null, null, 0,
                                null, null, null, 0,
                                null, null, null,
                                intent.getStringExtra(INTENT_EXTRA_MSG), destinationNumHopsFrom);
                        directMessage.setDirection(DirectMessage.INCOMING_MESSAGE);
                        messagesAdapter.addMessage(directMessage);
                    }
                }, new IntentFilter(destinationID));

        // configure the RecyclerView
        RecyclerView messagesRecyclerView = findViewById(R.id.message_list);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setReverseLayout(true);
        messagesRecyclerView.setLayoutManager(mLinearLayoutManager);
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @OnClick({R.id.chat_sendButton})
    public void onMessageSend(View v) {
        Me me = Me.getInstance(this.getApplicationContext());
        RoutingTable routingTable = RoutingTable.getInstance(this.getBaseContext());

        // get the directMessage and push it to the views
        String messageString = txtMessage.getText().toString();
        if (messageString.trim().length() > 0) {
            // Erase text box for user
            txtMessage.setText("");

            // Find next-hop neighbor
            Peer newNextHop = routingTable.getNextHopByID(destinationID);

            // Compose message
            DirectMessage directMessage = new DirectMessage(me.getUuid(), me.getName(), me.getLocation(),
                    me.getUuid(), me.getName(), me.getLocation(), me.getType().ordinal(),
                    newNextHop.getUuid(), newNextHop.getName(), newNextHop.getLocation(), newNextHop.getType().ordinal(),
                    destinationID, destinationName, destinationLocation, messageString, 1);
            // Display as outgoing message
            directMessage.setDirection(DirectMessage.OUTGOING_MESSAGE);
            // Display message for user
            messagesAdapter.addMessage(directMessage);

            Builder builder = new Builder();
            // Send message to the next-hop neighbor
            builder.setContent(directMessage.getContent()).setReceiverId(newNextHop.getUuid());
            Message message = builder.build();
            Bridgefy.sendMessage(message, BFEngineProfile.BFConfigProfileLongReach);

            // Save data for experiments
            ArrayList<String> data = new ArrayList<String>() {{
                add(String.valueOf(System.currentTimeMillis()));
                add(me.getUuid());
                add(destinationID);
                add(me.getUuid());
                add(String.valueOf(routingTable.getNumHopsByID(destinationID)));
                add(message.getUuid());
            }};
            me.save_data(getApplicationContext(), me.getDataHeader(), me.getFileNameSent(),
                    data);
        }
    }

    /**
     * RECYCLER VIEW CLASSES
     */
    class MessagesRecyclerViewAdapter
            extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.MessageViewHolder> {

        private final List<DirectMessage> directMessages;

        MessagesRecyclerViewAdapter(List<DirectMessage> directMessages) {
            this.directMessages = directMessages;
        }

        @Override
        public int getItemCount() {
            return directMessages.size();
        }

        void addMessage(DirectMessage directMessage) {
            directMessages.add(0, directMessage);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return directMessages.get(position).getDirection();
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View messageView = null;

            switch (viewType) {
                case DirectMessage.INCOMING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_incoming), viewGroup, false);
                    break;
                case DirectMessage.OUTGOING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_outgoing), viewGroup, false);
                    break;
            }

            return new MessageViewHolder(messageView);
        }

        @Override
        public void onBindViewHolder(final MessageViewHolder messageHolder, int position) {
            messageHolder.setDirectMessage(directMessages.get(position));
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView txtMessage;
            DirectMessage directMessage;

            MessageViewHolder(View view) {
                super(view);
                txtMessage = view.findViewById(R.id.textMessage);
            }

            void setDirectMessage(DirectMessage directMessage) {
                this.directMessage = directMessage;

                String creatorName = (String) directMessage.getContent().get("creator_name");
                String messageText = (String) directMessage.getContent().get("text");
                txtMessage.setText(String.format("%s:\n%s", creatorName, messageText));
            }
        }
    }
}
