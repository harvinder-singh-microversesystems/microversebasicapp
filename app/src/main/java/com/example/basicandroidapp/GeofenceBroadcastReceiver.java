package com.example.basicandroidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            return;
        }
        
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            
            for (Geofence geofence : triggeringGeofences) {
                String requestId = geofence.getRequestId();
                int reminderId = Integer.parseInt(requestId);
                
                AppDatabase database = AppDatabase.getDatabase(context);
                LocationReminder reminder = database.locationReminderDao().getReminderById(reminderId);
                
                if (reminder != null && reminder.isActive) {
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.showLocationNotification(reminder);
                }
            }
        }
    }
}
