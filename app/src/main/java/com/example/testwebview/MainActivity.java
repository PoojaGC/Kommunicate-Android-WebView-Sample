package com.example.testwebview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private WebView webView;
    private WebSettings webSettings;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        //webView.clearHistory();
        //webView.clearCache(true);

        webSettings = webView.getSettings();

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);

        webView.setWebChromeClient(new ChromeClient());
        webView.loadUrl("https://google.com");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isStoragePermissionGranted();
                webView.loadUrl("javascript:(function(d, m){\n" +
                        "        var kommunicateSettings = \n" +
                        "            {\"appId\":\"22823b4a764f9944ad7913ddb3e43cae1\",\"popupWidget\":true,\"automaticChatOpenOnNavigation\":true};\n" +
                        "        var s = document.createElement(\"script\"); s.type = \"text/javascript\"; s.async = true;\n" +
                        "        s.src = \"https://widget.kommunicate.io/v2/kommunicate.app\";\n" +
                        "        var h = document.getElementsByTagName(\"head\")[0]; h.appendChild(s);\n" +
                        "        window.kommunicate = m; m._globals = kommunicateSettings;\n" +
                        "    })(document, window.kommunicate || {});\n");
            }
        });
        //webView.addJavascriptInterface(new KmWebViewJsInterface(KmWebViewActivity.this), JS_INTERFACE_NAME);
        // webView.loadUrl("https://kommunicate.io");


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        } else {
            Toast.makeText(getApplicationContext(),
                    "Upload may take as long as you see this message.",
                    Toast.LENGTH_LONG).show();
        }

        Uri[] results = null;

        //check good response
        if (resultCode == RESULT_OK) {
            if (data == null) {
                if (filePath != null) {
                    results = new Uri[]{Uri.parse(filePath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;


        if (requestCode != FILE_CHOOSER_RESULT_CODE || mUploadMessage == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri result = null;

        try {
            if (resultCode != RESULT_OK) {
                result = null;
            } else {
                // retrieve from the private variable if the intent is null
                result = data == null ? mCapturedImageURI : data.getData();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "activity :" + e, Toast.LENGTH_LONG).show();
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    private class ChromeClient extends WebChromeClient {
        // For Android 3.0+
        public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(Intent.createChooser(i, "File Selection"), FILE_CHOOSER_RESULT_CODE);
        }

        //For Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                    String acceptType, String capture) {
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(Intent.createChooser(i, "File Selection"), FILE_CHOOSER_RESULT_CODE);
        }

        // For Android 5.0+ --- this is the method that will be called most of the time
        public boolean onShowFileChooser(WebView view,
                                         ValueCallback<Uri[]> filePath,
                                         WebChromeClient.FileChooserParams fileChooserParams) {

            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");

            Intent[] intentArray = new Intent[0];

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select Option:");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;
        }
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
    }
}