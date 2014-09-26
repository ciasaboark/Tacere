/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.TooltipManager;
import org.ciasaboark.tacere.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan Nelson on 9/26/14.
 */
public class FirstRunWizard {
    private static final String TAG = "FirstRunWizard";
    private final Context context;
    private final Activity parentActivity;
    private final List<ChromeHelpPopup> tooltips = new ArrayList<ChromeHelpPopup>();
    private boolean showingTooltips = false;

    BroadcastReceiver tooltipReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (ChromeHelpPopup tooltip : tooltips) {
                tooltips.remove(tooltip);
                Log.d(TAG, "dismissing tooltip with text:" + tooltip.getText());
                tooltip.dismiss();
            }
            showingTooltips = false;
        }
    };

    public FirstRunWizard(Context context, Activity parentActivity) {
        if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        if (parentActivity == null) {
            throw new IllegalArgumentException("Parent view can not be null");
        }
        this.context = context;
        this.parentActivity = parentActivity;
        LocalBroadcastManager.getInstance(context).registerReceiver(tooltipReceiver,
                new IntentFilter(TooltipManager.BROADCAST_MESSAGE_KEY));
    }

    public void showAllTooltips() {
        //show tooltips for main view widgets once
        if (!showingTooltips) {
            Prefs prefs = new Prefs(context);
            showingTooltips = true;
            showQuicksilenceTooltip();
            showEventTooltip();
            showSettingsTooltip();
            prefs.disableFirstRun();
        }
    }

    private void showQuicksilenceTooltip() {
        View quicksilenceButton = parentActivity.findViewById(R.id.quickSilenceButton);
        if (quicksilenceButton == null) {
            Log.e(TAG, "can not show quicksilence button tooltip");
        } else {
            ChromeHelpPopup quicksilenceTooltip = new ChromeHelpPopup(context, "Toggle quicksilence");
            if (quicksilenceButton != null) {
                quicksilenceTooltip.show(quicksilenceButton);
                quicksilenceTooltip.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        TooltipManager ttm = new TooltipManager(this, context);
                        ttm.broadcastTooltipDismissedMessage();
                    }
                });
                tooltips.add(quicksilenceTooltip);
            }
        }
    }

    private void showEventTooltip() {
        View addEventButton = parentActivity.findViewById(R.id.action_add_event);
        if (addEventButton == null) {
            Log.e(TAG, "can not show add event button tooltip");
        } else {
            ChromeHelpPopup addEventPopup = new ChromeHelpPopup(context, "Add an event");
            addEventPopup.setHighlightColor(context.getResources().getColor(android.R.color.holo_orange_light));
            addEventPopup.show(addEventButton);
            addEventPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    TooltipManager ttm = new TooltipManager(this, context);
                    ttm.broadcastTooltipDismissedMessage();
                }
            });
            tooltips.add(addEventPopup);
        }
    }

    private void showSettingsTooltip() {
        ActionBar actionBar = parentActivity.getActionBar();
        View actionBarView = actionBar.getCustomView();
        View settingsButton = actionBarView.findViewById(R.id.action_settings);
        if (settingsButton == null) {
            Log.e(TAG, "can not show settings button tooltip");
        } else {
            ChromeHelpPopup settingsPopup = new ChromeHelpPopup(context, "Select calendars to sync");
            int color = context.getResources().getColor(android.R.color.holo_red_light);
            settingsPopup.setHighlightColor(color);
            settingsPopup.show(settingsButton);
            settingsPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    TooltipManager ttm = new TooltipManager(this, context);
                    ttm.broadcastTooltipDismissedMessage();
                }
            });
            tooltips.add(settingsPopup);
        }
    }

}
