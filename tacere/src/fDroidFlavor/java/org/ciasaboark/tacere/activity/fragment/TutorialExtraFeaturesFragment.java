/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.tacere.R;

public class TutorialExtraFeaturesFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_fdroid;
    private ViewGroup rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);


        return rootView;
    }
}