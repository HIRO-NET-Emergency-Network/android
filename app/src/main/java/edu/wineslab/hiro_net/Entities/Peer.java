package edu.wineslab.hiro_net.Entities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import com.google.gson.Gson;

public class Peer {
    private String  name;
    private String  uuid;
    private Location location;
    private int numHopsFromMe;
    private boolean isConnected;
    private DeviceType type;

    public enum DeviceType {
        RASPBERRY_PI,
        ANDROID,
        IPHONE,
        UNKNOWN
    }

    public Peer(String name, String uuid, Location location, int numHops) {
        this.name = name;
        this.uuid = uuid;
        this.location = location;
        this.numHopsFromMe = numHops;
    }

    public String getName() { return this.name; }

    public String getUuid() { return this.uuid; }

    public Location getLocation() { return this.location; }

    public int getNumHopsFromMe() { return this.numHopsFromMe; }

    public boolean getConnectionStatus() { return this.isConnected; }

    public DeviceType getType() { return type; }

    public void setLocation(Location location) { this.location = location; }

    public void setType(DeviceType type) { this.type = type; }

    public void setNumHopsFromMe(int numHopsFromMe) { this.numHopsFromMe = numHopsFromMe; }

    public void setConnectionStatus(boolean isConnected) { this.isConnected = isConnected; }

    public static Peer create(String json) {
        return new Gson().fromJson(json, Peer.class);
    }

    public String locationToString(Location location) {
        return new Gson().toJson(location);
    }

    public Location locationFromString(String jsonString) {
        return new Gson().fromJson(jsonString, Location.class);
    }
}
