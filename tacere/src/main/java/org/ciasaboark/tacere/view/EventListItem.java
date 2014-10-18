/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.database.DatabaseInterface;
import org.ciasaboark.tacere.database.NoSuchEventInstanceException;
import org.ciasaboark.tacere.event.Calendar;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.ActiveEventManager;

import java.util.GregorianCalendar;
import java.util.List;

public class EventListItem extends LinearLayout {
    private static final String TAG = "EventListItem";
    private static final float DESATURATE_RATIO = 0.2f;
    private Context context;
    private EventInstance event = EventInstance.getBlankEvent();
    private DatabaseInterface databaseInterface;
    private int textColor;
    private int backgroundColor;
    private int iconTintColor;
    private boolean isEventFuture = false;
    private View view;
    private boolean forceActiveEvent = false;
    private boolean forceFutureEvent = false;
    private String calendarTitle = null;

    public EventListItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context == null) {
            throw new IllegalArgumentException("Context can not be null");
        }
        this.context = context;

        init(attrs);
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

            forceActiveEvent = a.getBoolean(R.styleable.EventListItem_isActiveEvent, false);
        } finally {
            a.recycle();
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.event_list_item, this);
        drawViews();
    }

    public void drawViews() {
        getColors();
        drawTextWidgets();
        drawSidebar();
        drawIcons();
        drawSidebarCharacter();
        drawTabletWidgets();
        colorizeBackground();
    }

    private void getColors() {
        textColor = context.getResources().getColor(R.color.textcolor);
        backgroundColor = context.getResources().getColor(R.color.event_list_item_background);
        iconTintColor = context.getResources().getColor(R.color.primary);

        if (isActiveEvent()) {
            backgroundColor = desaturateColor(context.getResources()
                    .getColor(R.color.accent), 0.1f);
        } else if (isFutureEvent()) {
            backgroundColor = context.getResources()
                    .getColor(R.color.event_list_item_future_background);
            textColor = context.getResources().getColor(R.color.textColorDisabled);
            iconTintColor = desaturateColor(iconTintColor, DESATURATE_RATIO);
            isEventFuture = true;
        } else {
            backgroundColor = context.getResources().getColor(R.color.event_list_item_background);
        }
    }

    private void drawTextWidgets() {
        drawTitleText();
        drawDateTimeText();
    }

    private void drawSidebar() {
        ImageView sidebar = (ImageView) view.findViewById(R.id.event_sidebar_image);
        Drawable sidebarDrawable = sidebar.getDrawable();
        int displayColor = event.getDisplayColor();
        if (isEventFuture) {
            displayColor = desaturateColor(displayColor, DESATURATE_RATIO);
        }
        sidebarDrawable.mutate().setColorFilter(displayColor, PorterDuff.Mode.MULTIPLY);
        sidebar.setImageDrawable(sidebarDrawable);
    }

    private void drawIcons() {
        // an icon to show the ringer state for this event
        ImageView eventIV = (ImageView) view.findViewById(R.id.ringerState);
        if (eventIV != null) {
            Drawable ringerIcon = getRingerIcon();
            eventIV.setImageDrawable(ringerIcon);
            eventIV.setContentDescription(context.getString(
                    R.string.icon_alt_text_normal));
        }

        ImageView ringerSourceView = (ImageView) view.findViewById(R.id.ringerSource);
        if (ringerSourceView != null) {
            Drawable ringerSource = getRingerSourceIcon();
            ringerSourceView.setImageDrawable(ringerSource);
        }
    }

    private void drawSidebarCharacter() {
        TextView embeddedLetter = (TextView) view.findViewById(R.id.embedded_letter);
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

    private void colorizeBackground() {
        View rootHolder = (View) view.findViewById(R.id.eventListItem);
        if (rootHolder != null) {
            rootHolder.setBackgroundColor(backgroundColor);
        }
    }

    public boolean isActiveEvent() {
        boolean isActive = false;
        if (this.forceActiveEvent || ActiveEventManager.isActiveEvent(event)) {
            isActive = true;
        }
        return isActive;
    }

    private static int desaturateColor(int color, float ratio) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        hsv[1] = (hsv[1] / 1 * ratio) + (DESATURATE_RATIO * (1.0f - ratio));

        return Color.HSVToColor(hsv);
    }

    private boolean isFutureEvent() {
        boolean isFuture = false;
        java.util.Calendar calendarDate = new GregorianCalendar();
        calendarDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendarDate.set(java.util.Calendar.MINUTE, 0);
        calendarDate.set(java.util.Calendar.SECOND, 0);
        calendarDate.set(java.util.Calendar.MILLISECOND, 0);
        calendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);
        long tomorrowMidnight = calendarDate.getTimeInMillis();
        long eventBegin = event.getBegin();
        if (this.forceFutureEvent || (eventBegin >= tomorrowMidnight)) {
            isFuture = true;
        }

        return isFuture;
    }

    private void drawTitleText() {
        TextView titleText = (TextView) view.findViewById(R.id.event_title);
        if (titleText != null) {
            titleText.setText(event.getTitle());
            titleText.setTextColor(textColor);
        }
    }

    private void drawDateTimeText() {
        String beginDate = event.getLocalBeginDate();
        String endDate = event.getLocalEndDate();
        String beginTime = event.getLocalBeginTime();
        String endTime = event.getLocalEndTime();

        TextView dateTimeField1 = (TextView) view.findViewById(R.id.event_date_time_field1);
        TextView dateTimeField2 = (TextView) view.findViewById(R.id.event_date_time_field2);
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
            }
        }

        dateTimeField1.setTextColor(textColor);
        dateTimeField2.setTextColor(textColor);

        ImageView clockIcon = (ImageView) view.findViewById(R.id.event_time_icon);
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

    private Drawable getRingerSourceIcon() {
        Drawable icon;

        EventManager eventManager = new EventManager(context, event);
        RingerSource ringerSource = eventManager.getRingerSource();

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
        ImageView eventRepetitionIcon = (ImageView) view.findViewById(R.id.event_time_icon);
        if (eventRepetitionIcon != null) {
            boolean eventRepeats = databaseInterface.doesEventRepeat(event.getEventId());
            Drawable icon;
            if (eventRepeats) {
                icon = context.getResources().getDrawable(R.drawable.history);
            } else {
                icon = context.getResources().getDrawable(R.drawable.clock);
            }

            icon.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
            eventRepetitionIcon.setImageDrawable(icon);
        }
    }

    private void drawCalendarTitle() {
        TextView eventCalendarTitle = (TextView) view.findViewById(R.id.event_calendar_text);
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

        ImageView calendarIcon = (ImageView) view.findViewById(R.id.event_calendar_icon);
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
        LinearLayout locationBox = (LinearLayout) view.findViewById(R.id.event_location_holder);
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

                ImageView locationIcon = (ImageView) view.findViewById(R.id.event_location_icon);
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

    public void setEvent(EventInstance e) {
        if (e == null) {
            this.event = EventInstance.getBlankEvent();
        } else {
            this.event = e;
        }
        invalidate();
        requestLayout();
        drawViews();
    }

    public void setIsActiveEvent(boolean isActiveEvent) {
        this.forceActiveEvent = isActiveEvent;
        this.forceFutureEvent = false;
        invalidate();
        requestLayout();
        drawViews();
    }

    public void setIsFutureEvent(boolean isFutureEvent) {
        this.forceFutureEvent = isFutureEvent;
        this.forceActiveEvent = false;
        invalidate();
        requestLayout();
        drawViews();
    }

    public void setCalendarTitle(String calendarTitle) {
        this.calendarTitle = calendarTitle;
        invalidate();
        requestLayout();
        drawViews();
    }
}
