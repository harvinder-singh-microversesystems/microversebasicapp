package com.example.basicandroidapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface LocationReminderDao {
    @Query("SELECT * FROM location_reminders ORDER BY createdAt DESC")
    List<LocationReminder> getAllReminders();
    
    @Query("SELECT * FROM location_reminders WHERE isActive = 1")
    List<LocationReminder> getActiveReminders();
    
    @Insert
    long insertReminder(LocationReminder reminder);
    
    @Update
    void updateReminder(LocationReminder reminder);
    
    @Delete
    void deleteReminder(LocationReminder reminder);
    
    @Query("SELECT * FROM location_reminders WHERE id = :id")
    LocationReminder getReminderById(int id);
}
