/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.prefs.Prefs;

public class BetaSettingsFragment extends android.support.v4.app.Fragment {
    public static final String TAG = "BetaSettingsFragment";
    private View rootView;
    private Context context;
    private Prefs prefs;

    public BetaSettingsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_advanced_settings, container, false);
        context = getActivity();
        if (context == null) {
            throw new IllegalStateException("Fragment " + TAG + " can not find its activity");
        }
        prefs = new Prefs(context);
        drawAllWidgets();

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void drawAllWidgets() {
        //TODO
    }


}
