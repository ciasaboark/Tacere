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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.EventDetailsFragment;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.EventCursorAdapter;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

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
    private DataSetManager dataSetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseInterface = DatabaseInterface.getInstance(this);
        prefs = new Prefs(this);
        dataSetManager = new DataSetManager(this, this);


        BroadcastReceiver datasetChangedReceiver = new BroadcastReceiver() {
            private static final String TAG = "datasetChangedReceiver";

            @Override
            public void onReceive(Context context, Intent intent) {
                String source = intent.getStringExtra(DataSetManager.SOURCE_KEY);
                Log.d(TAG, "got a notification from the service on behalf of " + source);

                //save the current position of the listview
                int index = eventListview.getFirstVisiblePosition();
                View v = eventListview.getChildAt(0);
                int top = (v == null) ? 0 : v.getTop();

                cursor = databaseInterface.getEventCursor();
                //TODO this should properly go from populated listView to an error message, but might
                //not go from error message back to populated listView
                if (cursor.getCount() == 0) {
                    drawEventListOrError();
                } else {
                    cursorAdapter.changeCursor(cursor);

                    long rowChanged = intent.getLongExtra(DataSetManager.ROW_CHANGED, -1);
                    if (rowChanged != -1) {
                        //a specific row has been updated, check that this row is visible, and, if so,
                        //update only that row
                        int start = eventListview.getFirstVisiblePosition();
                        for (int i = start, j = eventListview.getLastVisiblePosition(); i <= j; i++)
                            if (rowChanged == eventListview.getItemIdAtPosition(i)) {
                                View view = eventListview.getChildAt(i - start);
                                eventListview.getAdapter().getView(i, view, eventListview);
                                break;
                            }
                    } else {
                        //the notification does not specify a row, so update everything just to be sure
                        cursorAdapter.notifyDataSetChanged();
                    }

                    //restore the last position of the list view
                    eventListview.setSelectionFromTop(index, top);

                    //redraw the widgets
                    setupAndDrawActionButtons();

                    //we might be comming from an empty database to one that has entries, or vice versa
                    drawEventListOrError();

                    //if this broadcast message came from the anywhere besides the event silencer service
                    //or the database interface then the service needs to be restarted
                    boolean originIsNotService = !TextUtils.equals(
                            EventSilencerService.class.getName(), source);
                    boolean originIsNotInterface = !TextUtils.equals(
                            DatabaseInterface.class.getName(), source);
                    if (originIsNotService && originIsNotInterface) {
                        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
                        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
                    }
                }
            }
        };

        // register to receive broadcast messages
        LocalBroadcastManager.getInstance(this).registerReceiver(datasetChangedReceiver,
                new IntentFilter(DataSetManager.BROADCAST_MESSAGE_KEY));

        // start the background service
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);

        // display the updates dialog if it hasn't been shown yet
        ShowUpdatesActivity.showUpdatesDialogIfNeeded(this);

        // display the "thank you" dialog once if the donation key is installed
        DonationActivity.showDonationDialogIfNeeded(this);

        showFirstRunWizardIfNeeded();
    }

    private void drawEventListOrError() {
        setupListView();
        setupErrorMessage();

        if (databaseInterface.isDatabaseEmpty()) {
            hideEventList();
            drawError();
        } else {
            hideError();
            drawEventList();
        }
    }

    private void setupAndDrawActionButtons() {
        setupActionButtons();
        drawActionButton();
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
        eventListview.setFadingEdgeLength(32);
        eventListview.setBackgroundColor(getResources().getColor(R.color.event_list_item_future_background));
        cursor = databaseInterface.getEventCursor();
        cursorAdapter = new EventCursorAdapter(this, cursor);
        eventListview.setAdapter(cursorAdapter);
        eventListview.setOnItemClickListener(this);
        eventListview.setOnItemLongClickListener(this);
    }

    private void setupErrorMessage() {
        TextView noEventsTv = (TextView) findViewById(R.id.event_list_error);
        String errorText = "";

        //TODO move to members?
        boolean shouldAllCalendarsBeSynced = prefs.shouldAllCalendarsBeSynced();
        boolean selectedCalendarsIsEmpty = prefs.getSelectedCalendarsIds().isEmpty();

        if (!shouldAllCalendarsBeSynced && selectedCalendarsIsEmpty) {
            errorText = getString(R.string.list_error_no_calendars);
        } else if ((prefs.shouldAllCalendarsBeSynced() || !prefs.getSelectedCalendarsIds().isEmpty()) && databaseInterface.isDatabaseEmpty()) {
            errorText = String.format(getString(R.string.main_error_no_events), prefs.getLookaheadDays().string);
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
                if (ssManager.isQuicksilenceActive()) {
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

        if (ssManager.isQuicksilenceActive()) {
            drawStopQuicksilenceActionButton();
        } else {
            drawStartQuicksilenceActionButton();
        }
    }

    private void drawServiceWarningBoxIfNeeded() {
        LinearLayout warningBox = (LinearLayout) findViewById(R.id.main_service_warning);
        if (!prefs.isServiceActivated()) {
            warningBox.setVisibility(View.VISIBLE);
        } else {
            warningBox.setVisibility(View.GONE);
        }
    }

    private void stopOngoingQuicksilence() {
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
        alarmManagerWrapper.scheduleCancelQuickSilenceAlarmAt(System.currentTimeMillis());
    }

    private void drawStartQuicksilenceActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.setColorNormal(getResources().getColor(R.color.fab_quicksilence_normal));
        quickSilenceImageButton.setColorPressed(getResources().getColor(R.color.fab_quicksilence_pressed));
        quickSilenceImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_state_silent));
    }

    private void startQuicksilence() {
        // an intent to send to either start or stop a quick silence duration
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
        int duration = 60 * prefs.getQuickSilenceHours() + prefs.getQuicksilenceMinutes();
        alarmManagerWrapper.scheduleImmediateQuicksilenceForDuration(duration);
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
        drawEventListOrError();
        setupAndDrawActionButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_buy_upgrade:
                Intent buyNowActivityIntent = new Intent(this, InAppBillingActivity.class);
                buyNowActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(buyNowActivityIntent);
                return true;
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
            EventInstance thisEvent = databaseInterface.getEvent(id);
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
            AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
            alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
        } catch (NoSuchEventInstanceException e) {
            // if the selected event is no longer in the DB, then we need to remove it from the list
            // view, since this might also be the only item in the list we will use the broadcast
            // receiver to manage the transition
            dataSetManager.broadcastDataSetChangedMessage();
        }
    }

    private void removeListViewEvent(View view) {
        cursor = databaseInterface.getEventCursor();
        cursorAdapter.swapCursor(cursor);
        cursorAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            EventInstance event = databaseInterface.getEvent((int) id);

            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            EventDetailsFragment dialogFragment = EventDetailsFragment.newInstance(event);
            dialogFragment.show(fm, EventDetailsFragment.TAG);
        } catch (NoSuchEventInstanceException e) {
            Log.d(TAG, "unable to find event with id " + id);
        }
        return true;
    }
}