package com.example.basicandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1000;
    
    private Button btnStartCapture;
    private Button btnStopCapture;
    private TextView tvStatus;
    private TextView tvWebAddress;
    
    private MediaProjectionManager projectionManager;
    private ScreenCaptureService captureService;
    private boolean serviceBound = false;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ScreenCaptureService.LocalBinder binder = (ScreenCaptureService.LocalBinder) service;
            captureService = binder.getService();
            serviceBound = true;
            Log.d(TAG, "Service connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            captureService = null;
            Log.d(TAG, "Service disconnected");
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }
    
    private void initViews() {
        btnStartCapture = findViewById(R.id.btnStartCapture);
        btnStopCapture = findViewById(R.id.btnStopCapture);
        tvStatus = findViewById(R.id.tvStatus);
        tvWebAddress = findViewById(R.id.tvWebAddress);
        
        btnStartCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScreenCapture();
            }
        });
        
        btnStopCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenCapture();
            }
        });
        
        updateUI(false);
    }
    
    private void requestScreenCapture() {
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                startScreenCaptureService(data);
            } else {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void startScreenCaptureService(Intent data) {
        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (serviceBound && captureService != null) {
                                captureService.startCapture(data);
                                updateUI(true);
                                String serverUrl = captureService.getServerUrl();
                                tvWebAddress.setText(getString(R.string.web_address, serverUrl));
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread interrupted", e);
                }
            }
        }).start();
    }
    
    private void stopScreenCapture() {
        if (serviceBound && captureService != null) {
            captureService.stopCapture();
        }
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        updateUI(false);
        tvWebAddress.setText("");
    }
    
    private void updateUI(boolean isRunning) {
        btnStartCapture.setEnabled(!isRunning);
        btnStopCapture.setEnabled(isRunning);
        tvStatus.setText(isRunning ? getString(R.string.status_running) : getString(R.string.status_stopped));
    }
    
    @Override
    protected void onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection);
        }
        super.onDestroy();
    }
}
