/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

/**
 * Created by Jonathan Nelson on 9/27/14.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.viewpagerindicator.CirclePageIndicator;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.TutorialEventListFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialRingerPriorityFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialRingerSourceFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialWelcomeFragment;

public class TutorialActivity extends FragmentActivity {
    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        CirclePageIndicator pageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        pageIndicator.setViewPager(mPager);

        final Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                if (mPager.getCurrentItem() + 1 == mPager.getChildCount()) { //current item 0 indexed, count is not
                    nextButton.setText("Close");
                    nextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                }
            }
        });

        final Button skipButton = (Button) findViewById(R.id.tutorial_button_skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment pageFragment;
            switch (position) {
                case 0:
                    pageFragment = new TutorialWelcomeFragment();
                    break;
                case 1:
                    pageFragment = new TutorialEventListFragment();
                    break;
                case 2:
                    pageFragment = new TutorialRingerPriorityFragment();
                    break;
                case 3:
                    pageFragment = new TutorialRingerSourceFragment();
                    break;
                default:
                    pageFragment = new Fragment();
            }

            return pageFragment;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }
}
