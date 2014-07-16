/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.versioning.Versioning;

public class UpdatesActivity extends Activity {
    private Prefs prefs;
    private boolean showingUpdatesFromMainScreen = false;

    public static void showUpdatesDialogIfNeeded(Context ctx) {
        boolean showUpdates = shouldChangelogForCurrentAppVersionBeShown(ctx);
        if (showUpdates) {
            Intent updatesIntent = new Intent(ctx, UpdatesActivity.class);
            updatesIntent.putExtra("initiator", "main"); //TODO need a better way of keeping track of who started this activity
            ctx.startActivity(updatesIntent);
        }
    }

    public static void showUpdatesDialog(Context ctx) {
        Intent updatesIntent = new Intent(ctx, UpdatesActivity.class);
        ctx.startActivity(updatesIntent);
    }

    private static boolean shouldChangelogForCurrentAppVersionBeShown(Context ctx) {
        Prefs staticPrefs = new Prefs(ctx);
        boolean shouldChangelogBeShown = false;
        //the updates dialog should be shown if no value has been stored for the current app version
        try {
            staticPrefs.getBoolean(Versioning.getVersionCode());
        } catch (IllegalArgumentException e) {
            shouldChangelogBeShown = true;
        }
        return shouldChangelogBeShown;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        prefs = new Prefs(this);
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
        WebView webView = (WebView) findViewById(R.id.updatesWebView);
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
        webView.setBackgroundColor(0x00000000);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        Button closeButton = (Button) findViewById(R.id.updatesButton);
        if (showingUpdatesFromMainScreen) {
            closeButton.setText(R.string.hide_updates);
        } else {
            closeButton.setText(R.string.close);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideChangelogForCurrentAppVersion();
                finish();
            }
        });
    }

    private void hideChangelogForCurrentAppVersion() {
        try {
            prefs.storePreference(Versioning.getVersionCode(), false);
        } catch (IllegalArgumentException e) {
            //boolean values are accepted, should not reach here
        }
    }
}
