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
    private boolean isConnected;
    private DeviceType type;

    public enum DeviceType {
        RASPBERRY_PI,
        ANDROID,
        IPHONE
    }

    public Peer(String name, String uuid, Location location) {
        this.name = name;
        this.uuid = uuid;
        this.location = location;
    }

    public String getName() { return name; }

    public String getUuid() { return uuid; }

    public Location getLocation() { return location; }

    public boolean getConnectionStatus() { return this.isConnected; }

    public DeviceType getType() { return type; }

    public void setLocation(Location location) { this.location = location; }

    public void setType(DeviceType type) { this.type = type; }

    public void setConnectionStatus(boolean isConnected) { this.isConnected = isConnected; }

    public static Peer create(String json) {
        return new Gson().fromJson(json, Peer.class);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
