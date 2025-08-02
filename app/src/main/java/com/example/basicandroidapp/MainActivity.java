package com.example.basicandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1000;
    
    private Button btnGetLocation;
    private Button btnStopLocation;
    private TextView tvStatus;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvAltitude;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isLocationUpdatesActive = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        initLocationServices();
    }
    
    private void initViews() {
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnStopLocation = findViewById(R.id.btnStopLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        tvAltitude = findViewById(R.id.tvAltitude);
        
        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationUpdates();
            }
        });
        
        btnStopLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationUpdates();
            }
        });
        
        updateUI(false);
        resetLocationDisplay();
    }
    
    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(5000)
                .build();
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationDisplay(location);
                }
            }
        };
    }
    
    private void requestLocationUpdates() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isLocationUpdatesActive = true;
            updateUI(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            updateUI(false);
        }
    }
    
    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION_PERMISSION);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void updateLocationDisplay(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double altitude = location.hasAltitude() ? location.getAltitude() : 0.0;
        
        tvLatitude.setText(getString(R.string.latitude, String.format("%.6f", latitude)));
        tvLongitude.setText(getString(R.string.longitude, String.format("%.6f", longitude)));
        
        if (location.hasAltitude()) {
            tvAltitude.setText(getString(R.string.altitude, String.format("%.1f", altitude)));
        } else {
            tvAltitude.setText(getString(R.string.altitude, "N/A"));
        }
    }
    
    private void resetLocationDisplay() {
        tvLatitude.setText(getString(R.string.latitude, "N/A"));
        tvLongitude.setText(getString(R.string.longitude, "N/A"));
        tvAltitude.setText(getString(R.string.altitude, "N/A"));
    }
    
    private void updateUI(boolean isRunning) {
        btnGetLocation.setEnabled(!isRunning);
        btnStopLocation.setEnabled(isRunning);
        tvStatus.setText(isRunning ? getString(R.string.status_running) : getString(R.string.status_stopped));
    }
    
    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }
}
