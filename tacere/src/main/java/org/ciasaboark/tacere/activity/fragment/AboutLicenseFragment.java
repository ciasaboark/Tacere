/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.ciasaboark.tacere.R;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutLicenseFragment extends Fragment {
    private static final String TAG = "AboutLicenseActivity";
    private View rootView;
    private Context context;

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(context);
        context = ctx;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_about_license, container, false);
        initViews();
        return rootView;
    }

    private void initViews() {
        // Show the Up button in the action bar.
//        setupActionBar();

        WebView wv = (WebView) rootView.findViewById(R.id.webView1);

        String htmlData = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("license.html")));

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

        wv.loadData(htmlData, "text/html", "UTF8");

        // All links should open in the default browser, not this WebView
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        });

        wv.setBackgroundColor(0x00000000);
        wv.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
    }

//    /**
//     * Set up the {@link android.app.ActionBar}.
//     */
//    private void setupActionBar() {
//        try {
//            context.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);
//        } catch (NullPointerException e) {
//            Log.e(TAG, "unable to setup action bar");
//        }
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        ((Activity)context).getMenuInflater().inflate(R.menu.fragment_about_license, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask((Activity) context);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
