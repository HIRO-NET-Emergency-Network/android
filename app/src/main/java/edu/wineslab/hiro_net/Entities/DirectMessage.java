package edu.wineslab.hiro_net.Entities;

import android.location.Location;
import android.os.Build;

import com.google.gson.Gson;

import java.util.HashMap;

public class DirectMessage {
    public final static int INCOMING_MESSAGE = 0;
    public final static int OUTGOING_MESSAGE = 1;

    private HashMap<String, Object> content = new HashMap<>();

    // Incoming or outgoing
    private int      direction;


    public DirectMessage(String creatorID, String creatorName, Location creatorLoc,
                         String senderID, String senderName, Location senderLoc, int senderType,
                         String nextHopID, String nextHopName, Location nextHopLoc, int nextHopType,
                         String destID, String destName, Location destLoc,
                         String text, int numHops) {
        // create a HashMap object to send
        content.put("text", text);
        content.put("num_hops", numHops);
        content.put("creator_ID", creatorID);
        content.put("creator_name", creatorName);
        content.put("creator_location", creatorLoc);
        content.put("sender_ID", senderID);
        content.put("sender_name", senderName);
        content.put("sender_location", senderLoc);
        content.put("sender_type", senderType);
        content.put("nextHop_ID", nextHopID);
        content.put("nextHop_name", nextHopName);
        content.put("nextHop_location", nextHopLoc);
        content.put("nextHop_type", nextHopType);
        content.put("dest_ID", destID);
        content.put("dest_name", destName);
        content.put("dest_location", destLoc);
    }

    public void toHelloMessage() {
        this.content.remove("text");
    }

    public void setCreator(String creatorID, String creatorName, Location creatorLoc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            content.replace("creator_ID", creatorID);
            content.replace("creator_name", creatorName);
            content.replace("creator_location", creatorLoc);
        } else {
            content.remove("creator_ID");
            content.remove("creator_name");
            content.remove("creator_location");
            content.put("creator_ID", creatorID);
            content.put("creator_name", creatorName);
            content.put("creator_location", creatorLoc);
        }
    }

    public void setSender(String senderID, String senderName, Location senderLoc, int senderType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            content.replace("sender_ID", senderID);
            content.replace("sender_name", senderName);
            content.replace("sender_location", senderLoc);
            content.replace("sender_type", senderType);
        } else {
            content.remove("sender_ID");
            content.remove("sender_name");
            content.remove("sender_location");
            content.remove("sender_type");
            content.put("sender_ID", senderID);
            content.put("sender_name", senderName);
            content.put("sender_location", senderLoc);
            content.put("sender_type", senderType);
        }
    }

    public void setNextHop(String nextHopID, String nextHopName, Location nextHopLoc, int nextHopType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            content.replace("nextHop_ID", nextHopID);
            content.replace("nextHop_name", nextHopName);
            content.replace("nextHop_location", nextHopLoc);
        } else {
            content.remove("nextHop_ID");
            content.remove("nextHop_name");
            content.remove("nextHop_location");
            content.remove("nextHop_type");
            content.put("nextHop_ID", nextHopID);
            content.put("nextHop_name", nextHopName);
            content.put("nextHop_location", nextHopLoc);
            content.put("nextHop_type", nextHopType);
        }
    }

    public void setDestination(String destID, String destName, Location destLoc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            content.replace("dest_ID", destID);
            content.replace("dest_name", destName);
            content.replace("dest_location", destLoc);
        } else {
            content.remove("dest_ID");
            content.remove("dest_name");
            content.remove("dest_location");
            content.put("dest_ID", destID);
            content.put("dest_name", destName);
            content.put("dest_location", destLoc);
        }
    }
    
    public int getDirection() { return direction; }

    public HashMap<String, Object> getContent() { return content; }

    public void setDirection(int direction) { this.direction = direction; }

    public void setContent(HashMap<String, Object> newContent) { this.content = newContent; }

    public static DirectMessage create(String json) { return new Gson().fromJson(json, DirectMessage.class); }

    @Override
    public String toString() { return new Gson().toJson(this); }
}
