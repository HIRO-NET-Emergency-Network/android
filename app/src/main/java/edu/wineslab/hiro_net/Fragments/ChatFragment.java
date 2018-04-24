package edu.wineslab.hiro_net.Fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bridgefy.sdk.client.BFEnergyProfile;
import com.bridgefy.sdk.client.Bridgefy;
import com.bridgefy.sdk.client.BridgefyClient;
import com.bridgefy.sdk.client.BridgefyUtils;
import com.bridgefy.sdk.client.Config;
import com.bridgefy.sdk.client.Device;
import com.bridgefy.sdk.client.Message;
import com.bridgefy.sdk.client.MessageListener;
import com.bridgefy.sdk.client.RegistrationListener;
import com.bridgefy.sdk.client.Session;
import com.bridgefy.sdk.client.StateListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import edu.wineslab.hiro_net.ChatActivity;
import edu.wineslab.hiro_net.Entities.DirectMessage;
import edu.wineslab.hiro_net.Entities.Me;
import edu.wineslab.hiro_net.Entities.Peer;
import edu.wineslab.hiro_net.Entities.RoutingTable;
import edu.wineslab.hiro_net.R;

import static com.bridgefy.sdk.client.BridgefyUtils.isThingsDevice;

public class ChatFragment extends Fragment {

    private String TAG = "MainActivity";

    public static final String INTENT_EXTRA_NAME = "peerName";
    public static final String INTENT_EXTRA_UUID = "peerUuid";
    public static final String INTENT_EXTRA_MSG = "message";
    public static final String INTENT_EXTRA_LOCATION = "location";
    public static final String INTENT_EXTRA_NUM_HOPS = "numHops";

    public static final String API_KEY = "26cce93d-416b-4e72-aa5c-39a9e893a293";

    private RecyclerView peersRecyclerView;
    private RecyclerViewAdapter peersAdapter = new RecyclerViewAdapter(new ArrayList<Peer>());
    private RecyclerView.LayoutManager peersLayoutManager;

    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RecyclerView recyclerView = getActivity().findViewById(R.id.chat_peerList);
        peersLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(peersLayoutManager);
        recyclerView.setAdapter(peersAdapter);

        if (isThingsDevice(getActivity())) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
        }

        Me me = Me.getInstance(getActivity().getApplicationContext());

        Bridgefy.initialize(getActivity().getBaseContext(), API_KEY, new RegistrationListener() {
            @Override
            public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
                startBridgefy();
                Toast.makeText(getContext(), "Successfully Started Up Bridgefy", Toast.LENGTH_SHORT).show();
                me.setType(Peer.DeviceType.ANDROID);
                me.setUUID(bridgefyClient.getUserUuid());
            }

            @Override
            public void onRegistrationFailed(int errorCode, String message) {
                Toast.makeText(getContext(), getString(R.string.registration_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isFinishing()) { Bridgefy.stop(); }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Automatically handle clicks on Home/Up button, must specify
        // parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Start an instance of the Bridgefy controller
    private void startBridgefy() {
        Config.Builder builder = new Config.Builder();
        builder.setEncryption(true);
        Bridgefy.start(messageListener, stateListener, builder.build());
        Bridgefy.setEnergyProfile(BFEnergyProfile.HIGH_PERFORMANCE);
    }

    private MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            Me me = Me.getInstance(getActivity().getApplicationContext());

            String messageID = message.getUuid();
            String messageText = (String) message.getContent().get("text");
            Double numHopsDouble = (Double) message.getContent().get("num_hops");
            int numHops = numHopsDouble.intValue();
            String creatorName = (String) message.getContent().get("creator_name");
            String creatorLoc = (String) message.getContent().get("creator_location");
            String creatorID = (String) message.getContent().get("creator_ID");
            String senderName = (String) message.getContent().get("sender_name");
            String senderLoc = (String) message.getContent().get("sender_location");
            String senderID = message.getSenderId();
            Peer.DeviceType senderType = extractType(message, "sender_type");
            String nextHopName = (String) message.getContent().get("nextHop_name");
            String nextHopLoc = (String) message.getContent().get("nextHop_location");
            String nextHopID = (String) message.getContent().get("nextHop_ID");
            Peer.DeviceType nextHopType = extractType(message, "nextHop_type");
            String destName = (String) message.getContent().get("dest_name");
            String destLoc = (String) message.getContent().get("dest_location");
            String destID = (String) message.getContent().get("dest_ID");
            Peer creator = new Peer(creatorName, creatorID, creatorLoc, numHops);
            creator.setType(Peer.DeviceType.UNKNOWN);
            Peer sender = new Peer(senderName, senderID, senderLoc, 1);
            sender.setType(senderType);
            // MESSAGE "getUuid()" IS DIFFERENT UUID FROM "getSenderId()" and "getReceiverId()"

            DirectMessage directMessage = new DirectMessage(
                    creatorID, creatorName, creatorLoc,
                    senderID, senderName, senderLoc, senderType.ordinal(),
                    nextHopID, nextHopName, nextHopLoc, nextHopType.ordinal(),
                    destID, destName, destLoc,
                    messageText, numHops);

            RoutingTable routingTable = RoutingTable.getInstance(getActivity().getApplicationContext());

            // Check if we know this sender (CHANGE SO THAT IT CHECKS IF IT CONTAINS SENDER WITH SAME ID)
            if (senderName != null && !routingTable.hasDestinationID(senderID)) {
                Log.d(TAG, "Next-hop neighbor introduced itself: " + senderName);

                // Add to list of peers
                sender.setConnectionStatus(true);
                peersAdapter.addPeer(sender);

                final int[] index = {0};
                for (Peer destination : routingTable.getDestinationList()) {
                    Peer newNextHop = routingTable.getNextHopByID(destination.getUuid());
                    if (!Objects.equals(senderID, newNextHop.getUuid())) {
                        // Create hello message out of new peer's message
                        DirectMessage helloMessage =
                                new DirectMessage(senderID, senderName, senderLoc,                                                              // Creator
                                        me.getUuid(), me.getName(), me.getLocation(), me.getType().ordinal(),                                   // Sender
                                        newNextHop.getUuid(), newNextHop.getName(), newNextHop.getLocation(), newNextHop.getType().ordinal(),   // Next-hop
                                        destination.getUuid(), destination.getName(), destination.getLocation(),                                // Destination
                                        "", numHops + 1);
                        helloMessage.toHelloMessage();

                        Message.Builder builder = new Message.Builder();
                        builder.setContent(helloMessage.getContent())
                                .setReceiverId(routingTable.getNextHopByID(destID).getUuid());
                        Message newMessage = builder.build();
                        newMessage.setUuid(messageID);
                        Bridgefy.sendMessage(newMessage);

                        // Save data for experiments
                        ArrayList<String> data = new ArrayList<String>() {{
                            add(String.valueOf(System.currentTimeMillis()));
                            add(creatorID);
                            add(destination.getUuid());
                            add(me.getUuid());
                            add(String.valueOf(numHops + 1));
                            add(messageID);
                        }};
                        me.save_data(getActivity().getApplicationContext(),
                                me.getDataHeader(), me.getFileNameForward(), data);
                    }
                }

                // Add to routing table
                routingTable.addRoute(sender, sender, 1);

                // Save data for experiments
                ArrayList<String> data = new ArrayList<String>() {{
                    add(String.valueOf(System.currentTimeMillis()));            // Current time
                    add(senderID);                                              // Next Hop ID
                    add("1");                                                   // # of hops
                    add(senderID);                                              // Destination ID
                }};
                me.save_data(getActivity().getApplicationContext(),
                        me.getDataHeader(), me.getFileNameRoutingTable(), data);
            }
            // Check if we know this creator
            if (creatorName != null && !routingTable.hasDestinationID(creatorID)) {
                Log.d(TAG, "Multi-hop neighbor introduced itself: " + creator.getName());

                // Add to list of peers
                creator.setConnectionStatus(true);
                peersAdapter.addPeer(creator);

                final int[] index = {0};
                for (Peer destination : routingTable.getDestinationList()) {
                    Peer newNextHop = routingTable.getNextHopByID(destination.getUuid());
                    if (!Objects.equals(senderID, newNextHop.getUuid())) {
                        DirectMessage helloMessage =
                                new DirectMessage(creatorID, creatorName, creatorLoc,                                                           // Creator
                                        me.getUuid(), me.getName(), me.getLocation(), me.getType().ordinal(),                                   // Sender
                                        newNextHop.getUuid(), newNextHop.getName(), newNextHop.getLocation(), newNextHop.getType().ordinal(),   // Next-hop
                                        destination.getUuid(), destination.getName(), destination.getLocation(),                                // Destination
                                        "", (numHops + 1));

                        Message.Builder builder = new Message.Builder();
                        builder.setContent(helloMessage.getContent())
                                .setReceiverId(routingTable.getNextHopByID(destID).getUuid());
                        Message newMessage = builder.build();
                        newMessage.setUuid(messageID);
                        Bridgefy.sendMessage(newMessage);

                        // Save data for experiments
                        ArrayList<String> data = new ArrayList<String>() {{
                            add(String.valueOf(System.currentTimeMillis()));
                            add(creatorID);
                            add(destination.getUuid());
                            add(me.getUuid());
                            add(String.valueOf(numHops + 1));
                            add(messageID);
                        }};
                        me.save_data(getActivity().getApplicationContext(),
                                me.getDataHeader(), me.getFileNameForward(), data);
                    }
//                    Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        public void run() {
//                            index[0]++;
//                        }
//                    }, 1000);
                }

                // Add to routing table
                routingTable.addRoute(sender, creator, numHops);

                // Save data for experiments
                ArrayList<String> data = new ArrayList<String>() {{
                    add(String.valueOf(System.currentTimeMillis()));            // Current time
                    add(senderID);                                             // Next Hop ID
                    add(String.valueOf(numHops));                               // # of hops
                    add(creatorID);                                                // Destination ID
                }};
                me.save_data(getActivity().getApplicationContext(),
                        me.getDataHeader(), me.getFileNameRoutingTable(), data);
            }

            // Update device type of sender, if known
            if (routingTable.hasDestinationID(senderID)) {
                for (Peer destination : routingTable.getDestinationList()) {
                    if (destination.getType() == Peer.DeviceType.UNKNOWN
                            && senderType != Peer.DeviceType.UNKNOWN) {
                        peersAdapter.replacePeerByID(senderID, sender);
                    }
                }
            }

            // Forward message to next-hop neighbor
            if (!Objects.equals(destName, me.getName()) && !Objects.equals(destName, null)) {
                Peer newNextHop = routingTable.getNextHopByID(destID);
                if (routingTable.hasDestinationID(destID)) {
                    directMessage.setSender(me.getUuid(), me.getName(),
                            me.getLocation(), me.getType().ordinal());
                    directMessage.setNextHop(newNextHop.getUuid(), newNextHop.getName(),
                            newNextHop.getLocation(), newNextHop.getType().ordinal());
                    directMessage.setNumHops(numHops + 1);
                    message.setContent(directMessage.getContent());
                    message.setReceiverId(newNextHop.getUuid());
                    message.setUuid(messageID);
                    Bridgefy.sendMessage(message);

                    // Save data for experiments
                    ArrayList<String> data = new ArrayList<String>() {{
                        add(String.valueOf(System.currentTimeMillis()));
                        add(creatorID);
                        add(destID);
                        add(me.getUuid());
                        add(String.valueOf(numHops + 1));
                        add(messageID);
                    }};
                    me.save_data(getActivity().getApplicationContext(),
                            me.getDataHeader(), me.getFileNameForward(), data);
                }
            }
            // DirectMessage is meant for us
            else if (Objects.equals(destName, me.getName())) {
                Log.d(TAG, "Incoming private message: " + messageText);
                LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                        sendBroadcast(new Intent(message.getSenderId()).
                                putExtra(INTENT_EXTRA_MSG, messageText));
            }
            if (isThingsDevice(getActivity().getApplicationContext())) {
                Peer newNextHop = routingTable.getNextHopByID(destID);
                if (!Objects.equals(destName, me.getName()) && !Objects.equals(destName, null)) {
                    if (routingTable.hasDestinationID(creatorID)) {
                        DirectMessage replyMessage =
                                new DirectMessage(me.getUuid(), me.getName(), me.getLocation(),                                                 // Creator
                                        me.getUuid(), me.getName(), me.getLocation(), Peer.DeviceType.RASPBERRY_PI.ordinal(),                   // Sender
                                        newNextHop.getUuid(), newNextHop.getName(), newNextHop.getLocation(), newNextHop.getType().ordinal(),   // Next-hop
                                        creatorID, creatorName, creatorLoc,                                                                     // Destination
                                        "I'm a robot.", 1);

                        Message.Builder builder = new Message.Builder();
                        builder.setContent(replyMessage.getContent())
                                .setReceiverId(routingTable.getNextHopByID(destID).getUuid());
                        Bridgefy.sendMessage(builder.build());

                        // Save data for experiments
                        ArrayList<String> data = new ArrayList<String>() {{
                            add(String.valueOf(System.currentTimeMillis()));            // Current time
                            add(me.getUuid());                                          // Creator ID
                            add(creatorID);                                             // Destination ID
                            add(me.getUuid());                                          // Sender ID
                            add(String.valueOf(routingTable.getNumHopsByID(destID)));   // # of hops
                            add(messageID);                                             // Message ID
                        }};
                        me.save_data(getActivity().getApplicationContext(),
                                me.getDataHeader(), me.getFileNameSent(), data);
                    }
                }
            }
        }
    };

    private Peer.DeviceType extractType(Message message, String type_key) {
        int ordinal;
        Object obj = message.getContent().get(type_key);
        if (obj instanceof Double) {
            ordinal = ((Double) obj).intValue();
        } else {
            ordinal = (Integer) obj;
        }
        return Peer.DeviceType.values()[ordinal];
    }
    
    private StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(final Device device, Session session) {
            Toast.makeText(getContext(), "Connected to: " + device.getUserId(), Toast.LENGTH_SHORT).show();

            Me me = Me.getInstance(getActivity().getApplicationContext());

            DirectMessage helloMessage = new DirectMessage(
                    me.getUuid(), me.getName(), me.getLocation(),                                                   // Creator
                    me.getUuid(), me.getName(), me.getLocation(), me.getType().ordinal(),                           // Sender
                    device.getUserId(), device.getDeviceName(), null, Peer.DeviceType.UNKNOWN.ordinal(),  // Next-hop
                    device.getUserId(), device.getDeviceName(), null,                                        // Destination
                    "", 1);
            helloMessage.toHelloMessage();

            Log.d(TAG, String.format("Sender's name (in HELLO message): %s", helloMessage.getContent().get("sender_name")));
            Log.d(TAG, String.format("Creator's name (in HELLO message): %s", helloMessage.getContent().get("creator_name")));

            // Send a hello message to the new device
            device.sendMessage(helloMessage.getContent());

            // This device will also send a hello message
            // We receive their hello message in the onMessageReceived() function
            // They receive our hello message in their onMessageReceived() function
            // ... where we both forward the hello message to all nodes in our routing tables
        }

        @Override
        public void onDeviceLost(Device peer) {
            Log.w(TAG, "onDeviceLost: " + peer.getUserId());
            // Update list of peers
            peersAdapter.removePeer(peer);

            // Update routing table
            RoutingTable routingTable = RoutingTable
                    .getInstance(getActivity().getApplicationContext());
            routingTable.removeRouteByID(peer.getUserId());
        }

        @Override
        public void onStartError(String message, int errorCode) {
            Log.e(TAG, "onStartError: " + message);
            if (errorCode == StateListener.INSUFFICIENT_PERMISSIONS) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            }
        }
    };

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.PeerViewHolder> {

        private final List<Peer> peers;

        RecyclerViewAdapter(List<Peer> peers) { this.peers = peers; }

        @Override
        public int getItemCount() { return peers.size(); }

        void addPeer(Peer peer) {
            int position = getPeerPosition(peer.getUuid());
            if (position > -1) {
                peers.set(position, peer);
                notifyItemChanged(position);
            } else {
                peers.add(peer);
                notifyItemInserted(peers.size() - 1);
            }
        }

        void replacePeerByID(String peerID, Peer newPeer) {
            for (Peer peer: peers) {
                if (Objects.equals(peer.getUuid(), peerID)) {
                    int position = getPeerPosition(peer.getUuid());
                    if (position > -1) {
                        peers.set(position, newPeer);
                        notifyItemChanged(position);
                    } else {
                        peers.add(newPeer);
                        notifyItemInserted(peers.size() - 1);
                    }
                }
            }
        }

        void removePeer(Device lostPeer) {
            int position = getPeerPosition(lostPeer.getUserId());
            if (position > -1) {
                Peer peer = peers.get(position);
                peer.setConnectionStatus(false);
                peers.set(position, peer);
                notifyItemChanged(position);
            }
        }

        private int getPeerPosition(String peerId) {
            for (int i = 0; i < peers.size(); i++) {
                if (peers.get(i).getUuid().equals(peerId))
                    return i;
            }
            return -1;
        }

        @Override
        public PeerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_bar, parent, false);
            return new PeerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PeerViewHolder peerHolder, int position) {
            peerHolder.setPeer(peers.get(position));
        }

        class PeerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            final TextView mContentView;
            Peer peer;

            PeerViewHolder(View view) {
                super(view);
                mContentView = view.findViewById(R.id.user_name);
                view.setOnClickListener(this);
            }

            void setPeer(Peer peer) {
                this.peer = peer;

                switch (peer.getType()) {
                    case RASPBERRY_PI:
                        this.mContentView.setText(String.format("%s (Pi)", peer.getName()));
                    case ANDROID:
                        this.mContentView.setText(String.format("%s (Android)", peer.getName()));
                        break;
                    case IPHONE:
                        this.mContentView.setText(String.format("%s (iPhone)", peer.getName()));
                        break;
                    default:
                        this.mContentView.setText(String.format("%s (Unknown)", peer.getName()));
                        break;
                }

                if (peer.getConnectionStatus()) {
                    this.mContentView.setTextColor(Color.BLACK);
                } else {
                    this.mContentView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                startActivity(new Intent(getActivity().getApplicationContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, peer.getName())
                        .putExtra(INTENT_EXTRA_UUID, peer.getUuid())
                        .putExtra(INTENT_EXTRA_LOCATION, peer.getLocation())
                        .putExtra(INTENT_EXTRA_NUM_HOPS, peer.getNumHopsFromMe()));
            }
        }
    }
}
