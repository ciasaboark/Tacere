/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.TutorialActivity;
import org.ciasaboark.tacere.adapter.EventCursorAdapter;
import org.ciasaboark.tacere.database.DataSetManager;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.manager.ServiceStateManager;
import org.ciasaboark.tacere.prefs.Prefs;
import org.ciasaboark.tacere.prefs.Updates;
import org.ciasaboark.tacere.service.EventSilencerService;
import org.ciasaboark.tacere.service.RequestTypes;

public class EventsFragment extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "EventsFragment";
    private static final int CALENDAR_PERMISSIONS_REQUEST_CODE = 1;
    private View rootView;
    private Context context;
    private EventCursorAdapter cursorAdapter;
    private Cursor cursor;
    private DatabaseInterface databaseInterface;
    private ListView eventListview = null;
    private int listViewIndex = 0;
    private Prefs prefs;
    private long animationDuration = 300;
    private boolean showingTutorial = false;
    private DataSetManager dataSetManager;

    private void showFirstRunWizardIfNeeded() {
        if (prefs.isFirstRun()) {
            prefs.disableFirstRun();
            final ViewTreeObserver viewTreeObserver = ((Activity) context).getWindow().getDecorView().getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (!showingTutorial) {
                        showingTutorial = true;
                        startActivity(new Intent(context.getApplicationContext(), TutorialActivity.class));
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CALENDAR_PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMainViews();
                } else {
                    //TODO
                    //permissions were denied.
                }
                return;
            }
        }
    }

    @Override
    public void onAttach(Context ctx) {
        super.onAttach(context);
        context = ctx;
        databaseInterface = DatabaseInterface.getInstance(context);
        prefs = new Prefs(context);
        dataSetManager = new DataSetManager(this, context);


        BroadcastReceiver datasetChangedReceiver = new BroadcastReceiver() {
            private static final String TAG = "datasetChangedReceiver";

            @Override
            public void onReceive(Context context, Intent intent) {
                String source = intent.getStringExtra(DataSetManager.SOURCE_KEY);
                Log.d(TAG, "got a notification from the service on behalf of " + source);

                Cursor newCursor = databaseInterface.getEventCursor();
                Cursor oldCursor = cursorAdapter.getCursor();

                //TODO this should properly go from populated listView to an error message, but might
                //not go from error message back to populated listView

                if (oldCursor.getCount() == 0 && newCursor.getCount() != 0) {
                    hideError();
                    drawEventList();
                } else if (oldCursor.getCount() != 0 && newCursor.getCount() == 0) {
                    hideEventList();
                    drawEmptyEventListError();
                }

                //save the old position
                int index = eventListview.getFirstVisiblePosition();
                View v = eventListview.getChildAt(0);
                int top = (v == null) ? 0 : v.getTop();

                cursorAdapter.changeCursor(newCursor);
                if (newCursor.getCount() != 0) {
                    long rowChanged = intent.getLongExtra(DataSetManager.ROW_CHANGED, -1);
                    if (rowChanged == -1) {
                        //the notification does not specify a row, so update everything just to be sure
                        cursorAdapter.notifyDataSetChanged();
                    } else {
                        //a specific row has been updated, check that this row is visible, and, if so,
                        //update only that row
                        int start = eventListview.getFirstVisiblePosition();
                        int curPos = start;
                        int end = eventListview.getLastVisiblePosition();
                        while (curPos <= end) {
                            if (rowChanged == eventListview.getItemIdAtPosition(curPos)) {
                                View view = eventListview.getChildAt(curPos - start);
                                cursorAdapter.getView(curPos, view, eventListview);
                                break;
                            }
                            curPos++;
                        }
                    }

                    //restore the last position of the list view
                    eventListview.setSelectionFromTop(index, top);

                    //redraw the widgets
                    setupAndDrawActionButtons();

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
        LocalBroadcastManager.getInstance(context).registerReceiver(datasetChangedReceiver,
                new IntentFilter(DataSetManager.BROADCAST_MESSAGE_KEY));

        BroadcastReceiver quickSilenceReceiver = new BroadcastReceiver() {
            private static final String TAG = "QuickSilenceReceiver";

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "received quicksilence broadcast notification");
                drawActionButton();
            }
        };

        LocalBroadcastManager.getInstance(context).registerReceiver(quickSilenceReceiver,
                new IntentFilter(EventSilencerService.QUICKSILENCE_BROADCAST_KEY));

        // display the updates dialog if it hasn't been shown yet
        Updates updates = new Updates(context, this);
        updates.showUpdatesDialogIfNeeded();

        showFirstRunWizardIfNeeded();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_events, container, false);
        initMainViews();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        eventListview.setSelectionFromTop(listViewIndex, 0);
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        setContentView(R.layout.activity_main);
//
//    }

    @Override
    public void onPause() {
        super.onPause();
        listViewIndex = eventListview.getFirstVisiblePosition();
    }

    private void initMainViews() {
        drawEventListOrError();
        drawPermissionsWarningBoxIfNeeded();
        setupAndDrawActionButtons();
        // start the background service
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
        final FloatingActionButton quickSilenceImageButton = (FloatingActionButton) rootView.findViewById(R.id.quickSilenceButton);
        if (Build.VERSION.SDK_INT >= 21) {
            quickSilenceImageButton.setVisibility(View.INVISIBLE);
            int cx = (quickSilenceImageButton.getLeft() + quickSilenceImageButton.getRight()) / 2;
            int cy = (quickSilenceImageButton.getTop() + quickSilenceImageButton.getBottom()) / 2;

            // get the final radius for the clipping circle
            int finalRadius = quickSilenceImageButton.getWidth();

            // create and start the animator for this view
            // (the start radius is zero)
            //TODO triggers illegal state exception
            quickSilenceImageButton.setVisibility(View.VISIBLE);
//            Animator anim =
//                    ViewAnimationUtils.createCircularReveal(quickSilenceImageButton, cx, cy, 0, finalRadius);
//            anim.start();
        } else {
            quickSilenceImageButton.setVisibility(View.VISIBLE);
            quickSilenceImageButton.show();
        }
    }

    private void drawEventListOrError() {
        setupListView();
        setupErrorMessage();

        if (!isCalendarAuthenticated()) {
            hideEventList();
            drawPermissionsError();
        } else if (databaseInterface.isDatabaseEmpty()) {
            hideEventList();
            drawEmptyEventListError();
        } else {
            hideError();
            drawEventList();
        }
    }

    private void drawPermissionsWarningBoxIfNeeded() {
        View warningBox = rootView.findViewById(R.id.permissions_error_box);
        if (isCalendarAuthenticated()) {
            warningBox.setVisibility(View.GONE);
        } else {
            warningBox.setVisibility(View.VISIBLE);
            Button permissionsButton = (Button) warningBox.findViewById(R.id.grant_permissions_button);

            permissionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "user requesting permissions change");
//                    if (ActivityCompat.shouldShowRequestPermissionRationale(ctx,
//                            Manifest.permission.READ_CALENDAR)) {
//                    } else {
                    String[] permissions = {Manifest.permission.READ_CALENDAR};
                    ActivityCompat.requestPermissions((Activity) context, permissions, CALENDAR_PERMISSIONS_REQUEST_CODE);
//                    }
                }
            });
        }

    }

    private void setupAndDrawActionButtons() {
        setupActionButtons();
        drawActionButton();
    }

    private void setupListView() {
        eventListview = (ListView) rootView.findViewById(R.id.eventListView);
        eventListview.setFadingEdgeLength(32);
        eventListview.setBackgroundColor(getResources().getColor(R.color.event_list_background));
        cursor = databaseInterface.getEventCursor();
        cursorAdapter = new EventCursorAdapter(context, cursor);
        eventListview.setAdapter(cursorAdapter);
    }

    private void setupErrorMessage() {
        TextView noEventsTv = (TextView) rootView.findViewById(R.id.error_text);
        String errorText = "";

        //TODO move to members?
        boolean shouldAllCalendarsBeSynced = prefs.shouldAllCalendarsBeSynced();
        boolean selectedCalendarsIsEmpty = prefs.getSelectedCalendarsIds().isEmpty();

        if (!shouldAllCalendarsBeSynced && selectedCalendarsIsEmpty) {
            errorText = getString(R.string.list_error_no_calendars);
        } else if ((prefs.shouldAllCalendarsBeSynced() || !prefs.getSelectedCalendarsIds().isEmpty()) && databaseInterface.isDatabaseEmpty()) {
            errorText = String.format(getString(R.string.main_error_no_events), prefs.getLookaheadDays().injectString);
        }
        noEventsTv.setText(errorText);
    }

    private boolean isCalendarAuthenticated() {
        int permissions = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR);
        return permissions == PackageManager.PERMISSION_GRANTED;
    }

    private void hideEventList() {
        ListView listview = (ListView) rootView.findViewById(R.id.eventListView);
        listview.setVisibility(View.GONE);

        //also hide the warning box
        LinearLayout warningBox = (LinearLayout) rootView.findViewById(R.id.main_service_warning);
        warningBox.setVisibility(View.GONE);
    }

    private void drawPermissionsError() {
        showErrorBox();
        setErrorText(R.string.main_error_no_permissions);
    }

    private void drawEmptyEventListError() {
        showErrorBox();
        setErrorText(R.string.main_error_no_events);
    }

    private void hideError() {
        LinearLayout errorBox = (LinearLayout) rootView.findViewById(R.id.error_box);
        errorBox.setVisibility(View.GONE);
    }

    private void drawEventList() {
        drawServiceWarningBoxIfNeeded();
        ListView lv = (ListView) rootView.findViewById(R.id.eventListView);
        lv.setVisibility(View.VISIBLE);
    }

    private void setupActionButtons() {
        final FloatingActionButton quickSilenceImageButton = (FloatingActionButton) rootView.findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.attachToListView(eventListview);
        if (Build.VERSION.SDK_INT < 21) {
            //a shadow will be applied automatically on api versions >= 21, otherwise we have to
            //emulate one here
            quickSilenceImageButton.setShadow(true);
        }

        quickSilenceImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quickSilenceImageButton.setEnabled(false);
                ServiceStateManager ssManager = ServiceStateManager.getInstance(context.getApplicationContext());
                if (ssManager.isQuicksilenceActive()) {
                    stopOngoingQuicksilence();
                    drawStartQuicksilenceActionButton();
                } else {
                    startQuicksilence();
                    drawStopQuicksilenceActionButton();
                }

                quickSilenceImageButton.setEnabled(true);
            }
        });
    }

    private void drawActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) rootView.findViewById(R.id.quickSilenceButton);
        ServiceStateManager ssManager = ServiceStateManager.getInstance(context.getApplicationContext());

        if (ssManager.isQuicksilenceActive()) {
            drawStopQuicksilenceActionButton();
        } else {
            drawStartQuicksilenceActionButton();
        }
    }

    private void showErrorBox() {
        LinearLayout errorBox = (LinearLayout) rootView.findViewById(R.id.error_box);
        errorBox.setVisibility(View.VISIBLE);
    }

    private void setErrorText(int stringRes) {
        TextView errorText = (TextView) rootView.findViewById(R.id.error_text);
        errorText.setText(stringRes);
    }

    private void drawServiceWarningBoxIfNeeded() {
        LinearLayout warningBox = (LinearLayout) rootView.findViewById(R.id.main_service_warning);
        if (!prefs.isServiceActivated()) {
            warningBox.setVisibility(View.VISIBLE);
        } else {
            warningBox.setVisibility(View.GONE);
        }
    }

    private void stopOngoingQuicksilence() {
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
        alarmManagerWrapper.scheduleCancelQuickSilenceAlarmAt(System.currentTimeMillis());
    }

    private void drawStartQuicksilenceActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) rootView.findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.setColorNormal(context.getResources().getColor(R.color.fab_quicksilence_normal));
        quickSilenceImageButton.setColorPressed(context.getResources().getColor(R.color.fab_quicksilence_pressed));
        Drawable quicksilenceDrawable = context.getResources().getDrawable(R.drawable.fab_silent);
        quicksilenceDrawable.mutate().setColorFilter(context.getResources()
                .getColor(R.color.fab_quicksilence_icon_tint), PorterDuff.Mode.MULTIPLY);
        quickSilenceImageButton.setImageDrawable(quicksilenceDrawable);
    }

    private void startQuicksilence() {
        // an intent to send to either start or stop a quick silence duration
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
        int duration = 60 * prefs.getQuickSilenceHours() + prefs.getQuicksilenceMinutes();
        alarmManagerWrapper.scheduleImmediateQuicksilenceForDuration(duration);
    }

    private void drawStopQuicksilenceActionButton() {
        FloatingActionButton quickSilenceImageButton = (FloatingActionButton) rootView.findViewById(R.id.quickSilenceButton);
        quickSilenceImageButton.setColorNormal(context.getResources().getColor(R.color.fab_stop_normal));
        quickSilenceImageButton.setColorPressed(context.getResources().getColor(R.color.fab_stop_pressed));
        Drawable quicksilenceDrawable = context.getResources().getDrawable(R.drawable.fab_normal);
        quicksilenceDrawable.mutate().setColorFilter(context.getResources()
                .getColor(R.color.fab_stop_icon_tint), PorterDuff.Mode.MULTIPLY);
        quickSilenceImageButton.setImageDrawable(quicksilenceDrawable);
    }

//    @Override
//    public boolean onCreateOptionsMenu(final Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        ((Activity)context).getMenuInflater().inflate(R.menu.main, menu);
//        MenuItem upgrade = menu.findItem(R.id.action_buy_upgrade);
//        Authenticator authenticator = new Authenticator(this);
//        if (authenticator.isAuthenticated()) {
//            upgrade.setVisible(false);
//        } else {
//            upgrade.setVisible(true);
//        }
//        return true;
//    }

//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_buy_upgrade:
//                Intent buyNowActivityIntent = new Intent(context, ProUpgradeActivity.class);
//                buyNowActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(buyNowActivityIntent);
//                return true;
//            case R.id.action_settings:
//                // app icon in action bar clicked; go home
//                Intent settingsActivityIntent = new Intent(context, SettingsActivity.class);
//                settingsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(settingsActivityIntent);
//                return true;
//            case R.id.action_about:
//                Intent aboutActivityIntent = new Intent(context, AboutFragment.class);
//                aboutActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(aboutActivityIntent);
//                return true;
//            case R.id.action_add_event:
//                Intent addEventIntent = new Intent(Intent.ACTION_INSERT,
//                        CalendarContract.Events.CONTENT_URI);
//                addEventIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                startActivity(addEventIntent);
//                return true;
//            case R.id.action_beta:
//                Intent betaActivityIntent = new Intent(this, BetaReleaseActivity.class);
//                betaActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(betaActivityIntent);
//                return true;
//            case R.id.action_bug_report:
//                Intent reportBugActivity = new Intent(this, BugReportActivity.class);
//                reportBugActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(reportBugActivity);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    private void removeListViewEvent(View view) {
        cursor = databaseInterface.getEventCursor();
        cursorAdapter.swapCursor(cursor);
        cursorAdapter.notifyDataSetChanged();
    }
}