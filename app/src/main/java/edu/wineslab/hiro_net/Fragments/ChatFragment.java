package edu.wineslab.hiro_net.Fragments;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import edu.wineslab.hiro_net.ChatActivity;
import edu.wineslab.hiro_net.Entities.Peer;
import edu.wineslab.hiro_net.R;

public class ChatFragment extends Fragment {

    private String TAG = "MainActivity";

    public static final String INTENT_EXTRA_NAME = "peerName";
    public static final String INTENT_EXTRA_UUID = "peerUuid";
    public static final String INTENT_EXTRA_TYPE = "deviceType";
    public static final String INTENT_EXTRA_MSG  = "message";
    public static final String BROADCAST_CHAT    = "Broadcast";

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Toast.makeText(getContext(), "Created Recycler View",
                Toast.LENGTH_SHORT).show();

        RecyclerView recyclerView = getActivity().findViewById(R.id.chat_peerList);
        peersLayoutManager = new LinearLayoutManager(getActivity().getBaseContext());
        recyclerView.setLayoutManager(peersLayoutManager);
        recyclerView.setAdapter(peersAdapter);

        if (isThingsDevice(getActivity())) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.enable();
        }

        Bridgefy.initialize(getActivity().getApplicationContext(), API_KEY, new RegistrationListener() {
            @Override
            public void onRegistrationSuccessful(BridgefyClient bridgefyClient) {
                startBridgefy();
                Toast.makeText(getContext(), "Successful Started Up Bridgefy",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRegistrationFailed(int errorCode, String message) {
                Toast.makeText(getContext(), getString(R.string.registration_error),
                        Toast.LENGTH_LONG).show();
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
            case R.id.action_broadcast:
                startActivity(new Intent(getActivity().getBaseContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, BROADCAST_CHAT)
                        .putExtra(INTENT_EXTRA_UUID, BROADCAST_CHAT));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Start an instance of the Bridgefy controller
    private void startBridgefy() {
        Config.Builder builder = new Config.Builder();
        builder.setEnergyProfile(BFEnergyProfile.HIGH_PERFORMANCE);
        builder.setEncryption(false);
        Bridgefy.start(messageListener, stateListener, builder.build());
    }

    // Check if the current device is running Android Things
    public boolean isThingsDevice(Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature("android.hardware.type.embedded");
    }

    private MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessageReceived(Message message) {
            // direct messages carrying a Device name represent device handshakes
            if (message.getContent().get("device_name") != null) {
                Peer peer = new Peer(message.getSenderId(),
                        (String) message.getContent().get("device_name"));
                peer.setNearby(true);
                peer.setDeviceType(extractType(message));
                peersAdapter.addPeer(peer);

                // any other direct message should be treated as such
            } else {
                String incomingMessage = (String) message.getContent().get("text");
                LocalBroadcastManager.getInstance(getActivity().getBaseContext()).sendBroadcast(
                        new Intent(message.getSenderId())
                                .putExtra(INTENT_EXTRA_MSG, incomingMessage));
            }

            if (isThingsDevice(getActivity())) {
                //if it's an Android Things device, reply automatically
                HashMap<String, Object> content = new HashMap<>();
                content.put("text", "Beep boop. I'm a bot.");

                Message.Builder builder=new Message.Builder();
                builder.setContent(content).setReceiverId(message.getSenderId());
                Bridgefy.sendMessage(builder.build());

            }
        }

        @Override
        public void onBroadcastMessageReceived(Message message) {
            // we should not expect to have connected previously to the device that originated
            // the incoming broadcast message, so device information is included in this packet
            String incomingMsg = (String) message.getContent().get("text");
            String deviceName  = (String) message.getContent().get("device_name");
            Peer.DeviceType deviceType = extractType(message);

            LocalBroadcastManager.getInstance(getActivity().getBaseContext()).sendBroadcast(
                    new Intent(BROADCAST_CHAT)
                            .putExtra(INTENT_EXTRA_NAME, deviceName)
                            .putExtra(INTENT_EXTRA_TYPE, deviceType)
                            .putExtra(INTENT_EXTRA_MSG,  incomingMsg));
        }
    };

    private Peer.DeviceType extractType(Message message) {
        int eventOrdinal;
        Object eventObj = message.getContent().get("device_type");
        if (eventObj instanceof Double) {
            eventOrdinal = ((Double) eventObj).intValue();
        } else {
            eventOrdinal = (Integer) eventObj;
        }
        return Peer.DeviceType.values()[eventOrdinal];
    }

    private StateListener stateListener = new StateListener() {
        @Override
        public void onDeviceConnected(final Device device, Session session) {
            // send our information to the Device

            Toast.makeText(getContext(), "Connected to: " + device.getUserId(),
                    Toast.LENGTH_SHORT).show();

            HashMap<String, Object> map = new HashMap<>();
            map.put("device_name", Build.MANUFACTURER + " " + Build.MODEL);
            map.put("device_type", Peer.DeviceType.ANDROID.ordinal());
            device.sendMessage(map);
        }

        @Override
        public void onDeviceLost(Device peer) {
            Log.w(TAG, "onDeviceLost: " + peer.getUserId());
            peersAdapter.removePeer(peer);
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
                peer.setNearby(false);
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

                switch (peer.getDeviceType()) {
                    case ANDROID:
                        this.mContentView.setText(peer.getDeviceName() + " (android)");
                        break;
                    case IPHONE:
                        this.mContentView.setText(peer.getDeviceName() + " (iPhone)");
                        break;
                }

                if (peer.isNearby()) {
                    this.mContentView.setTextColor(Color.BLACK);
                } else {
                    this.mContentView.setTextColor(Color.GRAY);
                }
            }

            public void onClick(View v) {
                startActivity(new Intent(getActivity().getBaseContext(), ChatActivity.class)
                        .putExtra(INTENT_EXTRA_NAME, peer.getDeviceName())
                        .putExtra(INTENT_EXTRA_UUID, peer.getUuid()));
            }
        }
    }

}
