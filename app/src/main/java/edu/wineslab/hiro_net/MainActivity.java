package edu.wineslab.hiro_net;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bridgefy.sdk.client.Bridgefy;

import edu.wineslab.hiro_net.Entities.Me;

public class MainActivity extends AppCompatActivity {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */

    // For accessing all tabs in the activity
    private SectionsPagerAdapter mSectionsPagerAdapter;

    // Hosts tab's content
    private ViewPager mViewPager;

    // Activity's tag
    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Returns fragment for each tab in the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), getApplicationContext());

        // Set up the ViewPager with our fragments adapter
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Listen for change in selected tab
        TabLayout tabLayout = findViewById(R.id.tabs);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
            @Override
            public void onPageSelected(int position) {
                mViewPager.setCurrentItem(tabLayout.getSelectedTabPosition());
            }
        });
        // Display selected tab
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }
        });

        // Identity of this device
        Me me = Me.getInstance(getApplicationContext());

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Location access permissions
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location newLocation) {
                // Store this device's location
                if (isMoreAccurateLocation(newLocation, me.locationFromString(me.getLocation()))) {
                    me.setLocation(me.locationToString(newLocation));
                    Log.d(TAG, "Location: " + me.getLocation());
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            public void onProviderEnabled(String provider) {
            }
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission_group.LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            assert locationManager != null;
            // Register listener for GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) 0.0, (float) 0.0, locationListener);
            // Register listener for WiFi & cellular tower positioning
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (long) 0.0, (float) 0.0, locationListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu to add items to action bar, when present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handles clicks on Home/Up button (based on parent activity
        // in AndroidManifest.xml)
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected boolean isMoreAccurateLocation(Location location, Location currentLocation) {
        int MILLISEC_PER_MIN = 1000 * 60;

        // A measurement is better than none
        if (currentLocation == null) {
            return true;
        }

        // Is this measurement recent?
        long timeDelta = location.getTime() - currentLocation.getTime();
        boolean newer = timeDelta > MILLISEC_PER_MIN;
        boolean older = timeDelta < -MILLISEC_PER_MIN;

        if (newer) { return true; }
        else if (older) { return false; }

        // This measurement is recent
        // Is it accurate?
        float accuracyDelta = location.getAccuracy() - currentLocation.getAccuracy();

        return (accuracyDelta < 0.0);
    }
}
