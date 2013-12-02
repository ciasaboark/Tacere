/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;

public class UpdatesActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_updates);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	WebView webView = (WebView)findViewById(R.id.updatesWebView);
    	webView.loadUrl("file:///android_asset/updates.html");
        
        //All links should open in the default browser, not this WebView
        //NOTE: this does not seem to work for POST links.
        webView.setWebViewClient(new WebViewClient() {
        	@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		Intent i = new Intent(Intent.ACTION_VIEW);
        		i.setData(Uri.parse(url));
        		startActivity(i);
        		return true;
        	}
        });
        
       final CheckBox checkBox = (CheckBox)findViewById(R.id.updatesCheckBox);
       checkBox.setText(R.string.hide_updates);
       checkBox.setChecked(DefPrefs.UPDATES_CHECKBOX);
       
       Button closeButton = (Button)findViewById(R.id.updatesButton);
       closeButton.setText(R.string.close);
       closeButton.setOnClickListener(new View.OnClickListener() {
    	   @Override
    	   public void onClick(View v) {
    		   boolean showUpdates = !checkBox.isChecked();
    		   SharedPreferences preferences = getApplicationContext().getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
    		   SharedPreferences.Editor editor = preferences.edit();
    		   editor.putBoolean(DefPrefs.UPDATES_VERSION, showUpdates);
    		   editor.apply();
    		   finish();
    	   }
       });
    }
}
