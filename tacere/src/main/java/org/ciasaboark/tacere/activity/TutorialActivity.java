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
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.viewpagerindicator.CirclePageIndicator;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.TutorialCrashReporterFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialEventListFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialProVersionFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialRingerPriorityFragment;
import org.ciasaboark.tacere.activity.fragment.TutorialWelcomeFragment;
import org.ciasaboark.tacere.manager.AlarmManagerWrapper;
import org.ciasaboark.tacere.service.RequestTypes;

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
        viewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        viewPager.setOffscreenPageLimit(5);

        final Button skipButton = (Button) findViewById(R.id.tutorial_button_skip);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        final CirclePageIndicator pageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        pageIndicator.setViewPager(viewPager);
        pageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
                //nothing to do here
            }

            @Override
            public void onPageSelected(int pageSelected) {
                int pageAdapterCount = pagerAdapter.getCount() - 1;
                if (pageSelected == pageAdapterCount) {
                    Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
                    if (nextButton != null) {
                        nextButton.setText("Done");
                        nextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
                    }

                    Button skipButton = (Button) findViewById(R.id.tutorial_button_skip);
                    if (skipButton != null) {
                        skipButton.setVisibility(View.GONE);
                    }
                }

//                final View tutorialBackground = findViewById(R.id.tutorial_background);
//                int colorFrom = ((ColorDrawable)tutorialBackground.getBackground()).getColor();
//                int colorTo;
//                switch (pageSelected) {
//                    case 0:
//                        colorTo = getResources().getColor(R.color.tutorial_welcome_background);
//                        break;
//                    case 1:
//                        colorTo = getResources().getColor(R.color.tutorial_event_list_background);
//                        break;
//                    case 2:
//                        colorTo = getResources().getColor(R.color.tutorial_ringer_priorities_background);
//                        break;
//                    case 3:
//                        colorTo = getResources().getColor(R.color.tutorial_pro_background);
//                        break;
//                    case 4:
//                        colorTo = getResources().getColor(R.color.tutorial_report_background);
//                        break;
//                    default:
//                        colorTo = getResources().getColor(R.color.primary);
//                }
//
//                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
//                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        tutorialBackground.setBackgroundColor((Integer)animator.getAnimatedValue());
//                    }
//
//                });
//                colorAnimation.setDuration(1000);
//                colorAnimation.start();
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                //nothing to do here
            }
        });
        final Button nextButton = (Button) findViewById(R.id.tutorial_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int curPage = viewPager.getCurrentItem();
                pageIndicator.setCurrentItem(curPage + 1);
            }
        });
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

    @Override
    protected void onStop() {
        super.onStop();
        AlarmManagerWrapper alarmManagerWrapper = new AlarmManagerWrapper(this);
        alarmManagerWrapper.scheduleImmediateAlarm(RequestTypes.NORMAL);
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
                    pageFragment = new TutorialProVersionFragment();
                    break;
                case 4:
                    pageFragment = new TutorialCrashReporterFragment();
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


    private class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    private class FadePageTransformer implements ViewPager.PageTransformer {
        private static final String TAG = "FadePageTransformer";

        public void transformPage(View view, float position) {
            Log.d(TAG, "position: " + position);
            int pageWidth = view.getWidth();
            if (position <= -1 || position >= 1) {
                //this page is off-screen
                view.setVisibility(View.GONE);
            } else { // [-1,0]
                //this page is comming into view, or fading out
                view.setVisibility(View.VISIBLE);
                //keep the page centered while fading in/out
                view.setTranslationX(pageWidth * -position);
                view.setAlpha(1 - Math.abs(position));

            }

        }
    }
}
