/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.billing.Authenticator;
import org.ciasaboark.tacere.billing.KeySet;
import org.ciasaboark.tacere.billing.google.IabHelper;
import org.ciasaboark.tacere.billing.google.IabResult;
import org.ciasaboark.tacere.billing.google.Inventory;
import org.ciasaboark.tacere.billing.google.Purchase;
import org.ciasaboark.tacere.billing.google.SkuDetails;

import java.util.ArrayList;
import java.util.UUID;

public class ProUpgradeActivity extends Activity {
    private static final String TAG = "ProUpgradeActivity";
    private final String uuidString = UUID.randomUUID().toString();
    private final String SKU = KeySet.SKU_UPGRADE_PRO;
    private LinearLayout upgradeIntro;
    private LinearLayout upgradeWaiting;
    private LinearLayout upgradePurchaseNotAvailable;
    private LinearLayout upgradePurchaseAlreadyPurchased;
    private LinearLayout upgradePurchaseAvailable;
    private LinearLayout upgradePurchaseSuccess;
    private LinearLayout upgradePurchaseFailed;
    private String upgradePrice = "unknown";
    private IabHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_upgrade);
        init();
    }

    private void init() {
        upgradeIntro = (LinearLayout) findViewById(R.id.upgrade_intro);
        upgradeWaiting = (LinearLayout) findViewById(R.id.upgrade_wait);
        upgradePurchaseNotAvailable = (LinearLayout) findViewById(R.id.upgrade_purchase_not_available);
        upgradePurchaseAlreadyPurchased = (LinearLayout) findViewById(R.id.upgrade_purchase_already_purchased);
        upgradePurchaseAvailable = (LinearLayout) findViewById(R.id.upgrade_purchase_available);
        upgradePurchaseSuccess = (LinearLayout) findViewById(R.id.upgrade_purchase_success);
        upgradePurchaseFailed = (LinearLayout) findViewById(R.id.upgrade_purchase_failed);

        showIntroOrThanks();
    }

    private void showIntroOrThanks() {
        Authenticator authenticator = new Authenticator(this);
        if (authenticator.isAuthenticated()) {
            showPurchaseAlreadyPurchased();
        } else {
            showIntro();
        }
    }

    private void showPurchaseAlreadyPurchased() {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.VISIBLE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.GONE);

        Button closeButton = (Button) findViewById(R.id.upgrade_purchase_already_purchased_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void showIntro() {
        upgradeIntro.setVisibility(View.VISIBLE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.GONE);

        Button startUpgradeButton = (Button) findViewById(R.id.upgrades_start_upgrade);
        startUpgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginUpgrade();
            }
        });

        Button cancelUpgradeButton = (Button) findViewById(R.id.upgrades_cancel_upgrade);
        cancelUpgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void beginUpgrade() {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.VISIBLE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.GONE);

        Log.d(TAG, "beginning upgrade process");
        mHelper = new IabHelper(this, KeySet.GOOGLE_LICENSE_KEY);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "upgrade setup finished");
                if (result.isSuccess()) {
                    Log.d(TAG, "success, in-app billing is available");
                    // Fill a list of SKUs that we want the price infos for
                    // (SKU = "stockable unit" = buyable things)
                    ArrayList<String> moreSkus = new ArrayList<String>();
                    moreSkus.add(SKU);

                    // We initialize the price field with a "retrieving price" message while we wait
                    // for the price
//                    final TextView tvPrice = (TextView)BuyPremiumActivity.this.findViewById(R.id.price);
//                    tvPrice.setText(R.string.waiting_for_price);

                    // Start the query for the details for the SKUs. This runs asynchronously, so
                    // it may be that the price appears a bit later after the rest of the Activity is shown.
                    mHelper.queryInventoryAsync(true, moreSkus, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                // If we successfully got the price, show it in the text field
                                SkuDetails details = inv.getSkuDetails(SKU);
                                String price = details.getPrice();

                                // On successful init and price getting, enable the "buy me" button
                                showPurchaseAvailable();
                            } else {
                                // Error getting the price... show a sorry text in the price field now
                                showPurchaseNotAvailable("Error getting pricing information " + result.getMessage());
                            }
                        }
                    });
                } else {
                    Log.d(TAG, "Unable to initialize the in app billing process");
                    // If the billing API could not be initialized at all, show a sorry dialog. This
                    // will surely prevent the user from being able to buy anything.
                    showPurchaseNotAvailable("Unable to initialize the in app billing process: " + result.getMessage());
                }
            }
        });
    }

    private void showPurchaseAvailable() {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.VISIBLE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.GONE);

        IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                Authenticator authenticator = new Authenticator(getApplicationContext());
                if (result.isFailure() || purchase == null) {
                    authenticator.setAuthenticateSuccess(false);
                    switch (result.getResponse()) {
                        case IabHelper.IABHELPER_USER_CANCELLED:
                            finish();
                            break;
                        case IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR:
                            showPurchaseFailed("Something went wrong during the purchase process.  Please try again later");
                            break;
                        case IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED:
                            showPurchaseAlreadyPurchased();
                            break;
                        default:
                            showPurchaseFailed("Purchase failed (" + result.getMessage() + ").  Please try again later");
                    }
                } else if (purchase.getSku().equals(SKU)) {
                    String developerPayload = purchase.getDeveloperPayload();
                    if (TextUtils.equals(uuidString, developerPayload)) {
                        authenticator.setAuthenticateSuccess(true);
                        showPurchaseSuccess();
                    } else {
                        showPurchaseFailed("Unable to verify the purchase details.  This should not have happened.  Please contact the developer for more information.");
                    }
                } else {
                    //something was purchased, but I don't know what it was
                    authenticator.setAuthenticateSuccess(false);
                    showPurchaseFailed("The Google Play Store has notified me that something was purchased, buy I have no idea what it was.  This should not have happened.  Please contact the developer for more information");
                }

            }
        };

        //launch the in app billing activity
        mHelper.launchPurchaseFlow(this, SKU, 1, mPurchaseFinishedListener,
                uuidString);
    }

    private void showPurchaseNotAvailable(String message) {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.VISIBLE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.GONE);
        TextView responseText = (TextView) findViewById(R.id.upgrade_purchase_not_available_text);
        responseText.setText("Purchase not available, " + message);
        //TODO
    }

    private void showPurchaseFailed(String reason) {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.GONE);
        upgradePurchaseFailed.setVisibility(View.VISIBLE);

        TextView failedMessage = (TextView) findViewById(R.id.upgrade_purchase_failed_message);
        failedMessage.setText(reason);

        Button closeButton = (Button) findViewById(R.id.upgrade_purchase_failed_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void showPurchaseSuccess() {
        upgradeIntro.setVisibility(View.GONE);
        upgradeWaiting.setVisibility(View.GONE);
        upgradePurchaseNotAvailable.setVisibility(View.GONE);
        upgradePurchaseAlreadyPurchased.setVisibility(View.GONE);
        upgradePurchaseAvailable.setVisibility(View.GONE);
        upgradePurchaseSuccess.setVisibility(View.VISIBLE);
        upgradePurchaseFailed.setVisibility(View.GONE);

        Button closeButton = (Button) findViewById(R.id.upgrade_purchase_success_close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
        getMenuInflater().inflate(R.menu.pro_upgrade, menu);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
