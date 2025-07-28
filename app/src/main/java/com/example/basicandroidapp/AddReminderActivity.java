package com.example.basicandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;

public class AddReminderActivity extends AppCompatActivity {
    private TextInputEditText editTextTitle;
    private TextInputEditText editTextLocation;
    private TextInputEditText editTextNote;
    private Button buttonSaveReminder;
    private AppDatabase database;
    private LocationUtils locationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        initViews();
        setupDatabase();
        setupLocationUtils();
        setupSaveButton();
    }
    
    private void initViews() {
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextNote = findViewById(R.id.editTextNote);
        buttonSaveReminder = findViewById(R.id.buttonSaveReminder);
    }
    
    private void setupDatabase() {
        database = AppDatabase.getDatabase(this);
    }
    
    private void setupLocationUtils() {
        locationUtils = new LocationUtils(this);
    }
    
    private void setupSaveButton() {
        buttonSaveReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReminder();
            }
        });
    }
    
    private void saveReminder() {
        String title = editTextTitle.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String note = editTextNote.getText().toString().trim();
        
        if (title.isEmpty()) {
            editTextTitle.setError("Title is required");
            return;
        }
        
        if (location.isEmpty()) {
            editTextLocation.setError("Location is required");
            return;
        }
        
        if (note.isEmpty()) {
            editTextNote.setError("Note is required");
            return;
        }
        
        buttonSaveReminder.setEnabled(false);
        buttonSaveReminder.setText("Saving...");
        
        locationUtils.geocodeAddress(location, new LocationUtils.GeocodeCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                LocationReminder reminder = new LocationReminder(title, note, latitude, longitude, 150.0f);
                long reminderId = database.locationReminderDao().insertReminder(reminder);
                reminder.id = (int) reminderId;
                
                locationUtils.addGeofence(reminder);
                
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddReminderActivity.this, "Reminder saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonSaveReminder.setEnabled(true);
                        buttonSaveReminder.setText("Save Reminder");
                        Toast.makeText(AddReminderActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
