/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.billing.KeySet;
import org.ciasaboark.tacere.billing.google.IabHelper;
import org.ciasaboark.tacere.billing.google.IabResult;
import org.ciasaboark.tacere.billing.google.Inventory;

public class LicenseCheckWrapper extends Activity {
    private static final String TAG = "LicenseCheckWrapper";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_check_wrapper);
        final Authenticator authenticator = new Authenticator(this);
        authenticator.deAuthenticate();
        final Context context = this;

        final IabHelper buyHelper = new IabHelper(this, KeySet.GOOGLE_LICENSE_KEY);
        buyHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // On error, say sorry and leave the app in demo mode
                    Log.e(TAG, "unable to contact licensing server: " + result.getMessage());
                    Toast.makeText(context, "Unable to contact licensing server, will remain in reduced mode", Toast.LENGTH_SHORT);
                    startMainActivity();
                    buyHelper.dispose();
                    finish();
                    return;
                }

                // Get a list of all products the user owns
                buyHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    @Override
                    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                        if (result.isFailure()) {
                            // If we could not get a list of the owned SKUs, say sorry and keep the demo version
                            Log.e(TAG, "error getting list of owned SKUs: " + result.getMessage());
                            startMainActivity();
                            buyHelper.dispose();
                            finish();
                            return;
                        } else {
                            boolean isPremium = inv.hasPurchase(KeySet.SKU_UPGRADE_PRO);

                            // Set the shared preferences to premium=true if the user owns it
                            authenticator.setAuthenticateSuccess(isPremium);
                            buyHelper.dispose();

                            // Forward to the currect activity depending on premium / demo mode
                            startMainActivity();
                            finish();
                            return;
                        }
                    }
                });
            }
        });
    }

    private void startMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
