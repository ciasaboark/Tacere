/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.R.id;
import org.ciasaboark.tacere.versioning.Versioning;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutActivity extends Activity {
    private static final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        // Show the Up button in the action bar.
        setupActionBar();

        Drawable upIcon = getResources().getDrawable(R.drawable.info_icon);
        int c = getResources().getColor(R.color.header_text_color);
        upIcon.mutate().setColorFilter(c, PorterDuff.Mode.MULTIPLY);
        getActionBar().setIcon(upIcon);

        WebView wv = (WebView) findViewById(R.id.webView1);
        String htmlData = "";
        try {
//            FileInputStream fis = new FileInputStream("file:///android_asset/about.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("about.html")));

            String line;
            while ((line = br.readLine()) != null) {
                htmlData += line;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        int colorInt = getResources().getColor(R.color.accent);
        String hexColor = String.format("#%06X", (0xFFFFFF & colorInt));
        while (htmlData.contains("LINKCOLOR")) {
            htmlData = htmlData.replace("LINKCOLOR", hexColor);
        }

        wv.loadData(htmlData, "text/html", "UTF8");

        // All links should open in the default browser, not this WebView
        // NOTE: this does not seem to work for POST links.
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
        });

        wv.setBackgroundColor(0x00000000);
        wv.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        TextView versionText = (TextView) findViewById(id.about_version);
        String formattedVersion = String.format(getString(R.string.about_version), Versioning.getVersionCode());
        versionText.setText(formattedVersion);
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setIcon(R.drawable.info_icon);
        } catch (NullPointerException e) {
            Log.e(TAG, "unable to setup action bar");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean itemProcessedHere = false;
        switch (item.getItemId()) {
            case id.action_about_tutorial:
                Intent tutorialIntent = new Intent(this, TutorialActivity.class);
                startActivity(tutorialIntent);
                itemProcessedHere = true;
                break;
            case R.id.action_about_license:
                Intent licenseIntent = new Intent(this, org.ciasaboark.tacere.activity.AboutLicenseActivity.class);
                licenseIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(licenseIntent);
                itemProcessedHere = true;
                break;
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                itemProcessedHere = true;
                break;
            case R.id.action_about_updates:
                ShowUpdatesActivity.showUpdatesDialog(this);
                itemProcessedHere = true;
                break;
        }

        return itemProcessedHere;
    }

}
