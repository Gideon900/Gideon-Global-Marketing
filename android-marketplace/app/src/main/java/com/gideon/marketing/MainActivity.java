package com.gideon.marketing;
import android.app.Activity;import android.os.Bundle;import android.webkit.*;import android.content.Intent;import android.net.Uri;
public class MainActivity extends Activity { private WebView web;
 @Override public void onCreate(Bundle b){super.onCreate(b);web=new WebView(this);WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setLoadsImagesAutomatically(true);s.setAllowFileAccess(false);web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){Uri u=r.getUrl();if(u.getHost()!=null&&u.getHost().equals("gideon900.github.io"))return false;startActivity(new Intent(Intent.ACTION_VIEW,u));return true;}});web.setWebChromeClient(new WebChromeClient());setContentView(web);if(b==null)web.loadUrl("https://gideon900.github.io/Gideon-Global-Marketing/");else web.restoreState(b);}
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);if(web!=null)web.saveState(b);}
}
