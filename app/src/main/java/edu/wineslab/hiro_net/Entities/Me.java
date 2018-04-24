package edu.wineslab.hiro_net.Entities;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.bridgefy.sdk.client.Bridgefy;
import com.google.gson.Gson;
import com.instacart.library.truetime.TrueTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import edu.wineslab.hiro_net.R;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Vladislav on 4/13/2018.
 */

public class Me {
    private static Me instance;
    private String  name;
    private String  uuid;
    private String location;
    private Peer.DeviceType type;
    private long clock_offset;
    private String file_name_sent;
    private String file_name_receive;
    private String file_name_forward;
    private String file_name_routing_table;
    private String file_name_clock_offset;
    private ArrayList<String> data_header;
    private ArrayList<String> routing_table_header;
    private ArrayList<String> clock_offset_header;
    private static Context context;

    private Me(Context context) {
        Me.context = context;

        // Get my clock's offset from the TrueTime server
        new InitTrueTimeAsyncTask().execute();
        // Wait for initialization...
        while (!TrueTime.isInitialized()) {}
        // Get current time from TrueTime server
        Date trueTime = TrueTime.now();
        // Offset (ms) between TrueTime server time (ms) and device time (ms)
        long offset_time = trueTime.getTime() - System.currentTimeMillis();
        this.clock_offset = offset_time;

        this.clock_offset = 0;

        // Where to save experimental data
        this.file_name_sent = String.format("sent_messages_%s.txt", Long.toString(System.currentTimeMillis()));
        this.file_name_receive = String.format("received_messages_%s.txt", Long.toString(System.currentTimeMillis()));
        this.file_name_forward = String.format("forwarded_messages_%s.txt", Long.toString(System.currentTimeMillis()));
        this.file_name_routing_table = String.format("routing_table_messages_%s.txt", Long.toString(System.currentTimeMillis()));
        this.file_name_clock_offset = String.format("clock_offset_%s.txt", Long.toString(System.currentTimeMillis()));

        // Headers for experiment files
        this.data_header = new ArrayList<String>() {{
            add("Current Time");
            add("Creator ID");
            add("Destination ID");
            add("Sender ID");
            add("Number of Hops Travelled");
            add("Message ID");
        }};
        this.routing_table_header = new ArrayList<String>() {{
            add("Current Time");
            add("Next Hop List");
            add("Number of Hops List");
            add("Destination ID");
        }};
        this.clock_offset_header = new ArrayList<String>() {{
           add("Clock Offset");
           add("Node ID");
        }};

        // Get my name
        this.name = String.format("%s_%s", Build.MANUFACTURER, Build.MODEL);
    }

    public static synchronized Me getInstance(Context context) {
        if (instance == null) {
            instance = new Me(context);
        }
        return instance;
    }

    public String getName() { return name; }

    public String getUuid() { return uuid; }

    public String getLocation() { return location; }

    public long getTime() { return clock_offset; }

    public Peer.DeviceType getType() { return type; }

    public void setUUID(String uuid) { this.uuid = uuid; }

    public void setLocation(String location) { this.location = location; }

    public void setType(Peer.DeviceType type) { this.type = type; }

    public String getFileNameSent() {
        return file_name_sent;
    }
    public String getFileNameReceive() {
        return file_name_receive;
    }
    public String getFileNameForward() {
        return file_name_forward;
    }
    public String getFileNameRoutingTable() {
        return file_name_routing_table;
    }
    public String getFileNameClockOffset() {
        return file_name_clock_offset;
    }
    public ArrayList<String> getDataHeader() {
        return data_header;
    }
    public ArrayList<String> getRoutingTableHeader() {
        return routing_table_header;
    }
    public ArrayList<String> getClockOffsetHeader() {
        return clock_offset_header;
    }

    private static class InitTrueTimeAsyncTask
            extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            try {
                TrueTime.build()
                        .withNtpHost("time.apple.com")
                        .withLoggingEnabled(false)
                        .withConnectionTimeout(20000)
                        .initialize();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Me", "Error accessing TrueTime object", e);
            }
            return null;
        }
    }

    // Check if file already exists
    public boolean fileExists(Context context, String filename) {
        File file = context.getFileStreamPath(filename);
        return !(file == null || !file.exists());
    }

    // save data on file
    public void save_data(Context context, ArrayList<String> header,
                          String fileName, ArrayList data) {
        // Convert arrays to delimited strings
        String header_str = TextUtils.join("\t", header);
        String data_str = TextUtils.join("\t", data);

        File root = new File(Environment.
                getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath());
        File dataFile = new File(root, fileName);

        // If file doesn't exist
        if (!fileExists(context, fileName)) {
            try {
                // Add headers
                final boolean newFile = dataFile.createNewFile();
                FileWriter writer = new FileWriter(dataFile);
                writer.append(header_str);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            // Add actual data
            FileWriter writer = new FileWriter(dataFile);
            writer.append(data_str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String locationToString(Location location) {
        return new Gson().toJson(location);
    }

    public Location locationFromString(String jsonString) {
        return new Gson().fromJson(jsonString, Location.class);
    }
}
