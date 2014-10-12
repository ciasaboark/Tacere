/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.billing.KeySet;
import org.ciasaboark.tacere.billing.google.IabException;
import org.ciasaboark.tacere.billing.google.IabHelper;
import org.ciasaboark.tacere.billing.google.IabResult;
import org.ciasaboark.tacere.billing.google.Inventory;
import org.ciasaboark.tacere.billing.google.Purchase;

import java.util.UUID;

public class InAppBillingActivity extends Activity {
    private static final String TAG = "InAppBillingActivity";
    private final String SKU = KeySet.GOOGLE_SKU_UPGRADE;
    private IabHelper mHelper;
    private Button buyNowButton;
    private TextView purchaseResults;
    private UUID purchaseUUID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_billing);
        purchaseResults = (TextView) findViewById(R.id.purchace_results);
    }

    @Override
    public void onStart() {
        super.onStart();

        mHelper = new IabHelper(this, KeySet.GOOGLE_LICENSE_KEY);

        mHelper.enableDebugLogging(true, TAG);

        Log.w(TAG, "setup starting");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.w(TAG, "In-app Billing setup failed: " + result);
                } else {
                    Log.w(TAG, "In-app Billing is set up OK");

                    Inventory i = null;
                    try {
                        i = mHelper.queryInventory(true, null, null);
                        Log.w(TAG, "got inventory");
                        if (i.hasPurchase(SKU)) {
                            Log.w(TAG, "inventory contained correct sku");
                            buyNowButton.setEnabled(false);
                            purchaseResults.setText("User already owns sku " + SKU);
                        } else {
                            Log.w(TAG, "inventory did not contain sku");
                            purchaseResults.setText("user can now buy");
                            buyNowButton.setEnabled(true);
                        }
                    } catch (IabException e) {
                        Log.w(TAG, "unable to get an inventory of purchaces after setup finished");
                        purchaseResults.setText("unable to get a list of purchaced skus, you can try rebuying");
                        buyNowButton.setEnabled(true);
                    }
                }
            }
        });

        final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
                = new IabHelper.OnIabPurchaseFinishedListener() {

            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Log.w(TAG, "purchase finished");
                if (result.isFailure()) {
                    Log.w(TAG, "purchase result was failure");
                    purchaseResults.setText("purchase result was failure");
                    return;
                } else {
                    //it could be a success because it was a new purchase, or because the item is already owned
                    if (result.isOwned()) {
                        //TODO double check that the user really does own the item
                        Inventory i;
                        try {
                            i = mHelper.queryInventory(true, null, null);
                            if (i.hasPurchase(SKU)) {
                                purchaseResults.setText("result said item was already owned, and it is in inventory");      //COMPLETE SUCCESS
                                buyNowButton.setEnabled(false);
                            } else {
                                purchaseResults.setText("result said item was already owned, but it is not in inventory, bad purchase");
                            }
                        } catch (IabException e) {
                            Log.e(TAG, "error getting inventory");
                            purchaseResults.setText("result said the product was already owned, can not verify");
                        }
                    } else {
                        //TODO check that the new purchase was valid
                        Log.w(TAG, "purchase result was success, have to check the sku");
                        if (purchase.getSku().equals(SKU)) {
                            Log.w(TAG, "sku was correct");
                            String returnedUuidString = purchase.getDeveloperPayload();
                            UUID returnedUuid = UUID.fromString(returnedUuidString);
                            if (returnedUuid.equals(purchaseUUID)) {
                                Log.w(TAG, "purchase UUID matches, this was a good purchase");                              //COMPLETE SUCCESS
                                purchaseResults.setText("purchase was OK");
                            } else {
                                Log.w(TAG, "purchase UUID does not match, this was a bad purchase");
                                purchaseResults.setText("developer payload did not match");
                            }
                        } else {
                            Log.w(TAG, "given unknown sku, what did they buy?");
                        }
                    }

                }

            }
        };

        final Activity context = this;
        buyNowButton = (Button) findViewById(R.id.buy_now_button);
        buyNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UUID randomUUID = UUID.randomUUID();
                purchaseUUID = randomUUID;
                String uuidString = randomUUID.toString();

                mHelper.launchPurchaseFlow(context, SKU, 10001,
                        mPurchaseFinishedListener, uuidString);
            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.in_app_billing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (!mHelper.handleActivityResult(requestCode,
                resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
