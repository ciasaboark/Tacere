/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.prefs.Updates;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShowUpdatesActivity extends Activity {
    private static final String TAG = "ShowUpdatesActivity";
    private boolean showingUpdatesFromMainScreen = false;
    private Updates updates;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        updates = new Updates(this, this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updates);
        Intent i = getIntent();
        Bundle b = i.getExtras();
        if (b != null) {
            String startedFrom = b.getString("initiator");
            if ("main".equals(startedFrom)) {
                showingUpdatesFromMainScreen = true;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.setTitle(R.string.updates_title);

        String htmlData = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("updates.html")));

            String line;
            while ((line = br.readLine()) != null) {
                htmlData += line;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        int colorInt = getResources().getColor(R.color.link_color);
        String hexColor = String.format("#%06X", (0xFFFFFF & colorInt));
        while (htmlData.contains("LINKCOLOR")) {
            htmlData = htmlData.replace("LINKCOLOR", hexColor);
        }

        WebView webView = (WebView) findViewById(R.id.updatesWebView);
        webView.loadUrl("file:///android_asset/empty.html");
        webView.loadData(htmlData, "text/html", "UTF8");

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
        webView.setBackgroundColor(0x00000000);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        webView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        Button closeButton = (Button) findViewById(R.id.updatesButton);
        if (showingUpdatesFromMainScreen) {
            closeButton.setText(R.string.hide_updates);
        } else {
            closeButton.setText(R.string.close);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updates.hideChangelogForCurrentAppVersion();
                finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updates.hideChangelogForCurrentAppVersion();
    }


}
