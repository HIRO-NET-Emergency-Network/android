package edu.wineslab.hiro_net.Entities;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by Vladislav on 4/13/2018.
 */

public class RoutingTable {
    private static RoutingTable instance;
    private ArrayList<ArrayList<Peer>> nextHops = new ArrayList<>(1000);
    private ArrayList<Peer> destinations = new ArrayList<>(1000);
    private ArrayList<ArrayList<Integer>> numHopsFrom = new ArrayList<>(1000);
    // Maximum size of next-hops list
    private int capacityPerEntry = 5;
    // Persists throughout application's life-cycle
    private Random randomGenerator;
    private static Context context;

    private RoutingTable(Context context) {
        RoutingTable.context = context;

        randomGenerator = new Random();
        randomGenerator.setSeed(System.currentTimeMillis());
    }

    public static synchronized RoutingTable getInstance(Context context) {
        if (instance == null) {
            instance = new RoutingTable(context);
        }
        return instance;
    }

    public ArrayList<ArrayList<Peer>> getNextHopsList() {return this.nextHops; }

    public ArrayList<Peer> getDestinationList() {return this.destinations; }

    public ArrayList<ArrayList<Integer>> getNumHopsFromList() {return this.numHopsFrom; }

    public ArrayList<Peer> getNextHops(int index) { return this.nextHops.get(index); }

    public Peer getDestination(int index) { return this.destinations.get(index); }

    public ArrayList<Integer> getNumHopsFrom(int index) { return this.numHopsFrom.get(index); }

    public void addRoute(Peer nextHop, Peer newDestination, int numHops) {
        for(Peer destination : destinations) {
            // Check if we have a route for this destination
            if (destination.getUuid().equals(newDestination.getUuid())) {
                int index = destinations.indexOf(destination);
                ArrayList<Peer> nextHopsList = nextHops.get(index);
                // Check for space for alternate routes
                if (nextHopsList.size() < capacityPerEntry) {
                    nextHopsList.add(nextHop);
                    nextHops.set(index, nextHopsList);
                }
                // No space for alternate routes
                else {
                    ArrayList<Integer> numHopsList = numHopsFrom.get(index);
                    int maxNumHops = Collections.max(numHopsList);
                    int indexMax = numHopsList.indexOf(maxNumHops);
                    // Check if new route requires fewer hops than a current route
                    if (numHops < maxNumHops) {
                        // Change numHops to match new route
                        numHopsList.set(indexMax, numHops);
                        numHopsFrom.set(indexMax, numHopsList);
                        // Change nextHop to match new route
                        nextHopsList.set(indexMax, nextHop);
                        nextHops.set(indexMax, nextHopsList);
                    }
                }
            }
        }
        if (!destinations.contains(newDestination)) {
            ArrayList<Peer> nextHopsList = new ArrayList<>();
            nextHopsList.add(nextHop);
            ArrayList<Integer> numHopsList = new ArrayList<>();
            numHopsList.add(numHops);
            // Populate our routing table
            destinations.add(newDestination);
            nextHops.add(nextHopsList);
            numHopsFrom.add(numHopsList);
        }
    }

    public void removeRouteByID(String ID) {
        // Remove all routes if it is a destination
        for (Iterator<Peer> iterator = destinations.iterator(); iterator.hasNext(); ) {
            Peer dest = iterator.next();
            // Check if we have a route with this as destination
            if (dest.getUuid().equals(ID)) {
                int index = destinations.indexOf(dest);
                // Remove the whole entry (all associated routes)
                iterator.remove();
                nextHops.remove(index);
                numHopsFrom.remove(index);
            }
        }
        // Remove only routes where it is a next-hop
        ArrayList<Integer> removeListInds = new ArrayList<>();
        ArrayList<ArrayList<Integer>> removeEntryInds = new ArrayList<>();
        for (Iterator<ArrayList<Peer>> listIterator = nextHops.iterator(); listIterator.hasNext(); ) {
            ArrayList<Peer> nextHopsList = listIterator.next();
            int listIndex = nextHops.indexOf(nextHopsList);
            // Collect indices of all routes with this as next-hop
            ArrayList<Integer> entryInds = new ArrayList<>();
            for (Peer nextHop : nextHopsList) {
                // Check if we have a route with this as next-hop
                if (nextHop.getUuid().equals(ID)) {
                    int entryIndex = nextHopsList.indexOf(nextHop);
                    if (nextHopsList.size() > 1) {
                        // Don't remove destinations entry because there still other routes
                        removeListInds.add(listIndex);
                        entryInds.add(entryIndex);
                        ArrayList<Integer> numHopsList = numHopsFrom.get(listIndex);
                        // Remove number of hops entry corresponding to this node
                        numHopsList.remove(entryIndex);
                        numHopsFrom.set(listIndex, numHopsList);
                    } else {
                        // Remove the whole entry (this is the only route)
                        destinations.remove(entryIndex);
                        listIterator.remove();
                        numHopsFrom.remove(entryIndex);
                    }
                }
            }
            removeEntryInds.add(entryInds);
        }
        // Remove routes where this node is the next hop
        for (int listInd : removeListInds) {
            ArrayList<Peer> nextHopsList = nextHops.get(listInd);
            ArrayList<Integer> entryInds = removeEntryInds.get(listInd);
            // Remove all routes
            if (entryInds.size() == nextHopsList.size()) {
                destinations.remove(listInd);
                nextHops.remove(listInd);
                numHopsFrom.remove(listInd);
            }
            // Remove only some routes
            else {
                for (int entryInd : entryInds) {
                    nextHopsList.remove(entryInd);
                }
                nextHops.set(listInd, nextHopsList);
            }
        }

    }

    public Peer getNextHopByID(String destID) {
        int destInd = 0;
        for (Peer destination: destinations) {
            // Check for a route to this destination
            if (destination.getUuid().equals(destID)) {
                destInd = destinations.indexOf(destination);
            }
        }
        ArrayList<Integer> numHopsList = numHopsFrom.get(destInd);
        int minNumHops = Collections.min(numHopsList);
        ArrayList<Integer> duplicates = new ArrayList<Integer>();
        int dupInd = 0;
        // Check for routes with minimum # of hops
        for(Integer numHops : numHopsList) {
            if(numHops == minNumHops) { duplicates.add(dupInd); }
            dupInd++;
        }
        int minInd;
        // Random select from optimal routes
        if (duplicates.size() > 1) {
            dupInd = randomGenerator.nextInt(duplicates.size());
            minInd = duplicates.get(dupInd);
        }
        // Select only available route
        else { minInd = numHopsList.indexOf(minNumHops); }
        return nextHops.get(destInd).get(minInd);
    }

    public int getNumHopsByID(String destID) {
        int destInd = 0;
        for (Peer destination: destinations) {
            // Check for a route to this destination
            if (destination.getUuid().equals(destID)) {
                destInd = destinations.indexOf(destination);
            }
        }
        ArrayList<Integer> numHopsList = numHopsFrom.get(destInd);
        return Collections.min(numHopsList);
    }

    public boolean hasDestinationID(String ID) {
        for(Peer destination : destinations) {
            if(destination.getUuid().equals(ID)) {
                return true;
            }
        }
        return false;
    }

}
