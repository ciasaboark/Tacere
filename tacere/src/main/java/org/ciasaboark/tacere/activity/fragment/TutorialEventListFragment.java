/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.event.EventInstance;
import org.ciasaboark.tacere.event.ringer.RingerType;
import org.ciasaboark.tacere.view.EventListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TutorialEventListFragment extends Fragment {
    private final long NOW = System.currentTimeMillis();
    private int layout = R.layout.fragment_tutorial_page_event_list;
    private ViewGroup rootView;
    private List<String> eventTitles = new ArrayList<String>();
    private Random random = new Random();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        eventTitles.add("Meeting with Tom");
        eventTitles.add("Yoga");
        eventTitles.add("Super secret spy mission");
        eventTitles.add("Mission to Mars");

        renderEventListItems();
        return rootView;
    }

    private void renderEventListItems() {
        renderDefaultEvent();
        renderActiveEvent();
        renderFutureEvent();
    }

    private void renderDefaultEvent() {
        String eventTitle = getRandomTitle();
        long begin = NOW + EventInstance.MILLISECONDS_IN_MINUTE * 15;
        long end = begin + EventInstance.MILLISECONDS_IN_MINUTE * 60;
        EventInstance defaultEvent = new EventInstance(1, -1, -1,
                eventTitle,
                begin,
                end,
                "A default item",
                getResources().getColor(R.color.tutorial_event_default),
                false, false);
        defaultEvent.setLocation("Somewhere USA");
        defaultEvent.setRingerType(RingerType.IGNORE);
        EventListItem eventListItem = (EventListItem) rootView.findViewById(R.id.default_event);
        eventListItem.setEvent(defaultEvent);
        eventListItem.setCalendarTitle("Primary calendar");
        eventListItem.refresh();
    }

    private void renderActiveEvent() {
        String eventTitle = getRandomTitle();
        EventInstance defaultEvent = new EventInstance(1, -1, -1,
                eventTitle,
                NOW,
                NOW + EventInstance.MILLISECONDS_IN_MINUTE * 75,
                "A default item",
                getResources().getColor(R.color.tutorial_event_active),
                false, false);
        EventListItem eventListItem = (EventListItem) rootView.findViewById(R.id.active_event);
        eventListItem.setCalendarTitle("Work calendar");
        eventListItem.setIsActiveEvent(true);
        eventListItem.setEvent(defaultEvent);
        eventListItem.refresh();
    }

    private void renderFutureEvent() {
        String eventTitle = getRandomTitle();
        long begin = NOW + EventInstance.MILLISECONDS_IN_DAY * 3;
        long end = begin + EventInstance.MILLISECONDS_IN_MINUTE * 45;
        EventInstance defaultEvent = new EventInstance(1, -1, -1,
                eventTitle,
                begin,
                end,
                "A default item",
                getResources().getColor(R.color.tutorial_event_future),
                false, false);
        defaultEvent.setLocation("CCT room 403");
        defaultEvent.setRingerType(RingerType.NORMAL);
        EventListItem eventListItem = (EventListItem) rootView.findViewById(R.id.future_event);
        eventListItem.setIsFutureEvent(true);
        eventListItem.setCalendarTitle("Vacation time");
        eventListItem.setEvent(defaultEvent);
        eventListItem.refresh();
    }

    private String getRandomTitle() {
        int titleIndex = random.nextInt(eventTitles.size());
        String title = eventTitles.get(titleIndex);
        eventTitles.remove(titleIndex);
        return title;
    }
}