package edu.wineslab.hiro_net;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import javax.net.ssl.ManagerFactoryParameters;

/**
 * Created by Vladislav on 4/22/2018.
 */

public class SplashActivity extends AppCompatActivity {
    // Handles permissions
    private static final int PERMISSIONS_REQUEST = 0;

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH};

        // Request permissions for all required services
        if ((ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission_group.STORAGE) !=
                PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission_group.LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) !=
                        PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) !=
                        PackageManager.PERMISSION_GRANTED)) {
            Log.d(TAG, "Permissions have not been granted yet");
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Monitor successful permissions request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "User has granted all required permissions");
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }
        else if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        else Log.d(TAG, "Still waiting for permissions...");
    }
}
