/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 * 
 * Released under the BSD license. For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Outline;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.converter.DateConverter;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;


//import android.content.SharedPreferences;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";
    private Context ctx;

    private EventCursorAdapter cursorAdapter;
    private Cursor cursor;
    private DatabaseInterface databaseInterface;
    private ListView lv = null;
    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_main);
        databaseInterface = DatabaseInterface.getInstance(this);
        prefs = new Prefs(this);
        BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            private static final String TAG = "messageReceiver";

            @Override
            public void onReceive(Context context, Intent intent) {
                cursorAdapter.notifyDataSetChanged();
                Log.d(TAG, "got a notification from the service, updating adpater and views");
                cursor = databaseInterface.getCursor(Columns.BEGIN);
                cursorAdapter = new EventCursorAdapter(ctx, cursor);
                lv.setAdapter(cursorAdapter);
                lv.invalidateViews(); //TODO test that this forces the listview to redraw

                //redraw the widgets
                drawQuicksilenceButton();
            }
        };

        // register to receive broadcast messages
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("custom-event-name"));

        // display the updates dialog if it hasn't been shown yet
        UpdatesActivity.showUpdatesDialogIfNeeded(this);

        // display the "thank you" dialog once if the donation key is installed
        DonationActivity.showDonationDialogIfNeeded(this);
    }

    private void drawQuicksilenceButton() {
        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
        if (apiLevelAvailable >= 20) { //TODO this should be VERSION_NAMES.L but the preview reports its version as 20 for now
            drawNewQuicksilenceButton();
        } else {
            drawCompatQuicksilenceButton();
        }
    }

    @TargetApi(Build.VERSION_CODES.L)
    private void drawNewQuicksilenceButton() {
        ImageButton quickSilenceImageButton = (ImageButton) findViewById(R.id.quickSilenceButton);
        ImageButton cancelQuickSilenceButton = (ImageButton) findViewById(R.id.cancel_quickSilenceButton);


        ServiceStateManager ssManager = new ServiceStateManager(this);

        if (ssManager.isQuickSilenceActive()) {
            quickSilenceImageButton.setVisibility(View.GONE);
            cancelQuickSilenceButton.setVisibility(View.VISIBLE);
        } else {
            quickSilenceImageButton.setVisibility(View.VISIBLE);
            cancelQuickSilenceButton.setVisibility(View.GONE);
        }


        //Draw a round outline on the button
        //TODO should this be done on older styles as well?  It might be better to use the normal
        //rectangular button instead
        int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
        Outline outline = new Outline();
        outline.setOval(0, 0, size, size);
        quickSilenceImageButton.setOutline(outline);
        cancelQuickSilenceButton.setOutline(outline);

        //only the quicksilence button should be hidden, we may still need to be able to cancel
        //an ongoing quicksilence duration
        if (prefs.getQuickSilenceHours() == 0 && prefs.getQuicksilenceMinutes() == 0) {
            quickSilenceImageButton.setEnabled(false);
            quickSilenceImageButton.setVisibility(View.GONE);
        } else {
            quickSilenceImageButton.setEnabled(true);
            quickSilenceImageButton.setVisibility(View.VISIBLE);
        }

        quickSilenceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuicksilence();
            }
        });

        cancelQuickSilenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopOngoingQuicksilence();
            }
        });
        quickSilenceImageButton.invalidate();
        cancelQuickSilenceButton.invalidate();
    }

    private void drawCompatQuicksilenceButton() {
        Button quicksilenceButton = (Button) findViewById(R.id.quicksilenceButton_compat);
        quicksilenceButton.setEnabled(false);
        ServiceStateManager ssM = new ServiceStateManager(this);
        if (ssM.isQuickSilenceActive()) {
            quicksilenceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startQuicksilence();
                }
            });
            quicksilenceButton.setText(R.string.startQuicksilence);
        } else {
            quicksilenceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopOngoingQuicksilence();
                }
            });
            quicksilenceButton.setText(R.string.cancelQuicksilence);
        }
        quicksilenceButton.setEnabled(true);
        quicksilenceButton.setVisibility(View.VISIBLE);
    }

    private void startQuicksilence() {
        // an intent to send to either start or stop a quick silence duration
        Intent i = new Intent(getApplicationContext(), EventSilencerService.class);
        i.putExtra("type", RequestTypes.QUICKSILENCE);
        // the length of time for the pollService to sleep in minutes
        int duration = 60 * prefs.getQuickSilenceHours() + prefs.getQuicksilenceMinutes();
        i.putExtra("duration", duration);
        startService(i);
    }

    private void stopOngoingQuicksilence() {
        Intent i = new Intent(getApplicationContext(), EventSilencerService.class);
        i.putExtra("type", RequestTypes.CANCEL_QUICKSILENCE);
        startService(i);
    }

    private void crossfade(final ImageButton fadeOut, final ImageButton fadeIn) {
        int mShortAnimationDuration = 500;
        fadeOut.setEnabled(false);
        fadeIn.setEnabled(false);

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        fadeIn.setAlpha(0f);
        fadeIn.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        fadeIn.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        fadeOut.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fadeOut.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onStart() {
        super.onStart();
        setContentView(R.layout.activity_main);
        // start the background service
        restartEventSilencerService();
        drawQuicksilenceButton();
        drawEventListOrError();


    }

    /**
     * Restarts the event silencer service
     */
    private void restartEventSilencerService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    private void drawEventListOrError() {
        setupListView();
        setupErrorMessage();

        databaseInterface.update(prefs.getLookaheadDays());

        // prune the database of old events
        databaseInterface.pruneEventsBefore(System.currentTimeMillis() - 1000 * 60 * (long) prefs.getBufferMinutes());

        // since the number of days to display can change we need to
        // + remove events beyond the lookahead period
        databaseInterface.pruneEventsAfter(System.currentTimeMillis() + 1000 * 60 * 60 * 24
                * (long) prefs.getLookaheadDays());


        if (databaseInterface.isDatabaseEmpty()) {
            hideEventList();
            drawError();
        } else {
            hideError();
            drawEventList();
        }
    }

    private void setupListView() {
        lv = (ListView) findViewById(R.id.eventListView);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
        lv.setFadingEdgeLength(0);
        cursor = databaseInterface.getCursor(Columns.BEGIN);
        cursorAdapter = new EventCursorAdapter(this, cursor);
        lv.setAdapter(cursorAdapter);
    }

    private void setupErrorMessage() {
        TextView noEventsTv = (TextView) findViewById(R.id.event_list_error);
        int lookaheadDays = prefs.getLookaheadDays();
        DateConverter dateConverter = new DateConverter(lookaheadDays);

        String errorText = String.format(getString(R.string.no_events), dateConverter.toString());
        noEventsTv.setText(errorText);
    }

    private void hideEventList() {
        ListView listview = (ListView) findViewById(R.id.eventListView);
        listview.setVisibility(View.GONE);
    }

    private void drawError() {
        TextView tv = (TextView) findViewById(R.id.event_list_error);
        tv.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        TextView tv = (TextView) findViewById(R.id.event_list_error);
        tv.setVisibility(View.GONE);
    }

    private void drawEventList() {
        ListView lv = (ListView) findViewById(R.id.eventListView);
        lv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            CalEvent thisEvent = databaseInterface.getEvent((int) id);
            int nextRingerType = thisEvent.getRingerType() + 1;
            if (nextRingerType > CalEvent.RINGER.SILENT) {
                nextRingerType = CalEvent.RINGER.NORMAL;
            }
            databaseInterface.setRingerType((int) id, nextRingerType);
            lv.getAdapter().getView(position, view, lv);

            // since the database has changed we need to wake the service
            restartEventSilencerService();
        } catch (NullPointerException e) {
            // if the selected event is no longer in the DB, then we need to remove it from the list
            // view
            removeListViewEvent(view);
        } catch (NoSuchEventException e) {
            removeListViewEvent(view);
        }
    }

    private void removeListViewEvent(View view) {
        Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        anim.setDuration(500);
        view.startAnimation(anim);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                cursor = databaseInterface.getCursor(Columns.BEGIN);
                cursorAdapter.swapCursor(cursor);
                cursorAdapter.notifyDataSetChanged();
            }
        }, anim.getDuration());
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        boolean result = false;

        try {
            CalEvent thisEvent = databaseInterface.getEvent((int) id);
            databaseInterface.setRingerType((int) id, CalEvent.RINGER.UNDEFINED);
            lv.getAdapter().getView(position, view, lv);
            Toast.makeText(parent.getContext(), thisEvent.getTitle() + " reset to default ringer",
                    Toast.LENGTH_SHORT).show();

            // since the database has changed we need to wake the service
            restartEventSilencerService();
            result = true;
        } catch (NullPointerException e) {
            removeListViewEvent(view);
        } catch (NoSuchEventException e) {
            removeListViewEvent(view);
        }
        return result;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // app icon in action bar clicked; go home
                Intent settingsActivityIntent = new Intent(this, SettingsActivity.class);
                settingsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsActivityIntent);
                return true;
            case R.id.action_about:
                Intent aboutActivityIntent = new Intent(this, AboutActivity.class);
                aboutActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(aboutActivityIntent);
                return true;
            case R.id.action_add_event:
                Uri.Builder uriBuilder = CalendarContract.Events.CONTENT_URI.buildUpon();
                Intent addEventIntent = new Intent(Intent.ACTION_INSERT, uriBuilder.build());
                startActivity(addEventIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class EventCursorAdapter extends CursorAdapter {
        private final LayoutInflater layoutInflator;

        @SuppressWarnings("deprecation")
        public EventCursorAdapter(Context ctx, Cursor c) {
            super(ctx, c);
            layoutInflator = LayoutInflater.from(ctx);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup group) {
            return layoutInflator.inflate(R.layout.event_list_item, group, false);
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
            try {
                CalEvent thisEvent = databaseInterface.getEvent(id);
                // a text view to show the event title
                TextView descriptionTV = (TextView) view.findViewById(R.id.eventText);
                descriptionTV.setText(thisEvent.getTitle());

                // a text view to show the event date span
                TextView dateTV = (TextView) view.findViewById(R.id.eventDate);
                String begin = thisEvent.getLocalBeginDate();
                String end = thisEvent.getLocalEndDate();
                String date;
                if (begin.equals(end)) {
                    date = begin;
                } else {
                    date = begin + " - " + end;
                }
                dateTV.setText(date);

                // a text view to show the beginning and ending times for the event
                TextView timeTV = (TextView) view.findViewById(R.id.eventTime);
                StringBuilder timeSB = new StringBuilder(thisEvent.getLocalBeginTime() + " - "
                        + thisEvent.getLocalEndTime());

                if (thisEvent.isAllDay()) {
                    timeSB = new StringBuilder(getBaseContext().getString(R.string.all_day));
                }
                timeTV.setText(timeSB.toString());

                // a color box to match the calendar color
                RelativeLayout calColorBox = (RelativeLayout) view.findViewById(R.id.calendarColor);
                calColorBox.setBackgroundColor(thisEvent.getDisplayColor());

                // an image button to show the ringer state for this event
                ImageView eventIV = (ImageView) view.findViewById(R.id.ringerState);
                eventIV.setImageDrawable(this.getEventIcon(thisEvent, context));
                eventIV.setContentDescription(getBaseContext().getString(
                        R.string.icon_alt_text_normal));

                Animation iconAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
                iconAnim.setDuration(500);
                eventIV.startAnimation(iconAnim);

                // TODO find out how to animate the list items only when first displayed, this
                // animation will fire every time the event view is replaced
                /*
                 * Animation viewAnim = AnimationUtils.loadAnimation(context,
				 * android.R.anim.slide_in_left); viewAnim.setDuration(500);
				 * view.startAnimation(viewAnim);
				 */
            } catch (NoSuchEventException e) {
                Log.w(TAG, "unable to get calendar event to build listview: " + e.getMessage());
            }
        }

        private Drawable getRingerIcon(int ringerType, Integer color) {
            Drawable icon;

            switch (ringerType) {
                case CalEvent.RINGER.NORMAL:
                    icon = getResources().getDrawable(R.drawable.ic_state_normal);
                    break;
                case CalEvent.RINGER.SILENT:
                    icon = getResources().getDrawable(R.drawable.ic_state_silent);
                    break;
                case CalEvent.RINGER.VIBRATE:
                    icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                    break;
                case CalEvent.RINGER.UNDEFINED:
                    int defaultRinger = prefs.getRingerType();
                    icon = getRingerIcon(defaultRinger, null);
                    break;
                case CalEvent.RINGER.IGNORE:
                default:
                    // events that should be ignored are given a blank icon
                    icon = getResources().getDrawable(R.drawable.blank);
            }

            // colorize the icon if a color has been provided
            if (color != null) {
                icon.mutate().setColorFilter(color, Mode.MULTIPLY);
            }

            return icon;
        }

        private boolean eventShouldSilence(CalEvent event) {
            boolean eventShouldSilence = true;
            boolean silenceFreeTime = prefs.getSilenceFreeTimeEvents();
            boolean silenceAllDay = prefs.getSilenceAllDayEvents();

            // if a custom ringer is set then the event should silence, otherwise it depends on the
            // event type and settings
            if (event.getRingerType() == CalEvent.RINGER.UNDEFINED) {
                if ((event.isAllDay() && !silenceAllDay)
                        || (event.isFreeTime() && !silenceFreeTime)) {
                    eventShouldSilence = false;
                }
            }

            return eventShouldSilence;
        }

        private Drawable getEventIcon(CalEvent event, Context ctx) {
            Drawable icon;
            int defaultColor = 0xFFE8E8E8;

            if (eventShouldSilence(event)) {
                if (event.getRingerType() != CalEvent.RINGER.UNDEFINED) {
                    // a custom ringer has been applied
                    icon = getRingerIcon(event.getRingerType(), event.getDisplayColor());
                } else {
                    icon = getRingerIcon(CalEvent.RINGER.UNDEFINED, defaultColor);
                }
            } else {
                icon = getRingerIcon(CalEvent.RINGER.IGNORE, null);
            }

            return icon;
        }

    }

}
