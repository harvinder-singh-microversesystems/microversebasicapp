package com.example.basicandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ReminderAdapter.OnReminderDeleteListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;
    
    private RecyclerView recyclerViewReminders;
    private TextView emptyStateText;
    private FloatingActionButton fabAddReminder;
    private ReminderAdapter reminderAdapter;
    private AppDatabase database;
    private LocationUtils locationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupDatabase();
        setupLocationUtils();
        checkPermissions();
        setupRecyclerView();
        loadReminders();
    }
    
    private void initViews() {
        recyclerViewReminders = findViewById(R.id.recyclerViewReminders);
        emptyStateText = findViewById(R.id.emptyStateText);
        fabAddReminder = findViewById(R.id.fabAddReminder);
        
        fabAddReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
                startActivity(intent);
            }
        });
    }
    
    private void setupDatabase() {
        database = AppDatabase.getDatabase(this);
    }
    
    private void setupLocationUtils() {
        locationUtils = new LocationUtils(this);
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, 
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    private void setupRecyclerView() {
        reminderAdapter = new ReminderAdapter(this);
        recyclerViewReminders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReminders.setAdapter(reminderAdapter);
    }
    
    private void loadReminders() {
        List<LocationReminder> reminders = database.locationReminderDao().getAllReminders();
        reminderAdapter.setReminders(reminders);
        
        if (reminders.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerViewReminders.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerViewReminders.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }
    
    @Override
    public void onReminderDelete(LocationReminder reminder) {
        database.locationReminderDao().deleteReminder(reminder);
        locationUtils.removeGeofence(reminder.id);
        loadReminders();
    }
}
