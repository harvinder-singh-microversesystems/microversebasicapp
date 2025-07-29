package com.example.basicandroidapp;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class WebStreamingServer extends NanoHTTPD {
    private static final String TAG = "WebStreamingServer";
    private Bitmap currentFrame;
    private final Object frameLock = new Object();
    
    public WebStreamingServer(int port) {
        super(port);
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Log.d(TAG, "Request: " + uri);
        
        if ("/".equals(uri)) {
            return newFixedLengthResponse(getWebPageHTML());
        } else if ("/frame".equals(uri)) {
            return handleFrameRequest();
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }
    
    private Response handleFrameRequest() {
        synchronized (frameLock) {
            if (currentFrame != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    currentFrame.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] imageBytes = baos.toByteArray();
                    String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                    
                    Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", base64Image);
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                } catch (Exception e) {
                    Log.e(TAG, "Error encoding frame", e);
                }
            }
        }
        
        Response response = newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain", "");
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }
    
    private String getWebPageHTML() {
        return "<!DOCTYPE html>" +
               "<html><head>" +
               "<title>Screen Share</title>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
               "<style>" +
               "body { margin: 0; padding: 20px; background: #000; font-family: Arial, sans-serif; }" +
               "h1 { color: white; text-align: center; }" +
               "#screen { max-width: 100%; height: auto; border: 2px solid #333; display: block; margin: 0 auto; }" +
               "#status { color: white; text-align: center; margin: 10px 0; }" +
               "</style>" +
               "</head><body>" +
               "<h1>Phone Screen Mirror</h1>" +
               "<div id='status'>Connecting...</div>" +
               "<img id='screen' src='' alt='Phone Screen' />" +
               "<script>" +
               "const img = document.getElementById('screen');" +
               "const status = document.getElementById('status');" +
               "let isConnected = false;" +
               "function updateFrame() {" +
               "  fetch('/frame')" +
               "    .then(response => {" +
               "      if (response.ok && response.status !== 204) {" +
               "        return response.text();" +
               "      }" +
               "      throw new Error('No frame available');" +
               "    })" +
               "    .then(base64 => {" +
               "      img.src = 'data:image/jpeg;base64,' + base64;" +
               "      if (!isConnected) {" +
               "        status.textContent = 'Connected - Live Screen';" +
               "        isConnected = true;" +
               "      }" +
               "    })" +
               "    .catch(err => {" +
               "      if (isConnected) {" +
               "        status.textContent = 'Waiting for screen data...';" +
               "        isConnected = false;" +
               "      }" +
               "    })" +
               "    .finally(() => {" +
               "      setTimeout(updateFrame, 100);" +
               "    });" +
               "}" +
               "updateFrame();" +
               "</script>" +
               "</body></html>";
    }
    
    public void updateFrame(Bitmap bitmap) {
        synchronized (frameLock) {
            if (currentFrame != null && !currentFrame.isRecycled()) {
                currentFrame.recycle();
            }
            currentFrame = bitmap.copy(bitmap.getConfig(), false);
        }
    }
    
    public String getServerUrl() {
        try {
            String ipAddress = getLocalIpAddress();
            if (ipAddress != null) {
                return "http://" + ipAddress + ":8080";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting server URL", e);
        }
        return "http://localhost:8080";
    }
    
    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP", e);
        }
        return null;
    }
    
    @Override
    public void stop() {
        synchronized (frameLock) {
            if (currentFrame != null && !currentFrame.isRecycled()) {
                currentFrame.recycle();
                currentFrame = null;
            }
        }
        super.stop();
    }
}
