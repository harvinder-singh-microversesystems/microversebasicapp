package com.example.basicandroidapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_reminders")
public class LocationReminder {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String note;
    public double latitude;
    public double longitude;
    public float radius;
    public boolean isActive;
    public long createdAt;
    
    public LocationReminder(String title, String note, double latitude, double longitude, float radius) {
        this.title = title;
        this.note = note;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }
}
