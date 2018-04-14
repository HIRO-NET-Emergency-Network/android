package edu.wineslab.hiro_net.Entities;

import android.content.Context;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by Vladislav on 4/13/2018.
 */

public class RoutingTable {
    private static RoutingTable instance;
    private ArrayList<HashMap<String, Peer>> table = new ArrayList<HashMap<String, Peer>>(1000);
    private static Context context;

    private RoutingTable(Context context) {
        RoutingTable.context = context;
        HashMap<String,Peer> entry = new HashMap<>(5);
        entry.put("nextHop", null);
        entry.put("destination", null);
        this.table.add(entry);
    }

    public static synchronized RoutingTable getInstance(Context context) {
        if (instance == null) {
            instance = new RoutingTable(context);
        }
        return instance;
    }

    public ArrayList<HashMap<String, Peer>> getTable() { return this.table; }

    public void addTableEntry(Peer nextHop, Peer destination) {
        HashMap<String,Peer> map = new HashMap<>(5);
        map.put("nextHop", nextHop);
        map.put("destination", destination);
        this.table.add(map);

        for (HashMap<String,Peer> hashMap : table)
        {
            for (Map.Entry<String,Peer> entry: hashMap.entrySet()) {
                if (!Objects.equals(entry.getKey(), "nextHop") || !Objects.equals(entry.getKey(), "destination")) {
                    throw new IllegalArgumentException("Invalid entry: " + entry.getKey());
                }
            }
        }
    }

    public HashMap<String, Peer> findEntryByID(String ID) {
        for (HashMap<String,Peer> hashMap : table)
        {
            for (Map.Entry<String,Peer> entry: hashMap.entrySet()) {
                if (Objects.equals(entry.getValue().getUuid(), ID)) {
                    return hashMap;
                }
            }
        }
        return null;
    }

    public void removeTableEntryByID(String ID) {
        for (HashMap<String,Peer> hashMap : table)
        {
            for (Map.Entry<String,Peer> entry: hashMap.entrySet()) {
                if (Objects.equals(entry.getValue().getUuid(), ID)) {
                    this.table.remove(hashMap);
                }
            }
        }
    }

}
