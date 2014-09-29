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

import org.ciasaboark.tacere.R;

public class TutorialRingerSourceFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_page_ringer_source;
    private ViewGroup rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);

        colorizeIcons();
        return rootView;
    }

    private void colorizeIcons() {
        ImageView defaultView = (ImageView) rootView.findViewById(R.id.tutorial_icon_ringer_default);
        colorizeIcon(defaultView, getResources().getColor(R.color.ringer_default));

        ImageView calendarView = (ImageView) rootView.findViewById(R.id.tutorial_icon_ringer_calendar);
        colorizeIcon(calendarView, getResources().getColor(R.color.ringer_calendar));

        ImageView eventSeriesView = (ImageView) rootView.findViewById(R.id.tutorial_icon_ringer_event_series);
        colorizeIcon(eventSeriesView, getResources().getColor(R.color.ringer_series));

        ImageView instanceView = (ImageView) rootView.findViewById(R.id.tutorial_icon_ringer_instance);
        colorizeIcon(instanceView, getResources().getColor(R.color.ringer_instance));
    }

    private void colorizeIcon(ImageView view, int color) {
        Drawable d = view.getBackground();
        d.mutate().setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        view.setBackgroundDrawable(d); //TODO deprecated method use
    }
}