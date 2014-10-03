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
import org.ciasaboark.tacere.activity.fragment.TutorialEndFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialEventListFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialRingerPriorityFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialRingerSourceFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialWelcomeFragment;

public class TutorialActivity extends FragmentActivity {
    private static final int TOTAL_PAGES = 5;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private int currentPage = 0;

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        final Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
//        nextButton.setBackgroundColor(getResources().getColor(R.color.tutorial_next_button_background));
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });

        final Button skipButton = (Button) findViewById(R.id.tutorial_button_skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == TOTAL_PAGES) {
                    nextButton.setText(R.string.close);
                    nextButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        CirclePageIndicator pageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        pageIndicator.setViewPager(viewPager);
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentPage = viewPager.getCurrentItem();
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewPager.setCurrentItem(currentPage);
    }

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
                case 4:
                    pageFragment = new TutorialEndFragment();
                    break;
                default:
                    pageFragment = new Fragment();
            }

            return pageFragment;
        }

        @Override
        public int getCount() {
            return TOTAL_PAGES;
        }
    }
}
