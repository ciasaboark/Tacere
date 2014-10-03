/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

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
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.EventDetailsFragment;
import org.ciasaboark.tacere.converter.DateConverter;
import org.ciasaboark.tacere.database.Columns;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
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
    private boolean showingTutorial = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseInterface = DatabaseInterface.getInstance(getApplicationContext());
        prefs = new Prefs(this);


        BroadcastReceiver datasetChangedReceiver = new BroadcastReceiver() {
            private static final String TAG = "datasetChangedReceiver";

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

                //if this broadcast message came from the anywhere besided the event silencer service
                //then the service needs to be restarted
                String messageSource = intent.getStringExtra(DataSetManager.SOURCE_KEY);
                if (!EventSilencerService.class.getName().equals(messageSource)) {
                    restartEventSilencerService();
                }
            }
        };

        // register to receive broadcast messages
        LocalBroadcastManager.getInstance(this).registerReceiver(datasetChangedReceiver,
                new IntentFilter(DataSetManager.BROADCAST_MESSAGE_KEY));


        // display the updates dialog if it hasn't been shown yet
        ShowUpdatesActivity.showUpdatesDialogIfNeeded(this);

        // display the "thank you" dialog once if the donation key is installed
        DonationActivity.showDonationDialogIfNeeded(this);
    }

    private void setupAndDrawActionButtons() {
        setupActionButtons();
        drawActionButton();
    }

    /**
     * Restarts the event silencer service
     */
    private void restartEventSilencerService() {
        Intent i = new Intent(this, EventSilencerService.class);
        i.putExtra("type", RequestTypes.ACTIVITY_RESTART);
        startService(i);
    }

    private void setupActionButtons() {
        final FloatingActionButton quickSilenceImageButton = (FloatingActionButton) findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.attachToListView(eventListview);

        quickSilenceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quickSilenceImageButton.setEnabled(false);
                quickSilenceImageButton.hide();
                quickSilenceImageButton.setVisibility(View.GONE);

                ServiceStateManager ssManager = new ServiceStateManager(getApplicationContext());
                if (ssManager.isQuickSilenceActive()) {
                    stopOngoingQuicksilence();
                    drawStartQuicksilenceActionButton();
                } else {
                    startQuicksilence();
                    drawStopQuicksilenceActionButton();
                }

                quickSilenceImageButton.setVisibility(View.VISIBLE);
                quickSilenceImageButton.show();
                quickSilenceImageButton.setEnabled(true);
            }
        });
    }

    private void drawActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) findViewById(R.id.quickSilenceButton);
        ServiceStateManager ssManager = new ServiceStateManager(this);

        if (ssManager.isQuickSilenceActive()) {
            drawStopQuicksilenceActionButton();
        } else {
            drawStartQuicksilenceActionButton();
        }
    }

    private void stopOngoingQuicksilence() {
        Intent i = new Intent(getApplicationContext(), EventSilencerService.class);
        i.putExtra("type", RequestTypes.CANCEL_QUICKSILENCE);
        startService(i);
    }

    private void drawStartQuicksilenceActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.setColorNormal(getResources().getColor(R.color.fab_quicksilence_normal));
        quickSilenceImageButton.setColorPressed(getResources().getColor(R.color.fab_quicksilence_pressed));
        quickSilenceImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_state_silent));
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

    private void drawStopQuicksilenceActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.setColorNormal(getResources().getColor(R.color.fab_stop_normal));
        quickSilenceImageButton.setColorPressed(getResources().getColor(R.color.fab_stop_pressed));
        quickSilenceImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_state_normal));
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

    @Override
    public void onStart() {
        super.onStart();
        setContentView(R.layout.activity_main);
        // start the background service
        restartEventSilencerService();
        drawEventListOrError();
        setupAndDrawActionButtons();
        showFirstRunWizardIfNeeded();
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

    private void showFirstRunWizardIfNeeded() {
        if (prefs.isFirstRun()) {
            prefs.disableFirstRun();
            final ViewTreeObserver viewTreeObserver = getWindow().getDecorView().getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!showingTutorial) {
                        showingTutorial = true;
                        startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
                    }
                    if (viewTreeObserver.isAlive()) {
                        if (Build.VERSION.SDK_INT >= 16) {
                            viewTreeObserver.removeOnGlobalLayoutListener(this);
                        } else {
                            viewTreeObserver.removeGlobalOnLayoutListener(this);
                        }
                    }
                }
            });
        }
    }

    private void setupListView() {
        eventListview = (ListView) findViewById(R.id.eventListView);
        eventListview.setOnItemClickListener(this);
        eventListview.setOnItemLongClickListener(this);
        eventListview.setFadingEdgeLength(32);
        eventListview.setBackgroundColor(getResources().getColor(R.color.event_list_item_future_background));
        cursor = databaseInterface.getEventCursor();
        cursorAdapter = new EventCursorAdapter(this, cursor);
        eventListview.setAdapter(cursorAdapter);
    }

    private void setupErrorMessage() {
        TextView noEventsTv = (TextView) findViewById(R.id.event_list_error);
        int lookaheadDays = prefs.getLookaheadDays();
        DateConverter dateConverter = new DateConverter(lookaheadDays);
        String errorText = "";
        if (!prefs.shouldAllCalendarsBeSynced() && prefs.getSelectedCalendars().isEmpty()) {
            errorText = getString(R.string.list_error_no_calendars);
        } else if ((prefs.shouldAllCalendarsBeSynced() || !prefs.getSelectedCalendars().isEmpty()) && databaseInterface.isDatabaseEmpty()) {
            errorText = String.format(getString(R.string.main_error_no_events), dateConverter.toString());
        }
        noEventsTv.setText(errorText);
    }

    private void hideEventList() {
        ListView listview = (ListView) findViewById(R.id.eventListView);
        listview.setVisibility(View.GONE);

        //also hide the warning box
        LinearLayout warningBox = (LinearLayout) findViewById(R.id.main_service_warning);
        warningBox.setVisibility(View.GONE);
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
        drawServiceWarningBoxIfNeeded();
        ListView lv = (ListView) findViewById(R.id.eventListView);
        lv.setVisibility(View.VISIBLE);
    }

    private void drawServiceWarningBoxIfNeeded() {
        LinearLayout warningBox = (LinearLayout) findViewById(R.id.main_service_warning);
        if (!prefs.isServiceActivated()) {
            warningBox.setVisibility(View.VISIBLE);
        } else {
            warningBox.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            EventInstance thisEvent = databaseInterface.getEvent((int) id);
            //we want the ringer to cycle to the next visible ringer, so if the event does
            //not have a ringer set we jump to the next based on what the event manager provides
            RingerType nextRingerType;
            if (thisEvent.getRingerType() == RingerType.UNDEFINED) {
                EventManager eventManager = new EventManager(this, thisEvent);
                RingerType currentRinger = eventManager.getBestRinger();
                nextRingerType = currentRinger.getNext();
            } else {
                nextRingerType = thisEvent.getRingerType().getNext();
            }

            databaseInterface.setRingerForInstance((int) id, nextRingerType);
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
            EventInstance event = databaseInterface.getEvent((int) id);

            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            EventDetailsFragment dialogFragment = EventDetailsFragment.newInstance(event.getId());
            dialogFragment.show(fm, EventDetailsFragment.TAG);
        } catch (NoSuchEventException e) {
            Log.d(TAG, "unable to find event with id " + id);
        }
        return true;
    }

    private class EventCursorAdapter extends CursorAdapter {
        private final LayoutInflater layoutInflator;
        private EventInstance thisEvent;
        private int lastAnimatedView = -1;

        public EventCursorAdapter(Context ctx, Cursor c) {
            super(ctx, c);
            layoutInflator = LayoutInflater.from(ctx);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup group) {
            return layoutInflator.inflate(R.layout.list_item_event, group, false);
        }


        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            int id = cursor.getInt(cursor.getColumnIndex(Columns._ID));
            try {
                thisEvent = databaseInterface.getEvent(id);
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

                EventManager eventManager = new EventManager(context, thisEvent);

                // an icon to show the ringer state for this event
                ImageView eventIV = (ImageView) view.findViewById(R.id.ringerState);
                if (eventIV != null) {
                    Drawable ringerIcon = getRingerIcon();
                    eventIV.setImageDrawable(ringerIcon);
                    eventIV.setContentDescription(getBaseContext().getString(
                            R.string.icon_alt_text_normal));
                }

                ImageView ringerSourceView = (ImageView) view.findViewById(R.id.ringerSource);
                if (ringerSourceView != null) {

                    Drawable ringerSource = getRingerSourceIcon();
                    ringerSourceView.setImageDrawable(ringerSource);
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
                            .getColor(R.color.event_list_item_active_event);
                } else if (eventBegin >= tomorrowMidnight) {
                    backgroundColor = getResources()
                            .getColor(R.color.event_list_item_future_background);
                    textColor = getResources().getColor(R.color.event_list_item_future_text);
                }

                RelativeLayout listItemContainer = (RelativeLayout) view.findViewById(R.id.list_item_container);
                listItemContainer.setBackgroundColor(backgroundColor);

                for (TextView v : textElements) {
                    v.setTextColor(textColor);
                }

                Animation animation = AnimationUtils.loadAnimation(context, (id > lastAnimatedView) ? R.anim.up_from_bottom : R.anim.down_from_top);
                view.startAnimation(animation);
                lastAnimatedView = id;

            } catch (NoSuchEventException e) {
                Log.w(TAG, "unable to get calendar event to build listview: " + e.getMessage());
            }
        }

        private Drawable getRingerIcon() {
            Drawable icon;
            int defaultColor = getResources().getColor(R.color.ringer_default);
            int calendarColor = getResources().getColor(R.color.ringer_calendar);
            int eventColor = getResources().getColor(R.color.ringer_series);
            int instanceColor = getResources().getColor(R.color.ringer_instance);
            int color = defaultColor;


            EventManager eventManager = new EventManager(getApplicationContext(), thisEvent);
            RingerType ringerType = eventManager.getBestRinger();
            icon = getIconForRinger(ringerType);

            RingerSource ringerSource = eventManager.getRingerSource();
            switch (ringerSource) {
                case CALENDAR:
                    color = calendarColor;
                    break;
                case EVENT_SERIES:
                    color = eventColor;
                    break;
                case INSTANCE:
                    color = instanceColor;
            }

            icon.mutate().setColorFilter(color, Mode.MULTIPLY);

            if (icon == null) {
                throw new AssertionError(this.getClass().getName() + "Ringer icon should not be null");
            }
            return icon;
        }

        private Drawable getRingerSourceIcon() {
            Drawable icon;
            int iconColor = getResources().getColor(R.color.ringer_default);
            EventManager eventManager = new EventManager(getApplicationContext(), thisEvent);
            RingerSource ringerSource = eventManager.getRingerSource();

            switch (ringerSource) {
                case DEFAULT:
                    icon = getResources().getDrawable(R.drawable.blank);
                    break;
                case CALENDAR:
                    icon = getResources().getDrawable(R.drawable.calendar_calendar);
                    break;
                case EVENT_SERIES:
                    icon = getResources().getDrawable(R.drawable.calendar_series);
                    break;
                case INSTANCE:
                    icon = getResources().getDrawable(R.drawable.calendar_instance);
                    break;
                default:
                    icon = getResources().getDrawable(R.drawable.blank);
            }
            icon.mutate().setColorFilter(iconColor, Mode.MULTIPLY);

            return icon;
        }


        private Drawable getIconForRinger(RingerType ringerType) {
            Drawable icon;
            if (ringerType == null) {
                icon = getResources().getDrawable(R.drawable.blank);
            } else {
                switch (ringerType) {
                    case NORMAL:
                        icon = getResources().getDrawable(R.drawable.ic_state_normal);
                        break;
                    case VIBRATE:
                        icon = getResources().getDrawable(R.drawable.ic_state_vibrate);
                        break;
                    case SILENT:
                        icon = getResources().getDrawable(R.drawable.ic_state_silent);
                        break;
                    case IGNORE:
                        icon = getResources().getDrawable(R.drawable.ic_state_ignore);
                        break;
                    default:
                        icon = getResources().getDrawable(R.drawable.blank);
                }
            }

            return icon;
        }
    }
}