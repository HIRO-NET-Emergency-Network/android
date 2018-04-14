package edu.wineslab.hiro_net.Entities;

import android.location.Location;

import com.google.gson.Gson;

public class Message {
    public final static int INCOMING_MESSAGE = 0;
    public final static int OUTGOING_MESSAGE = 1;

    // Message origin
    private String   creatorID;
    private String   creatorName;
    private Location creatorLoc;
    // Last-hop of message
    private String   peerID;
    private String   peerName;
    private Location peerLoc;
    // Message destination
    private String   destID;
    private String   destName;
    private Location destLoc;
    // Contents
    private String   text;

    // ROUTING TABLE CONTAINS KEY OF DESTINATION ID WHICH IS PAIRED TO A VALUE OF THE NEXT HOP ID WHERE THE MESSAGE MUST BE SENT TO

    // Incoming or outgoing
    private int      direction;


    public Message(String text) { this.text = text; }

    public int getDirection() { return direction; }

    public String getText() { return text; }

    public void setDirection(int direction) { this.direction = direction; }

    // Get Node Information

    public String getPeerID() { return peerID; }

    public String getPeerName() { return peerName; }

    public Location getPeerLocation() { return peerLoc; }

    public String getDestID() { return destID; }

    public String getDestName() { return destName; }

    public Location getDestLocation() { return destLoc; }

    public String getCreatorID() { return creatorID; }

    public String getCreatorName() { return creatorName; }

    public Location getCreatorLocation() { return creatorLoc; }

    // Set Node Information

    public void setPeerID(String peerID) { this.peerID = peerID; }

    public void setPeerName(String peerName) { this.peerName = peerName; }

    public void setPeerLoc(Location peerLoc) { this.peerLoc = peerLoc; }

    public void setDestID(String destID) { this.destID = destID; }

    public void setDestName(String destName) { this.destName = destName; }

    public void setDestLoc(Location destLoc) { this.destLoc = destLoc; }

    public void setCreatorID(String creatorID) { this.creatorID = creatorID; }

    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public void setCreatorLoc(Location creatorLoc) { this.creatorLoc = creatorLoc; }

    public static Message create(String json) { return new Gson().fromJson(json, Message.class); }

    @Override
    public String toString() { return new Gson().toJson(this); }
}
