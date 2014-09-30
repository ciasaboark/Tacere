/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ciasaboark.tacere.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TutorialEventListFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_page_event_list;
    private ViewGroup rootView;
    private List<String> eventTitles = new ArrayList<String>();
    private long now = System.currentTimeMillis();
    private Random random = new Random();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        eventTitles.add("Meeting with Tom");
        eventTitles.add("Vacation");
        eventTitles.add("Yoga");
        eventTitles.add("Super secret spy mission");

        renderEventListItems();
        return rootView;
    }

    private void renderEventListItems() {
        renderDefaultEvent();
        renderActiveEvent();
        renderFutureEvent();
    }

    private void renderDefaultEvent() {
        ImageView sidebar = (ImageView) rootView.findViewById(R.id.default_event_sidebar);
        int sidebarColor = getResources().getColor(android.R.color.holo_red_light);
        colorSidebar(sidebar, sidebarColor);

        RelativeLayout frame = (RelativeLayout) rootView.findViewById(R.id.default_event_list_item);
        frame.setBackgroundColor(getResources().getColor(R.color.event_list_item_background));

        ImageView ringerIcon = (ImageView) rootView.findViewById(R.id.default_event_ringer);
        setRingerIcon(ringerIcon);

        TextView eventTitle = (TextView) rootView.findViewById(R.id.default_event_title);
        TextView eventDate = (TextView) rootView.findViewById(R.id.default_event_date);
        TextView eventTime = (TextView) rootView.findViewById(R.id.default_event_time);
        setRandomTitle(eventTitle);
    }

    private void setRandomTitle(TextView textView) {
        int titleIndex = random.nextInt(eventTitles.size());
        String title = eventTitles.get(titleIndex);
        eventTitles.remove(titleIndex);
        textView.setText(title);
    }

    private void renderActiveEvent() {
        ImageView sidebar = (ImageView) rootView.findViewById(R.id.active_event_sidebar);
        int sidebarColor = getResources().getColor(android.R.color.holo_orange_light);
        colorSidebar(sidebar, sidebarColor);

        RelativeLayout frame = (RelativeLayout) rootView.findViewById(R.id.active_event_list_item);
        frame.setBackgroundColor(getResources().getColor(R.color.event_list_item_active_event));

        ImageView ringerIcon = (ImageView) rootView.findViewById(R.id.active_event_ringer);
        setRingerIcon(ringerIcon);

        TextView eventTitle = (TextView) rootView.findViewById(R.id.active_event_title);
        TextView eventDate = (TextView) rootView.findViewById(R.id.future_event_title);
        TextView eventTime = (TextView) rootView.findViewById(R.id.future_event_title);
        setRandomTitle(eventTitle);
    }

    private void renderFutureEvent() {
        ImageView sidebar = (ImageView) rootView.findViewById(R.id.future_event_sidebar);
        int sidebarColor = getResources().getColor(android.R.color.holo_blue_light);
        colorSidebar(sidebar, sidebarColor);

        RelativeLayout frame = (RelativeLayout) rootView.findViewById(R.id.future_event_list_item);
        frame.setBackgroundColor(getResources().getColor(R.color.event_list_item_future_background));

        ImageView ringerIcon = (ImageView) rootView.findViewById(R.id.future_event_ringer);
        setRingerIcon(ringerIcon);

        TextView eventTitle = (TextView) rootView.findViewById(R.id.future_event_title);
        setRandomTitle(eventTitle);

        TextView eventDate = (TextView) rootView.findViewById(R.id.future_event_date);
        TextView eventTime = (TextView) rootView.findViewById(R.id.future_event_time);
        int textColor = getResources().getColor(R.color.event_list_item_future_text);
        eventTitle.setTextColor(textColor);
        eventDate.setTextColor(textColor);
        eventTime.setTextColor(textColor);
    }

    private void colorSidebar(ImageView view, int color) {
        Drawable d = view.getBackground();
        colorBackgroundDrawable(view, d, color);
    }

    private void setRingerIcon(ImageView view) {
        Drawable[] ringerDrawables = {
                getResources().getDrawable(R.drawable.ic_state_silent),
                getResources().getDrawable(R.drawable.ic_state_vibrate),
                getResources().getDrawable(R.drawable.ic_state_normal),
                getResources().getDrawable(R.drawable.ic_state_ignore)
        };
        Drawable d = ringerDrawables[random.nextInt(ringerDrawables.length)];

        int[] colors = {
                getResources().getColor(R.color.ringer_default),
                getResources().getColor(R.color.ringer_calendar),
                getResources().getColor(R.color.ringer_series),
                getResources().getColor(R.color.ringer_instance)
        };
        int color = colors[random.nextInt(colors.length)];

        colorBackgroundDrawable(view, d, color);
    }

    private void colorBackgroundDrawable(ImageView view, Drawable drawable, int color) {
        drawable.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        view.setImageDrawable(drawable);
    }
}