/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.ProUpgradeActivity;
import org.ciasaboark.tacere.billing.Authenticator;

public class TutorialExtraFeaturesFragment extends Fragment {
    private int layout = R.layout.fragment_tutorial_page_pro;
    private ViewGroup rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                layout, container, false);
        View thankYouPage = rootView.findViewById(R.id.tutorial_pro_page_thanks);
        View upgradePage = rootView.findViewById(R.id.tutorial_pro_page_upgrade);
        Authenticator authenticator = new Authenticator(getActivity());
        if (authenticator.isAuthenticated()) {
            thankYouPage.setVisibility(View.VISIBLE);
            upgradePage.setVisibility(View.GONE);
        } else {
            thankYouPage.setVisibility(View.GONE);
            upgradePage.setVisibility(View.VISIBLE);
        }

        Button upgradeButton = (Button) rootView.findViewById(R.id.tutorial_pro_upgrade_button);
        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent buyNowActivityIntent = new Intent(rootView.getContext(), ProUpgradeActivity.class);
                buyNowActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(buyNowActivityIntent);
            }
        });
        return rootView;
    }
}