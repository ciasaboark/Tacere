/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentActivity;
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
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.converter.DateConverter;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.database.SimpleCalendarEvent;
import org.ciasaboark.tacere.manager.ActiveEventManager;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

import java.util.Calendar;
import java.util.GregorianCalendar;

//import android.graphics.Outline;

public class MainActivity extends FragmentActivity implements OnItemClickListener, OnItemLongClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";
    private EventCursorAdapter cursorAdapter;
    private Cursor cursor;
    private DatabaseInterface databaseInterface;
    private ListView eventListview = null;
    private int listViewIndex = 0;
    private Prefs prefs;
    private long animationDuration = 300;
//    private Outline outline;


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
        ShowUpdatesActivity.showUpdatesDialogIfNeeded(this);

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

        quickSilenceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quickSilenceImageButton.setEnabled(false);
                fadeImageButtonsInAndOut(cancelQuickSilenceButton, quickSilenceImageButton);
                startQuicksilence();
                quickSilenceImageButton.setEnabled(true);
            }
        });

        cancelQuickSilenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelQuickSilenceButton.setEnabled(false);
                fadeImageButtonsInAndOut(quickSilenceImageButton, cancelQuickSilenceButton);
                stopOngoingQuicksilence();
                cancelQuickSilenceButton.setEnabled(true);
            }
        });
    }

    private void fadeImageButtonsInAndOut(final ImageButton fadeInImageButton, final ImageButton fadeOutImageButton) {
        fadeInImageButton.setAlpha(0f);
        fadeOutImageButton.setAlpha(1f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(fadeOutImageButton, "alpha", 1f, 0f);
        animator.setDuration(animationDuration);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                fadeOutImageButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                fadeOutImageButton.setVisibility(View.GONE);
                fadeOutImageButton.setAlpha(1f);
                flipIn(fadeInImageButton);
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
        animator.setDuration(animationDuration);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                button.setAlpha(0f);
                button.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
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
        eventListview.setFadingEdgeLength(32);
        eventListview.setBackgroundColor(getResources().getColor(R.color.event_list_future_background));
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void drawError() {
        LinearLayout errorBox = (LinearLayout) findViewById(R.id.error_box);
        ImageView calendarIcon = (ImageView) findViewById(R.id.calendar_icon);
        Drawable d = calendarIcon.getBackground();
        Drawable mutable = d.mutate();
        mutable.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
        int apiLevelAvailable = android.os.Build.VERSION.SDK_INT;
        if (apiLevelAvailable >= 16) {
            calendarIcon.setBackground(mutable);
        } else {
            calendarIcon.setBackgroundDrawable(mutable);
        }
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

    private int getBestRingerForInstance(SimpleCalendarEvent event) {
        int bestRinger = prefs.getRingerType();
        int eventRinger = prefs.getRingerForEventSeries(event.getEventId());

        if (eventRinger != SimpleCalendarEvent.RINGER.UNDEFINED) {
            bestRinger = eventRinger;
        }
        if (event.getRingerType() != SimpleCalendarEvent.RINGER.UNDEFINED) {
            bestRinger = event.getRingerType();
        }
        return bestRinger;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            SimpleCalendarEvent thisEvent = databaseInterface.getEvent((int) id);
            int nextRingerType = thisEvent.getRingerType() + 1;
            if (nextRingerType > SimpleCalendarEvent.RINGER.SILENT) {
                nextRingerType = SimpleCalendarEvent.RINGER.NORMAL;
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
        anim.setDuration(animationDuration);
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
        try {
            SimpleCalendarEvent event = databaseInterface.getEvent((int) id);

            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            EventDetailsFragment dialogFragment = EventDetailsFragment.newInstance(event.getId());
            dialogFragment.show(fm, "foo");

        } catch (NoSuchEventException e) {
            Log.d(TAG, "unable to find event with id " + id);
        }
        restartEventSilencerService();

        return true;
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
                Intent addEventIntent = new Intent(Intent.ACTION_INSERT,
                        CalendarContract.Events.CONTENT_URI);
                addEventIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
                SimpleCalendarEvent thisEvent = databaseInterface.getEvent(id);
                // a text view to show the event title
                TextView descriptionTV = (TextView) view.findViewById(R.id.eventText);
                if (descriptionTV != null) {
                    descriptionTV.setText(thisEvent.getTitle());
                }

                // a text view to show the event date span
                String begin = thisEvent.getLocalBeginDate();
                String end = thisEvent.getLocalEndDate();
                String date;
                if (begin.equals(end)) {
                    date = begin;
                } else {
                    date = begin + " - " + end;
                }
                TextView dateTV = (TextView) view.findViewById(R.id.eventDate);
                if (dateTV != null) {
                    dateTV.setText(date);
                }

                // a text view to show the beginning and ending times for the event
                TextView timeTV = (TextView) view.findViewById(R.id.eventTime);
                if (timeTV != null) {
                    StringBuilder timeSB = new StringBuilder(thisEvent.getLocalBeginTime() + " - "
                            + thisEvent.getLocalEndTime());

                    if (thisEvent.isAllDay()) {
                        timeSB = new StringBuilder(getBaseContext().getString(R.string.all_day));
                    }
                    timeTV.setText(timeSB.toString());
                }

                ImageView sidebar = (ImageView) view.findViewById(R.id.event_sidebar);
                Drawable coloredSidebar = (Drawable) getResources().getDrawable(R.drawable.sidebar);
                int displayColor = thisEvent.getDisplayColor();
                coloredSidebar.mutate().setColorFilter(displayColor, Mode.MULTIPLY);
                sidebar.setBackgroundDrawable(coloredSidebar);

                // an image button to show the ringer state for this event
                ImageView eventIV = (ImageView) view.findViewById(R.id.ringerState);
                if (eventIV != null) {
                    eventIV.setImageDrawable(this.getRingerIcon(thisEvent));
                    eventIV.setContentDescription(getBaseContext().getString(
                            R.string.icon_alt_text_normal));
                }

                Calendar calendarDate = new GregorianCalendar();
                calendarDate.set(Calendar.HOUR_OF_DAY, 0);
                calendarDate.set(Calendar.MINUTE, 0);
                calendarDate.set(Calendar.SECOND, 0);
                calendarDate.set(Calendar.MILLISECOND, 0);
                calendarDate.add(Calendar.DAY_OF_MONTH, 1);
                TextView[] textElements = {descriptionTV, dateTV, timeTV};
                int backgroundColor = getResources().getColor(R.color.windowBackground);
                int textColor = getResources().getColor(R.color.textcolor);

                long tomorrowMidnight = calendarDate.getTimeInMillis();
                long eventBegin = thisEvent.getBegin();

                if (ActiveEventManager.isActiveEvent(thisEvent)) {
                    backgroundColor = getResources()
                            .getColor(R.color.event_list_active_event);
                } else if (eventBegin >= tomorrowMidnight) {
                    backgroundColor = getResources()
                            .getColor(R.color.event_list_future_background);
                    textColor = getResources().getColor(R.color.event_list_future_text);
                }

                view.setBackgroundColor(backgroundColor);
                for (TextView v : textElements) {
                    v.setTextColor(textColor);
                }

            } catch (NoSuchEventException e) {
                Log.w(TAG, "unable to get calendar event to build listview: " + e.getMessage());
            }
        }

        private Drawable getRingerIcon(SimpleCalendarEvent event) {
            Drawable icon;
            int bestRinger = getBestRingerForInstance(event);
            int defaultColor = getResources().getColor(R.color.icon_accent);
            int primaryColor = getResources().getColor(R.color.primary);
            int color;
            if (eventHasCustomRinger(event)) {
                color = primaryColor;
            } else {
                color = defaultColor;
            }

            switch (bestRinger) {
                case SimpleCalendarEvent.RINGER.NORMAL:
                    icon = getResources().getDrawable(R.drawable.ic_state_normal);
                    break;
                case SimpleCalendarEvent.RINGER.VIBRATE:
                    icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                    break;
                case SimpleCalendarEvent.RINGER.SILENT:
                    icon = getResources().getDrawable(R.drawable.ic_state_silent);
                    break;
                case SimpleCalendarEvent.RINGER.IGNORE:
                    icon = getResources().getDrawable(R.drawable.blank);
                    break;
                default:
                    icon = getResources().getDrawable(R.drawable.normal);
            }
            icon.mutate().setColorFilter(color, Mode.MULTIPLY);
            return icon;
        }

        private boolean eventHasCustomRinger(SimpleCalendarEvent event) {
            boolean hasCustomRinger = false;
            if (event.getRingerType() != SimpleCalendarEvent.RINGER.UNDEFINED) {
                hasCustomRinger = true;
            } else if (prefs.getRingerForEventSeries(event.getEventId()) != SimpleCalendarEvent.RINGER.UNDEFINED) {
                hasCustomRinger = true;
            }

            return hasCustomRinger;
        }

        private boolean shouldEventSilence(SimpleCalendarEvent event) {
            boolean eventMatches = false;

            // if the event is marked as busy (but is not an all day event)
            // + then we need no further tests
            boolean busy_notAllDay = false;
            if (!event.isFreeTime() && !event.isAllDay()) {
                busy_notAllDay = true;
            }

            // all day events
            boolean allDay = false;
            if (prefs.getSilenceAllDayEvents() && event.isAllDay()) {
                allDay = true;
            }

            // events marked as 'free' or 'available'
            boolean free_notAllDay = false;
            if (prefs.getSilenceFreeTimeEvents() && event.isFreeTime() && !event.isAllDay()) {
                free_notAllDay = true;
            }

            // events with a custom ringer set should always use that ringer
            boolean isCustomRingerSet = false;
            if (event.getRingerType() != SimpleCalendarEvent.RINGER.UNDEFINED) {
                isCustomRingerSet = true;
            }

            if (busy_notAllDay || allDay || free_notAllDay || isCustomRingerSet) {
                eventMatches = true;
            }

            //all of this is negated if the event has been marked to be ignored
            if (event.getRingerType() == SimpleCalendarEvent.RINGER.IGNORE) {
                eventMatches = false;
            }

            return eventMatches;
        }
    }
}
