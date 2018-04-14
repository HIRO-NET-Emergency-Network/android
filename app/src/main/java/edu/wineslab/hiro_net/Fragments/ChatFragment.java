package edu.wineslab.hiro_net.Fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import edu.wineslab.hiro_net.ChatActivity;
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
        peersLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        recyclerView.setLayoutManager(peersLayoutManager);
        recyclerView.setAdapter(peersAdapter);

        if (isThingsDevice(getActivity())) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
        }

        Me me = Me.getInstance(getActivity().getBaseContext());

        Bridgefy.initialize(getActivity().getApplicationContext(), API_KEY, new RegistrationListener() {
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

//    // Check if the current device is running Android Things
//    public boolean isThingsDevice(Context context) {
//        final PackageManager pm = context.getPackageManager();
//        return pm.hasSystemFeature("android.hardware.type.embedded");
//    }

    private MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            Me me = Me.getInstance(getActivity().getBaseContext());

            String messageID = message.getUuid();
            String creatorName = (String) message.getContent().get("creator_name");
            Location creatorLoc = (Location) message.getContent().get("creator_location");
            String creatorID = (String) message.getContent().get("creator_ID");
            String peerName = (String) message.getContent().get("peer_name");
            Location peerLoc = (Location) message.getContent().get("peer_location");
            String peerID = message.getSenderId();
            Peer.DeviceType peerType = extractType(message);
            String destName = (String) message.getContent().get("dest_name");
            Location destLoc = (Location) message.getContent().get("dest_location");
            String destID = (String) message.getContent().get("dest_ID");
            Peer creator = new Peer(creatorName, creatorID, creatorLoc);
            Peer peer = new Peer(peerName, peerID, peerLoc);
            Peer dest = new Peer(destName, destID, destLoc);

            // MESSAGE "getUuid()" IS DIFFERENT UUID FROM "getSenderId()" and "getReceiverId"

            RoutingTable routingTable = RoutingTable.getInstance(getActivity().getBaseContext());
            ArrayList<HashMap<String, Peer>> table = routingTable.getTable();

            // Ensure we don't already know this peer
            if (peerName != null && !peersAdapter.peers.contains(peer)) {
                Log.d(TAG, "Peer introduced itself: " + peer.getName());

                // Add to list of peers
                peer.setConnectionStatus(true);
                peer.setType(peerType);
                peersAdapter.addPeer(peer);

                // Add to routing table
                routingTable.addTableEntry(peer, peer);
            }
            // Ensure we don't already know this sender
            if (creatorName != null && !peersAdapter.peers.contains(creator)) {
                Log.d(TAG, "Multi-hop neighbor introduced itself: " + creator.getName());

                // Add to list of peers
                creator.setConnectionStatus(true);
                creator.setType(extractType(message));
                peersAdapter.addPeer(creator);

                // Add to routing table
                routingTable.addTableEntry(peer, creator);
            }

            // Forward message to next hop neighbor
            if (!Objects.equals(destName, me.getName()) && !Objects.equals(destName, null)) {
                if(!(routingTable.findEntryByID(destID) == null)) {
                    HashMap<String, Object> forwardMessage = message.getContent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        forwardMessage.replace("peer_name", me.getName());
                        forwardMessage.replace("peer_location", me.getLocation());
                        forwardMessage.replace("peer_type", me.getType().ordinal());
                    }
                    else {
                        forwardMessage.remove("peer_name");
                        forwardMessage.remove("peer_location");
                        forwardMessage.remove("peer_type");
                        forwardMessage.put("peer_name", me.getName());
                        forwardMessage.put("peer_location", me.getLocation());
                        forwardMessage.put("peer_type", me.getType().ordinal());
                    }
                    message.setContent(forwardMessage);
                    message.setReceiverId(routingTable.findEntryByID(destID).get("nextHop").getUuid());
                    message.setUuid(messageID);
                    Bridgefy.sendMessage(message);
                }
            }
            // Message is meant for us
            else if (Objects.equals(destName, me.getName())) {
                String incomingMessage = (String) message.getContent().get("text");
                Log.d(TAG, "Incoming private message: " + incomingMessage);
                LocalBroadcastManager.getInstance(getActivity().getBaseContext()).sendBroadcast(
                        new Intent(message.getSenderId())
                                .putExtra(INTENT_EXTRA_MSG, incomingMessage));
            }

            if (isThingsDevice(getActivity().getBaseContext())) {
                //if it's an Android Things device, reply automatically
                HashMap<String, Object> content = new HashMap<>();

                if (!Objects.equals(destName, me.getName()) && !Objects.equals(destName, null)) {
                    if(!(routingTable.findEntryByID((String) message.getContent().get("creator_ID")) == null)) {
                        content.put("text", "Beep boop. I'm a bot.");
                        content.put("creator_name", me.getName());
                        content.put("creator_location", me.getLocation());
                        content.put("creator_ID", me.getUuid());
                        content.put("peer_name", me.getName());
                        content.put("peer_location", me.getLocation());
                        content.put("peer_ID", me.getUuid());
                        content.put("peer_type", Peer.DeviceType.RASPBERRY_PI.ordinal());
                        content.put("dest_name", message.getContent().get("creator_name"));
                        content.put("dest_location", message.getContent().get("creator_location"));
                        content.put("dest_ID", message.getContent().get("creator_ID"));

                        Message.Builder builder=new Message.Builder();
                        builder.setContent(content).setReceiverId(routingTable.findEntryByID(destID).get("nextHop").getUuid());
                        Bridgefy.sendMessage(builder.build());
                    }
                }
            }
        }
    };

    private Peer.DeviceType extractType(Message message) {
        int ordinal;
        Object obj = message.getContent().get("peer_type");
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
            Toast.makeText(getContext(), "Connected to: " + device.getUserId(),
                    Toast.LENGTH_SHORT).show();

            Me me = Me.getInstance(getActivity().getBaseContext());

            HashMap<String, Object> content = new HashMap<>();
            content.put("creator_name", Build.MANUFACTURER + " " + Build.MODEL);
            content.put("creator_location", me.getLocation());
            content.put("creator_ID", me.getUuid());
            content.put("peer_name", me.getName());
            content.put("peer_location", me.getLocation());
            content.put("peer_ID", me.getUuid());
            content.put("peer_type", me.getType().ordinal());
            content.put("dest_name", device.getDeviceName());
            // How can I access the device's location? HELLO messages may not have location information.
            content.put("dest_location", null);
            content.put("dest_ID", device.getUserId());

            device.sendMessage(content);
        }

        @Override
        public void onDeviceLost(Device peer) {
            Log.w(TAG, "onDeviceLost: " + peer.getUserId());
            // Update list of peers
            peersAdapter.removePeer(peer);

            // Update routing table
            RoutingTable routingTable = RoutingTable.getInstance(getActivity().getApplicationContext());
            routingTable.removeTableEntryByID(peer.getUserId());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBridgefy();
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getActivity(), "Location permissions needed for peer-discovery.",
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

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
                        this.mContentView.setText(String.format("%s (android)", peer.getName()));
                        break;
                    case IPHONE:
                        this.mContentView.setText(String.format("%s (iPhone)", peer.getName()));
                        break;
                }

                if (peer.getConnectionStatus()) {
                    this.mContentView.setTextColor(Color.BLACK);
                } else {
                    this.mContentView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                startActivity(new Intent(getActivity().getBaseContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, peer.getName())
                        .putExtra(INTENT_EXTRA_UUID, peer.getUuid())
                        .putExtra(INTENT_EXTRA_LOCATION, peer.getLocation().toString()));
            }
        }
    }

}
