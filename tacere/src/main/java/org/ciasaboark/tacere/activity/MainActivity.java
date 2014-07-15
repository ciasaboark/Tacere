/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
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
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.converter.DateConverter;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.manager.ActiveEventManager;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import java.util.Calendar;
import java.util.GregorianCalendar;


//import android.content.SharedPreferences;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";
    private EventCursorAdapter cursorAdapter;
    private Cursor cursor;
    private DatabaseInterface databaseInterface;
    private ListView eventListview = null;
    private int listViewIndex = 0;
    private Prefs prefs;
    private Outline outline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseInterface = DatabaseInterface.getInstance(getApplicationContext());
        prefs = new Prefs(this);
        BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            private static final String TAG = "messageReceiver";

            @Override
            public void onReceive(Context context, Intent intent) {
                //save the current position of the listview
                Parcelable listviewState = eventListview.onSaveInstanceState();

                Log.d(TAG, "got a notification from the service, updating adpater and views");
                cursor = databaseInterface.getEventCursor();
                cursorAdapter.changeCursor(cursor);
                cursorAdapter.notifyDataSetChanged();

                //restore the last position of the list view
                eventListview.onRestoreInstanceState(listviewState);

                //redraw the widgets
                setupAndDrawActionButtons();
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

    private void setupAndDrawActionButtons() {
        setupActionButtons();
        drawActionButton();
    }

    private void setupActionButtons() {
        final ImageButton quickSilenceImageButton = (ImageButton) findViewById(R.id.quickSilenceButton);
        final ImageButton cancelQuickSilenceButton = (ImageButton) findViewById(R.id.cancel_quickSilenceButton);
        quickSilenceImageButton.setVisibility(View.GONE);
        cancelQuickSilenceButton.setVisibility(View.GONE);

        //Draw a round outline on the buttons
        int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
        outline = new Outline();
        outline.setOval(0, 0, size, size);
        quickSilenceImageButton.setOutline(outline);
        cancelQuickSilenceButton.setOutline(outline);

        quickSilenceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quickSilenceImageButton.setEnabled(false);
                flipInAndOut(cancelQuickSilenceButton, quickSilenceImageButton);
                startQuicksilence();
                quickSilenceImageButton.setEnabled(true);
            }
        });

        cancelQuickSilenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelQuickSilenceButton.setEnabled(false);
                flipInAndOut(quickSilenceImageButton, cancelQuickSilenceButton);
                stopOngoingQuicksilence();
                cancelQuickSilenceButton.setEnabled(true);
            }
        });
    }

    private void flipInAndOut(final ImageButton flipInImageButton, final ImageButton flipOutImageButton) {
        flipInImageButton.setAlpha(0f);
        flipOutImageButton.setAlpha(1f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(flipOutImageButton, "alpha", 1f, 0f);
        animator.setDuration(1000);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
//                flipOutImageButton.setOutline(null);
                flipOutImageButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                flipOutImageButton.setVisibility(View.GONE);
//                flipOutImageButton.setOutline(outline);
                flipOutImageButton.setAlpha(1f);
                flipIn(flipInImageButton);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }


    private void flipIn(final ImageButton button) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(button, "alpha", 0f, 1f);
        animator.setDuration(1000);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                button.setAlpha(0f);
                button.setVisibility(View.VISIBLE);
//                button.setOutline(null);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
//                button.setOutline(outline);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();


    }

    private void drawActionButton() {
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
    }

//    private void drawQuicksilenceButton() {
//        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
//        if (apiLevelAvailable >= 20) { //TODO this should be VERSION_NAMES.L but the preview reports its version as 20 for now
//            drawNewQuicksilenceButton();
//        } else {
//            drawCompatQuicksilenceButton();
//        }
//    }

//    private void drawCompatQuicksilenceButton() {
//        Button quicksilenceButton = (Button) findViewById(R.id.quicksilenceButton_compat);
//        quicksilenceButton.setEnabled(false);
//        ServiceStateManager ssM = new ServiceStateManager(this);
//        if (ssM.isQuickSilenceActive()) {
//            quicksilenceButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    startQuicksilence();
//                }
//            });
//            quicksilenceButton.setText(R.string.startQuicksilence);
//        } else {
//            quicksilenceButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    stopOngoingQuicksilence();
//                }
//            });
//            quicksilenceButton.setText(R.string.cancelQuicksilence);
//        }
//        quicksilenceButton.setEnabled(true);
//        quicksilenceButton.setVisibility(View.VISIBLE);
//    }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        setContentView(R.layout.activity_main);
        // start the background service
        restartEventSilencerService();
        setupAndDrawActionButtons();
        drawEventListOrError();
    }

    @Override
    public void onPause() {
        super.onPause();
        listViewIndex = eventListview.getFirstVisiblePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        eventListview.setSelectionFromTop(listViewIndex, 0);
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

        if (databaseInterface.isDatabaseEmpty()) {
            hideEventList();
            drawError();
        } else {
            hideError();
            drawEventList();
        }
    }

    private void setupListView() {
        eventListview = (ListView) findViewById(R.id.eventListView);
        eventListview.setOnItemClickListener(this);
        eventListview.setOnItemLongClickListener(this);
        eventListview.setFadingEdgeLength(0);
        cursor = databaseInterface.getEventCursor();
        cursorAdapter = new EventCursorAdapter(this, cursor);
        eventListview.setAdapter(cursorAdapter);
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
        LinearLayout errorBox = (LinearLayout) findViewById(R.id.error_box);
        errorBox.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        LinearLayout errorBox = (LinearLayout) findViewById(R.id.error_box);
        errorBox.setVisibility(View.GONE);
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
            eventListview.getAdapter().getView(position, view, eventListview);

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
                cursor = databaseInterface.getEventCursor();
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
            eventListview.getAdapter().getView(position, view, eventListview);
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
                eventIV.setImageDrawable(this.getEventIcon(thisEvent));
                eventIV.setContentDescription(getBaseContext().getString(
                        R.string.icon_alt_text_normal));

                Calendar calendarDate = new GregorianCalendar();
                calendarDate.set(Calendar.HOUR_OF_DAY, 0);
                calendarDate.set(Calendar.MINUTE, 0);
                calendarDate.set(Calendar.SECOND, 0);
                calendarDate.set(Calendar.MILLISECOND, 0);
                calendarDate.add(Calendar.DAY_OF_MONTH, 1);

                long tomorrowMidnight = calendarDate.getTimeInMillis();
                long eventBegin = thisEvent.getBegin();

                if (ActiveEventManager.isActiveEvent(thisEvent)) {
                    view.setBackgroundColor(getResources()
                            .getColor(R.color.event_list_active_event));
                } else if (eventBegin > tomorrowMidnight) {
                    view.setBackgroundColor(getResources()
                            .getColor(R.color.event_list_dark_background));
                } else {
                    view.setBackgroundColor(getResources()
                            .getColor(R.color.windowBackground));
                }
            } catch (NoSuchEventException e) {
                Log.w(TAG, "unable to get calendar event to build listview: " + e.getMessage());
            }
        }

        private Drawable getRingerIcon(int ringerType, Integer color) {
            Drawable icon;

            switch (ringerType) {
                case CalEvent.RINGER.NORMAL:
                    icon = getResources().getDrawable(R.drawable.ringer_on);
                    break;
                case CalEvent.RINGER.SILENT:
                    icon = getResources().getDrawable(R.drawable.do_not_disturb);
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

        private Drawable getEventIcon(CalEvent event) {
            Drawable icon;
            int defaultColor = getResources().getColor(R.color.icon_accent);

            if (eventShouldSilence(event)) {
                if (event.getRingerType() != CalEvent.RINGER.UNDEFINED) {
                    // a custom ringer has been applied
                    icon = getRingerIcon(event.getRingerType(), getResources().getColor(R.color.primary));
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
