/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.EventDetailsFragment;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.Calendar;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.prefs.BetaPrefs;
import org.ciasaboark.tacere.service.RequestTypes;

import java.util.List;

public class EventListItem extends LinearLayout {
    private static final String TAG = "EventListItem";
    private static final float DESATURATE_RATIO = 0.6f;
    private Context context;
    private EventInstance event = null;
    private DatabaseInterface databaseInterface;
    private int textColor;
    private int iconTintColor;
    private View view;
    private boolean isActiveEvent = false;
    private boolean isFutureEvent = false;
    private String calendarTitle = null;
    private HeaderType headerType = HeaderType.NONE;

    //child views
    private ImageView sidebarImageView;
    private ImageView eventImageView;
    private ImageView ringerSourceImageView;
    private TextView embeddedLetter;
    private TextView titleTextView;
    private TextView dateTimeField1;
    private TextView dateTimeField2;
    private ImageView clockIcon;
    private ImageView eventRepetitionIcon;
    private TextView eventCalendarTitle;
    private ImageView calendarIcon;
    private LinearLayout locationBox;
    private ImageView locationIcon;
    private RelativeLayout spinnerBox;
    private RelativeLayout eventWidgets;
    private View eventListItem;

    public EventListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        this.context = context;

        init(attrs);
    }

    private static int desaturateColor(int color, float ratio) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        hsv[1] = (hsv[1] / 1 * ratio) + (DESATURATE_RATIO * (1.0f - ratio));

        return Color.HSVToColor(hsv);
    }

    private void findChildren() {
        sidebarImageView = (ImageView) view.findViewById(R.id.event_sidebar_image);
        ;
        eventImageView = (ImageView) view.findViewById(R.id.ringerState);
        ;
        ringerSourceImageView = (ImageView) view.findViewById(R.id.ringerSource);
        ;
        embeddedLetter = (TextView) view.findViewById(R.id.embedded_letter);
        titleTextView = (TextView) view.findViewById(R.id.event_title);
        dateTimeField1 = (TextView) view.findViewById(R.id.event_date_time_field1);
        dateTimeField2 = (TextView) view.findViewById(R.id.event_date_time_field2);
        clockIcon = (ImageView) view.findViewById(R.id.event_time_icon);
        eventCalendarTitle = (TextView) view.findViewById(R.id.event_calendar_text);
        calendarIcon = (ImageView) view.findViewById(R.id.event_calendar_icon);
        locationIcon = (ImageView) view.findViewById(R.id.event_location_icon);
        locationBox = (LinearLayout) view.findViewById(R.id.event_location_holder);
        spinnerBox = (RelativeLayout) view.findViewById(R.id.event_spinner);
        eventWidgets = (RelativeLayout) view.findViewById(R.id.event_widgets);
        eventListItem = (View) view.findViewById(R.id.event_list_item);
    }

    private void init(AttributeSet attrs) {
        this.databaseInterface = DatabaseInterface.getInstance(context);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.EventListItem,
                0, 0);

        long instanceId;
        try {
            instanceId = a.getInt(R.styleable.EventListItem_instanceId, -1);
            if (instanceId != -1) {
                try {
                    event = databaseInterface.getEvent(instanceId);
                } catch (NoSuchEventInstanceException e) {
                    Log.e(TAG, "given instance id " + instanceId + ", but this event not found " +
                            "in database, using default values");
                }
            }

            isActiveEvent = a.getBoolean(R.styleable.EventListItem_isActiveEvent, false);
        } finally {
            a.recycle();
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        BetaPrefs betaPrefs = new BetaPrefs(context);
        if (betaPrefs.getUseLargeDisplay()) {
            view = inflater.inflate(R.layout.event_list_item_large, this);
        } else {
            view = inflater.inflate(R.layout.event_list_item, this);
        }
        findChildren();
        attachOnClickListeners();
        drawViews();
    }

    private void attachOnClickListeners() {
        eventListItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (event != null) {
                    //we want the ringer to cycle to the next visible ringer, so if the event does
                    //not have a ringer set we jump to the next based on what the event manager provides
                    RingerType nextRingerType;
                    if (event.getRingerType() == RingerType.UNDEFINED) {
                        EventManager eventManager = new EventManager(context, event);
                        RingerType currentRinger = eventManager.getBestRinger();
                        nextRingerType = currentRinger.getNext();
                    } else {
                        nextRingerType = event.getRingerType().getNext();
                    }

                    databaseInterface.setRingerForInstance(event.getId(), nextRingerType);
                    try {
                        setEvent(databaseInterface.getEvent(event.getId()));
                    } catch (NoSuchEventInstanceException e) {
                        Log.e(TAG, "unable to refresh internal event from database, this should " +
                                "not have happened, event view will remain unrefreshed until" +
                                "reloaded");
                        e.printStackTrace();
                    }
                    refresh();

                    // since the database has changed we need to wake the service
                    AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(context);
                    alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
                }
            }
        });

        eventListItem.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    android.support.v4.app.FragmentManager fm = ((ActionBarActivity) context).getSupportFragmentManager();
                    EventDetailsFragment dialogFragment = EventDetailsFragment.newInstance(event);
                    dialogFragment.show(fm, EventDetailsFragment.TAG);
                } catch (ClassCastException e) {
                    Log.e(TAG, "unable to get the support fragment manager from the given context.");
                }
                return true;
            }
        });
    }

    private void drawViews() {
        if (event != null) {
            getColors();
            drawTextWidgets();
            drawSidebar();
            drawRingerIcons();
            drawSidebarCharacter();
            drawTabletWidgets();
            drawHeader();
            spinnerBox.setVisibility(View.GONE);
            eventWidgets.setVisibility(View.VISIBLE);
        } else {
            eventWidgets.setVisibility(View.GONE);
            spinnerBox.setVisibility(View.VISIBLE);
            hideHeader();
        }
    }


    private void getColors() {
        textColor = context.getResources().getColor(R.color.text_color);
        iconTintColor = context.getResources().getColor(R.color.icon_tint);

        if (isFutureEvent()) {
            textColor = context.getResources().getColor(R.color.text_color_disabled);
            iconTintColor = desaturateColor(iconTintColor, DESATURATE_RATIO);
        }
    }

    private void drawTextWidgets() {
        drawTitleText();
        drawDateTimeText();
    }

    private void drawSidebar() {
        Drawable sidebarDrawable = sidebarImageView.getDrawable();
        int displayColor = event.getDisplayColor();
        if (isFutureEvent()) {
            displayColor = desaturateColor(displayColor, DESATURATE_RATIO);
        }
        sidebarDrawable.mutate().setColorFilter(displayColor, PorterDuff.Mode.MULTIPLY);
        sidebarImageView.setImageDrawable(sidebarDrawable);
    }

    private void drawRingerIcons() {
        // an icon to show the ringer state for this event
        if (eventImageView != null) {
            Drawable ringerIcon = getRingerIcon();
            eventImageView.setImageDrawable(ringerIcon);
            eventImageView.setContentDescription(context.getString(
                    R.string.icon_alt_text_normal));
        }

        if (ringerSourceImageView != null) {
            EventManager eventManager = new EventManager(context, event);
            RingerSource ringerSource = eventManager.getRingerSource();
            if (ringerSource != RingerSource.DEFAULT) {
                ringerSourceImageView.setVisibility(View.VISIBLE);
                Drawable ringerSourceDrawable = getRingerSourceIcon(ringerSource);
                ringerSourceImageView.setImageDrawable(ringerSourceDrawable);
            } else {
                ringerSourceImageView.setVisibility(View.GONE);
            }
        }
    }

    private void drawSidebarCharacter() {
        char firstChar = ' ';
        if (embeddedLetter != null) {
            if (calendarTitle != null && calendarTitle.length() != 0) {
                //use the provided calendar title
                firstChar = calendarTitle.charAt(0);
            } else {
                long calendarId = event.getCalendarId();
                //TODO cache this info
                List<Calendar> calendars = databaseInterface.getCalendarIdList();
                for (org.ciasaboark.tacere.event.Calendar cal : calendars) {
                    if (cal.getId() == calendarId) {
                        firstChar = cal.getDisplayName().charAt(0);
                        break;
                    }
                }
            }
            embeddedLetter.setText(((Character) firstChar).toString().toUpperCase());
        }
    }

    private void drawTabletWidgets() {
        drawSidebarCharacter();
        drawRepeatingIcon();
        drawCalendarTitle();
        drawLocation();
    }

    public boolean isActiveEvent() {
        return isActiveEvent;
    }

    public boolean isFutureEvent() {
        return isFutureEvent;
    }

    private void drawTitleText() {
        String titleText;
        if (titleTextView != null) {
            if (isActiveEvent()) {
                titleText = ">>> " + event.getTitle();
            } else {
                titleText = event.getTitle();
            }
            titleTextView.setText(titleText);
            titleTextView.setTextColor(textColor);
        }
    }

    private void drawDateTimeText() {
        String beginDate = event.getLocalBeginDate();
        String endDate = event.getLocalEndDate();
        String beginTime = event.getLocalBeginTime();
        String endTime = event.getLocalEndTime();
        boolean usingExtendedMinutes = false;
        int extendMinutes = event.getExtendMinutes();
        if (extendMinutes != 0) {
            usingExtendedMinutes = true;
            endDate = event.getLocalEffectiveEndDate();
            endTime = event.getLocalEffectiveEndTime();
        }

        //there are a number of different ways to display the date and time
        if (beginDate.equals(endDate)) {
            if (event.isAllDay()) {
                //field1: <begin date>
                //field2: 'All Day'
                dateTimeField1.setText(beginDate);
                dateTimeField2.setText(R.string.all_day);
            } else {
                //field1: <begin date>
                //field2: <begin time> - <end time>
                dateTimeField1.setText(beginDate);
                dateTimeField2.setText(beginTime + " - " + endTime);
            }
        } else {
            if (event.isAllDay()) {
                //field1: <begin date> - <end date>
                //field2: 'All Day'
                dateTimeField1.setText(beginDate + " - " + endDate);
                dateTimeField2.setText(R.string.all_day);
            } else {
                //field1: <begin date> <begin time>
                //field2: <end date> <end time>
                dateTimeField1.setText(beginDate + " " + beginTime);
                dateTimeField2.setText(endDate + " " + endTime);
                if (usingExtendedMinutes) {
                    dateTimeField2.setTypeface(null, Typeface.BOLD);
                }
            }
        }

        dateTimeField1.setTextColor(textColor);
        dateTimeField2.setTextColor(textColor);

        if (isFutureEvent() && clockIcon != null) {
            try {
                Drawable d = clockIcon.getDrawable();
                d.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
                clockIcon.setImageDrawable(d);
            } catch (NullPointerException e) {
                Log.e(TAG, "caught null pointer exception pulling drawable from clock icon");
            }
        }
    }

    private Drawable getRingerIcon() {
        Drawable icon;
        int defaultColor = context.getResources().getColor(R.color.ringer_default);
        int color = defaultColor;


        EventManager eventManager = new EventManager(context, event);
        RingerType ringerType = eventManager.getBestRinger();
        icon = getIconForRinger(ringerType);

        RingerSource ringerSource = eventManager.getRingerSource();
        if (ringerSource != RingerSource.DEFAULT) {
            color = iconTintColor;
        }

        icon.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        if (icon == null) {
            throw new AssertionError(this.getClass().getName() + "Ringer icon should not be null");
        }
        return icon;
    }

    private Drawable getRingerSourceIcon(RingerSource ringerSource) {
        Drawable icon;

        switch (ringerSource) {
            case DEFAULT:
                icon = context.getResources().getDrawable(R.drawable.blank);
                break;
            case CALENDAR:
                icon = context.getResources().getDrawable(R.drawable.calendar_calendar);
                break;
            case EVENT_SERIES:
                icon = context.getResources().getDrawable(R.drawable.calendar_series);
                break;
            case INSTANCE:
                icon = context.getResources().getDrawable(R.drawable.calendar_instance);
                break;
            default:
                icon = context.getResources().getDrawable(R.drawable.blank);
        }
        icon.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);

        return icon;
    }

    private void drawRepeatingIcon() {
        if (clockIcon != null) {
            boolean eventRepeats = databaseInterface.doesEventRepeat(event.getEventId());
            Drawable icon;
            if (eventRepeats) {
                icon = context.getResources().getDrawable(R.drawable.history);
            } else {
                icon = context.getResources().getDrawable(R.drawable.clock);
            }

            icon.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
            clockIcon.setImageDrawable(icon);
        }
    }

    private void drawCalendarTitle() {
        if (calendarIcon != null) {
            Drawable calendarIconDrawable = getResources().getDrawable(R.drawable.calendar_calendar);
            calendarIconDrawable.setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
            calendarIcon.setImageDrawable(calendarIconDrawable);
        }
        if (eventCalendarTitle != null) {
            String title;
            if (calendarTitle != null) {
                title = calendarTitle;
            } else {
                long calendarId = event.getCalendarId();
                title = databaseInterface.getCalendarNameForId(calendarId);
            }
            eventCalendarTitle.setText(title);
            eventCalendarTitle.setTextColor(textColor);
        }

        if (isFutureEvent() && calendarIcon != null) {
            try {
                Drawable d = calendarIcon.getDrawable();
                d.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
                calendarIcon.setImageDrawable(d);
            } catch (NullPointerException e) {
                Log.e(TAG, "caught null pointer exception pulling drawable from calendar icon");
            }
        }
    }

    private void drawLocation() {
        if (locationIcon != null) {
            Drawable calendarIconDrawable = getResources().getDrawable(R.drawable.location);
            calendarIconDrawable.setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
            locationIcon.setImageDrawable(calendarIconDrawable);
        }

        if (locationBox != null) {
            String eventLocation = event.getLocation();
            if (eventLocation.equals("")) {
                locationBox.setVisibility(View.INVISIBLE);
            } else {
                locationBox.setVisibility(View.VISIBLE);
                TextView location = (TextView) view.findViewById(R.id.event_location);
                if (location != null) {
                    location.setText(eventLocation);
                    location.setTextColor(textColor);
                }

                if (isFutureEvent() && locationIcon != null) {
                    try {
                        Drawable d = locationIcon.getDrawable();
                        d.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
                        locationIcon.setImageDrawable(d);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "caught null pointer exception pulling drawable from location icon");
                    }
                }
            }
        }


    }

    private Drawable getIconForRinger(RingerType ringerType) {
        Drawable icon;
        if (ringerType == null) {
            icon = context.getResources().getDrawable(R.drawable.blank);
        } else {
            switch (ringerType) {
                case NORMAL:
                    icon = context.getResources().getDrawable(R.drawable.ic_state_normal);
                    break;
                case VIBRATE:
                    icon = context.getResources().getDrawable(R.drawable.ic_state_vibrate);
                    break;
                case SILENT:
                    icon = context.getResources().getDrawable(R.drawable.ic_state_silent);
                    break;
                case IGNORE:
                    icon = context.getResources().getDrawable(R.drawable.ic_state_ignore);
                    break;
                default:
                    icon = context.getResources().getDrawable(R.drawable.blank);
            }
        }

        return icon;
    }

    public void setEventById(long instanceId) {
        if (instanceId <= 0) {
            throw new IllegalArgumentException("instance id must be > 0");
        }
        try {
            EventInstance e = databaseInterface.getEvent(instanceId);
            setEvent(e);
        } catch (NoSuchEventInstanceException e) {
            this.event = null;
        }
    }

    public void setEvent(EventInstance e) {
        if (e == null) {
            Log.w(TAG, "setEvent() given null EventInstance, using default blank event");
            this.event = null;
            this.setIsActiveEvent(false);
            this.setIsFutureEvent(false);
        } else {
            this.event = e;

        }

//        invalidate();
//        requestLayout();
//        drawViews();
    }

    public void setIsActiveEvent(boolean isActiveEvent) {
        this.isActiveEvent = isActiveEvent;
        this.isFutureEvent = false;
//        invalidate();
//        requestLayout();
//        drawViews();
    }

    public void setIsFutureEvent(boolean isFutureEvent) {
        this.isFutureEvent = isFutureEvent;
        this.isActiveEvent = false;
//        invalidate();
//        requestLayout();
//        drawViews();
    }

    public void setCalendarTitle(String calendarTitle) {
        this.calendarTitle = calendarTitle;
//        invalidate();
//        requestLayout();
//        drawViews();
    }

    public void setHeaderType(HeaderType h) {
        this.headerType = h;
    }

    public void refresh() {
        invalidate();
        requestLayout();
        drawViews();
    }

    private void drawHeader() {
//        if (headerType == HeaderType.LATER) {
//            drawFutureHeader();
//        } else if (headerType == HeaderType.TODAY) {
//            drawTodayHeader();
//        } else {
//            hideHeader();
//        }
        hideHeader();
    }

    private void drawFutureHeader() {
        TextView header = (TextView) view.findViewById(R.id.event_list_header);
        if (header != null) {
            header.setVisibility(View.VISIBLE);
            header.setText("Future");
        }
    }

    private void drawTodayHeader() {
        TextView header = (TextView) view.findViewById(R.id.event_list_header);
        if (header != null) {
            header.setVisibility(View.VISIBLE);
            header.setText("Today");
        }
    }

    private void hideHeader() {
        TextView header = (TextView) view.findViewById(R.id.event_list_header);
        if (header != null) {
            header.setVisibility(View.GONE);
        }
    }

    public enum HeaderType {
        TODAY,
        LATER,
        NONE
    }

    private static class EventViewHolder {

    }
}
