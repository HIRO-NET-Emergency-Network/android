package edu.wineslab.hiro_net;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

    // Handles permissions
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION_ACCESS = 0;
    private static final int PERMISSIONS_REQUEST_COARSE_LOCATION_ACCESS = 1;
    private static final int PERMISSIONS_REQUEST_BLUETOOTH = 2;
    private static final int PERMISSIONS_REQUEST_BLUETOOTH_ADMIN = 3;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5;

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
                if (isMoreAccurateLocation(newLocation, me.getLocation())) {
                    me.setLocation(newLocation);
                    Toast.makeText(getBaseContext(), "Location: " + me.getLocation().toString(), Toast.LENGTH_SHORT).show();
                }
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        // Request permissions for all required services
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSIONS_REQUEST_COARSE_LOCATION_ACCESS);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_FINE_LOCATION_ACCESS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH},PERMISSIONS_REQUEST_BLUETOOTH);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN},PERMISSIONS_REQUEST_BLUETOOTH_ADMIN);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
        assert locationManager != null;
        // Register listener for satellite positioning
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long) 0.0, (float) 0.0, locationListener);
        // Register listener for WiFi & cellular tower positioning
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (long) 0.0, (float) 0.0, locationListener);
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

    // Monitor successful permissions request
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION_ACCESS: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "User has granted permissions for fine location access");
                }
            }
            case PERMISSIONS_REQUEST_COARSE_LOCATION_ACCESS: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "User has granted permissions for coarse location access");
                }
            }
        }
    }

    private static int MILLISEC_PER_MIN = 1000 * 60;

    protected boolean isMoreAccurateLocation(Location location, Location currentLocation) {
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
