/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.database;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.event.Calendar;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.EventManager;
import org.ciasaboark.tacere.event.ringer.RingerSource;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.manager.ActiveEventManager;

import java.util.GregorianCalendar;
import java.util.List;

public class EventListItemInflator {
    private static final float DESATURATE_RATIO = 0.2f;
    private final Context context;
    private final View view;
    private final EventInstance event;
    private final DatabaseInterface databaseInterface;
    private int textColor;
    private int backgroundColor;
    private int iconTintColor;
    private boolean isEventFuture = false;


    public EventListItemInflator(final Context context, final View view, final EventInstance event) {
        this.context = context;
        this.view = view;
        this.event = event;
        this.databaseInterface = DatabaseInterface.getInstance(context);
    }

    public void inflateView() {
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

        java.util.Calendar calendarDate = new GregorianCalendar();
        calendarDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendarDate.set(java.util.Calendar.MINUTE, 0);
        calendarDate.set(java.util.Calendar.SECOND, 0);
        calendarDate.set(java.util.Calendar.MILLISECOND, 0);
        calendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);


        long tomorrowMidnight = calendarDate.getTimeInMillis();
        long eventBegin = event.getBegin();

        if (ActiveEventManager.isActiveEvent(event)) {
            backgroundColor = desaturateColor(context.getResources()
                    .getColor(R.color.accent), 0.1f);
        } else if (eventBegin >= tomorrowMidnight) {
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
        TextView descriptionTV = (TextView) view.findViewById(R.id.eventText);
        if (descriptionTV != null) {
            descriptionTV.setText(event.getTitle());
            descriptionTV.setTextColor(textColor);
        }

        // a text view to show the event date span
        String begin = event.getLocalBeginDate();
        String end = event.getLocalEndDate();
        String date;
        if (begin.equals(end)) {
            date = begin;
        } else {
            date = begin + " - " + end;
        }
        TextView dateTV = (TextView) view.findViewById(R.id.eventDate);
        if (dateTV != null) {
            dateTV.setText(date);
            dateTV.setTextColor(textColor);
        }

        // a text view to show the beginning and ending times for the event
        TextView timeTV = (TextView) view.findViewById(R.id.eventTime);
        if (timeTV != null) {
            StringBuilder timeSB = new StringBuilder(event.getLocalBeginTime() + " - "
                    + event.getLocalEndTime());

            if (event.isAllDay()) {
                timeSB = new StringBuilder(context.getString(R.string.all_day));
            }
            timeTV.setText(timeSB.toString());
            timeTV.setTextColor(textColor);
        }
    }

    private void drawSidebar() {
        ImageView sidebar = (ImageView) view.findViewById(R.id.event_sidebar_image);
        Drawable coloredSidebar = (Drawable) context.getResources().getDrawable(R.drawable.sidebar);
        int displayColor = event.getDisplayColor();
        if (isEventFuture) {
            displayColor = desaturateColor(displayColor, DESATURATE_RATIO);
        }
        coloredSidebar.mutate().setColorFilter(displayColor, PorterDuff.Mode.MULTIPLY);
        sidebar.setBackgroundDrawable(coloredSidebar);
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
        if (embeddedLetter != null) {
            long calendarId = event.getCalendarId();
            //TODO cache this info
            List<Calendar> calendars = databaseInterface.getCalendarIdList();
            char firstChar = ' ';
            for (org.ciasaboark.tacere.event.Calendar cal : calendars) {
                if (cal.getId() == calendarId) {
                    firstChar = cal.getDisplayName().charAt(0);
                    break;
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

    public static int desaturateColor(int color, float ratio) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        hsv[1] = (hsv[1] / 1 * ratio) + (DESATURATE_RATIO * (1.0f - ratio));

        return Color.HSVToColor(hsv);
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
            long calendarId = event.getCalendarId();
            String calendarTitle = databaseInterface.getCalendarNameForId(calendarId);
            eventCalendarTitle.setText(calendarTitle);
            eventCalendarTitle.setTextColor(textColor);
        }

        ImageView eventCalendarIcon = (ImageView) view.findViewById(R.id.event_calendar_icon);
        if (eventCalendarIcon != null) {
            Drawable d = eventCalendarIcon.getDrawable();
            d.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
            eventCalendarIcon.setImageDrawable(d);
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
                if (locationIcon != null) {
                    Drawable d = locationIcon.getDrawable();
                    d.mutate().setColorFilter(iconTintColor, PorterDuff.Mode.MULTIPLY);
                    locationIcon.setImageDrawable(d);
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

    public View getView() {
        return view;
    }
}
