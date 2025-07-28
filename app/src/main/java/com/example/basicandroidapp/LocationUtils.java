package com.example.basicandroidapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    private Context context;
    private GeofencingClient geofencingClient;
    private FusedLocationProviderClient fusedLocationClient;
    
    public LocationUtils(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    public interface GeocodeCallback {
        void onSuccess(double latitude, double longitude);
        void onError(String error);
    }
    
    public void geocodeAddress(String address, GeocodeCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                callback.onSuccess(location.getLatitude(), location.getLongitude());
            } else {
                callback.onError("Address not found");
            }
        } catch (IOException e) {
            callback.onError("Geocoding failed: " + e.getMessage());
        }
    }
    
    public void addGeofence(LocationReminder reminder) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(reminder.id))
                .setCircularRegion(reminder.latitude, reminder.longitude, reminder.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();
        
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
        
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        PendingIntent geofencePendingIntent = PendingIntent.getBroadcast(context, reminder.id, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent);
    }
    
    public void removeGeofence(int reminderId) {
        geofencingClient.removeGeofences(List.of(String.valueOf(reminderId)));
    }
    
    public Task<Location> getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return fusedLocationClient.getLastLocation();
    }
}
