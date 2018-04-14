package edu.wineslab.hiro_net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.sdk.client.BFEngineProfile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.Message.Builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.wineslab.hiro_net.Entities.Me;
import edu.wineslab.hiro_net.Entities.Message;
import edu.wineslab.hiro_net.Entities.Peer;

import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_MSG;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_NAME;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_UUID;
import static edu.wineslab.hiro_net.Fragments.ChatFragment.INTENT_EXTRA_LOCATION;

public class ChatActivity extends AppCompatActivity {
    private String conversationName;
    private String conversationId;
    private String conversationLocation;

    @BindView(R.id.chat_txtMessage)
    EditText txtMessage;

    MessagesRecyclerViewAdapter messagesAdapter =
            new MessagesRecyclerViewAdapter(new ArrayList<Message>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        // recover our Peer object
        conversationName = getIntent().getStringExtra(INTENT_EXTRA_NAME);
        conversationId = getIntent().getStringExtra(INTENT_EXTRA_UUID);
        conversationLocation = getIntent().getStringExtra(INTENT_EXTRA_LOCATION);

        // Enable the Up button
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(conversationName);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // register the receiver to listen for incoming messages
        LocalBroadcastManager.getInstance(getBaseContext())
                .registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Message message = new Message(intent.getStringExtra(INTENT_EXTRA_MSG));
                        message.setPeerName(intent.getStringExtra(INTENT_EXTRA_NAME));
                        message.setDirection(Message.INCOMING_MESSAGE);
                        messagesAdapter.addMessage(message);
                    }
                }, new IntentFilter(conversationId));

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
        Me me = Me.getInstance(this.getBaseContext());

        // get the message and push it to the views
        String messageString = txtMessage.getText().toString();
        if (messageString.trim().length() > 0) {
            // update the views
            txtMessage.setText("");
            Message message = new Message(messageString);
            message.setDirection(Message.OUTGOING_MESSAGE);
            messagesAdapter.addMessage(message);

            // create a HashMap object to send
            HashMap<String, Object> content = new HashMap<>();
            content.put("text", messageString);
            content.put("creator_name", me.getName());
            content.put("creator_location", me.getLocation());
            content.put("creator_ID", me.getUuid());
            content.put("peer_name", me.getName());
            content.put("peer_location", me.getLocation());
            content.put("peer_ID", me.getUuid());
            content.put("peer_type", Peer.DeviceType.RASPBERRY_PI.ordinal());
            content.put("dest_name", conversationName);
            content.put("dest_location", conversationLocation);
            content.put("dest_ID", conversationId);

            // send message text to device
            Builder builder = new Builder();
            builder.setContent(content).setReceiverId(conversationId);

            Bridgefy.sendMessage(builder.build(),
                    BFEngineProfile.BFConfigProfileLongReach);
        }
    }

    /**
     * RECYCLER VIEW CLASSES
     */
    class MessagesRecyclerViewAdapter
            extends RecyclerView.Adapter<MessagesRecyclerViewAdapter.MessageViewHolder> {

        private final List<Message> messages;

        MessagesRecyclerViewAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        void addMessage(Message message) {
            messages.add(0, message);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return messages.get(position).getDirection();
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View messageView = null;

            switch (viewType) {
                case Message.INCOMING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_incoming), viewGroup, false);
                    break;
                case Message.OUTGOING_MESSAGE:
                    messageView = LayoutInflater.from(viewGroup.getContext()).
                            inflate((R.layout.message_outgoing), viewGroup, false);
                    break;
            }

            return new MessageViewHolder(messageView);
        }

        @Override
        public void onBindViewHolder(final MessageViewHolder messageHolder, int position) {
            messageHolder.setMessage(messages.get(position));
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            final TextView txtMessage;
            Message message;

            MessageViewHolder(View view) {
                super(view);
                txtMessage = view.findViewById(R.id.textMessage);
            }

            void setMessage(Message message) {
                this.message = message;

                txtMessage.setText(message.getPeerName() + ":\n" + message.getText());
            }
        }
    }
}
