package edu.wineslab.hiro_net.Entities;

import android.content.Context;
import android.location.Location;
import android.os.Build;

import com.bridgefy.sdk.client.Bridgefy;

/**
 * Created by Vladislav on 4/13/2018.
 */

public class Me {
    private static Me instance;
    private String  name;
    private String  uuid;
    private Location location;
    private Peer.DeviceType type;
    private static Context context;

    private Me(Context context) {

        Me.context = context;
        this.name = Build.MANUFACTURER + " " + Build.MODEL;
    }

    public static synchronized Me getInstance(Context context) {
        if (instance == null) {
            instance = new Me(context);
        }
        return instance;
    }

    public String getName() { return name; }

    public String getUuid() { return uuid; }

    public Location getLocation() { return location; }

    public Peer.DeviceType getType() { return type; }

    public void setUUID(String uuid) { this.uuid = uuid; }

    public void setLocation(Location location) { this.location = location; }

    public void setType(Peer.DeviceType type) { this.type = type; }
}
