package com.example.basicandroidapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private WebStreamingServer webServer;
    private Handler mainHandler;
    
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    public class LocalBinder extends Binder {
        ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }
    
    private final IBinder binder = new LocalBinder();
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mainHandler = new Handler(Looper.getMainLooper());
        
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        webServer = new WebStreamingServer(8080);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }
    
    public void startCapture(Intent data) {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = manager.getMediaProjection(-1, data);
        
        setupImageReader();
        setupVirtualDisplay();
        
        try {
            webServer.start();
            Log.d(TAG, "Web server started on port 8080");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start web server", e);
        }
    }
    
    public void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        if (webServer != null) {
            webServer.stop();
        }
        
        stopForeground(true);
        stopSelf();
    }
    
    private void setupImageReader() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        processImage(image);
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        }, mainHandler);
    }
    
    private void setupVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            null
        );
    }
    
    private void processImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;
        
        Bitmap bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        
        if (webServer != null) {
            webServer.updateFrame(bitmap);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Screen sharing service");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Sharing Active")
                .setContentText("Your screen is being shared")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("Screen Sharing Active")
                .setContentText("Your screen is being shared")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        }
    }
    
    public String getServerUrl() {
        return webServer != null ? webServer.getServerUrl() : null;
    }
    
    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }
}
