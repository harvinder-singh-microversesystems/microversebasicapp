package com.example.basicandroidapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private EditText addressBar;
    private Button btnBack, btnForward, btnRefresh, btnHome, btnGo, btnBookmark, btnSettings;
    private SharedPreferences bookmarks;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupWebView();
        setupEventListeners();
        loadHomePage();
    }
    
    private void initViews() {
        webView = findViewById(R.id.webView);
        addressBar = findViewById(R.id.etAddressBar);
        btnBack = findViewById(R.id.btnBack);
        btnForward = findViewById(R.id.btnForward);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnHome = findViewById(R.id.btnHome);
        btnGo = findViewById(R.id.btnGo);
        btnBookmark = findViewById(R.id.btnBookmark);
        btnSettings = findViewById(R.id.btnSettings);
        
        bookmarks = getSharedPreferences("HarvinderBrowserBookmarks", MODE_PRIVATE);
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                addressBar.setText(url);
                updateNavigationButtons();
                injectAdBlockingScript(view);
            }
            
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                addressBar.setText(url);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    updateNavigationButtons();
                }
            }
        });
    }
    
    private void setupEventListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnForward.setOnClickListener(v -> goForward());
        btnRefresh.setOnClickListener(v -> refresh());
        btnHome.setOnClickListener(v -> goHome());
        btnGo.setOnClickListener(v -> navigate());
        btnBookmark.setOnClickListener(v -> addBookmark());
        btnSettings.setOnClickListener(v -> showSettings());
        
        addressBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    navigate();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void navigate() {
        String url = addressBar.getText().toString().trim();
        if (url.isEmpty()) return;
        
        if (!url.contains("://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            }
        }
        
        webView.loadUrl(url);
    }
    
    private void goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }
    
    private void goForward() {
        if (webView.canGoForward()) {
            webView.goForward();
        }
    }
    
    private void refresh() {
        webView.reload();
    }
    
    private void goHome() {
        webView.loadUrl("https://www.google.com");
        addressBar.setText("https://www.google.com");
    }
    
    private void updateNavigationButtons() {
        btnBack.setEnabled(webView.canGoBack());
        btnForward.setEnabled(webView.canGoForward());
    }
    
    private void addBookmark() {
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url != null && title != null) {
            SharedPreferences.Editor editor = bookmarks.edit();
            editor.putString(title, url);
            editor.apply();
            Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");
        
        String[] options = {"View Bookmarks", "Clear Browsing Data", "About"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showBookmarks();
                    break;
                case 1:
                    clearBrowsingData();
                    break;
                case 2:
                    showAbout();
                    break;
            }
        });
        
        builder.show();
    }
    
    private void showBookmarks() {
        Map<String, ?> bookmarkMap = bookmarks.getAll();
        if (bookmarkMap.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] titles = bookmarkMap.keySet().toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bookmarks");
        builder.setItems(titles, (dialog, which) -> {
            String selectedTitle = titles[which];
            String url = bookmarks.getString(selectedTitle, "");
            if (!url.isEmpty()) {
                webView.loadUrl(url);
                addressBar.setText(url);
            }
        });
        
        builder.setNegativeButton("Close", null);
        builder.show();
    }
    
    private void clearBrowsingData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Browsing Data");
        builder.setMessage("This will clear your browsing history, cache, and cookies. Continue?");
        builder.setPositiveButton("Clear", (dialog, which) -> {
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About Harvinder Browser");
        builder.setMessage("Harvinder Browser v1.0.0\n\nA custom browser for Android\nCreated by Harvinder Singh\n\nBuilt with Android WebView\n\nFeatures:\n• Ad blocking (ads resized to 10x10 pixels)\n• JavaScript support\n• Bookmark management\n• Full web browsing");
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    private void loadHomePage() {
        webView.loadUrl("https://www.google.com");
        addressBar.setText("https://www.google.com");
    }
    
    private void injectAdBlockingScript(WebView webView) {
        String adBlockingScript = 
            "(function() {" +
            "  var adSelectors = [" +
            "    'iframe[src*=\"ads\"]'," +
            "    'iframe[src*=\"doubleclick\"]'," +
            "    'iframe[src*=\"googlesyndication\"]'," +
            "    'iframe[src*=\"googleadservices\"]'," +
            "    'div[id*=\"ad\"]'," +
            "    'div[class*=\"ad\"]'," +
            "    'div[class*=\"advertisement\"]'," +
            "    'div[class*=\"banner\"]'," +
            "    'div[id*=\"banner\"]'," +
            "    'div[class*=\"sponsor\"]'," +
            "    'div[id*=\"sponsor\"]'," +
            "    'ins.adsbygoogle'," +
            "    '[data-ad-slot]'," +
            "    '[data-ad-client]'," +
            "    '.adsbox'," +
            "    '.advertisement'," +
            "    '.google-ads'," +
            "    '.sponsored'," +
            "    '.ad-container'," +
            "    '.ad-banner'," +
            "    '.ad-wrapper'" +
            "  ];" +
            "  " +
            "  function resizeAds() {" +
            "    adSelectors.forEach(function(selector) {" +
            "      var elements = document.querySelectorAll(selector);" +
            "      elements.forEach(function(element) {" +
            "        element.style.width = '10px !important';" +
            "        element.style.height = '10px !important';" +
            "        element.style.maxWidth = '10px !important';" +
            "        element.style.maxHeight = '10px !important';" +
            "        element.style.minWidth = '10px !important';" +
            "        element.style.minHeight = '10px !important';" +
            "        element.style.overflow = 'hidden !important';" +
            "        element.style.visibility = 'visible !important';" +
            "        element.style.display = 'block !important';" +
            "      });" +
            "    });" +
            "  }" +
            "  " +
            "  resizeAds();" +
            "  " +
            "  var observer = new MutationObserver(function(mutations) {" +
            "    mutations.forEach(function(mutation) {" +
            "      if (mutation.type === 'childList') {" +
            "        resizeAds();" +
            "      }" +
            "    });" +
            "  });" +
            "  " +
            "  observer.observe(document.body, {" +
            "    childList: true," +
            "    subtree: true" +
            "  });" +
            "  " +
            "  setTimeout(resizeAds, 1000);" +
            "  setTimeout(resizeAds, 3000);" +
            "  setTimeout(resizeAds, 5000);" +
            "})();";
        
        webView.evaluateJavascript(adBlockingScript, null);
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
