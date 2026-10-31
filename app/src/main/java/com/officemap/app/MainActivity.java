package com.officemap.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;
    private static final int REQ_LOC = 2;
    private GeolocationPermissions.Callback geoCb;
    private String geoOrigin;
    private WebView web;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                if (Build.VERSION.SDK_INT < 23
                        || MainActivity.this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    cb.invoke(origin, true, false);
                } else {
                    geoCb = cb;
                    geoOrigin = origin;
                    MainActivity.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQ_LOC && geoCb != null) {
            boolean ok = false;
            for (int r : results) if (r == PackageManager.PERMISSION_GRANTED) ok = true;
            geoCb.invoke(geoOrigin, ok, false);
            geoCb = null;
        } else {
            super.onRequestPermissionsResult(code, perms, results);
        }
    }

    @Override
    protected void onPause() {
        // حفظ فوري للتعديلات عند مغادرة التطبيق
        if (web != null) web.evaluateJavascript("window.__officeFlush && window.__officeFlush()", null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        String js = "(function(){var d=document,g=d.getElementById('dlg');"
                + "if(g&&!g.hidden){g.hidden=true;return 'x';}"
                + "if(d.body.classList.contains('vis-mode')){d.getElementById('vBack').click();return 'x';}"
                + "return '';})()";
        web.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if (v == null || v.equals("\"\"") || v.equals("null")) MainActivity.super.onBackPressed();
            }
        });
    }

    private class Bridge {
        @JavascriptInterface
        public boolean saveFile(String name, String base64, String mime) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
                OutputStream out;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) return false;
                    out = getContentResolver().openOutputStream(uri);
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir != null) dir.mkdirs();
                    out = new FileOutputStream(new File(dir, safe));
                }
                out.write(bytes);
                out.close();
                return true;
            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() { Toast.makeText(MainActivity.this, "تعذّر حفظ الملف", Toast.LENGTH_SHORT).show(); }
                });
                return false;
            }
        }
    }
}
