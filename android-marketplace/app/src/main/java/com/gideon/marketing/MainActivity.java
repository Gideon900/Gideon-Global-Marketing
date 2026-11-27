package com.gideon.marketing;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> uploadCallback;
  private static final int FILE_CHOOSER_REQUEST = 501;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setLoadsImagesAutomatically(true);
    s.setAllowFileAccess(false);
    web.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        if (uri.getHost() != null && uri.getHost().equals("gideon900.github.io")) return false;
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        uploadCallback = callback;
        Intent intent = params.createIntent();
        try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); return true; }
        catch (Exception e) { uploadCallback = null; return false; }
      }
    });
    setContentView(web);
    if (state == null) web.loadUrl("https://gideon900.github.io/Gideon-Global-Marketing/");
    else web.restoreState(state);
  }
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_CHOOSER_REQUEST && uploadCallback != null) {
      Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
      uploadCallback.onReceiveValue(results);
      uploadCallback = null;
    }
  }
  @Override public void onBackPressed() {
    if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (web != null) web.saveState(outState);
  }
}
