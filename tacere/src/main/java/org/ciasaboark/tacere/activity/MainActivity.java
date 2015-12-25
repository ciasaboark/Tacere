/*
 * Copyright (c) 2015 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.ciasaboark.tacere.R;
import org.ciasaboark.tacere.activity.fragment.AboutFragment;
import org.ciasaboark.tacere.activity.fragment.BugReportDialogFragment;
import org.ciasaboark.tacere.activity.fragment.EventsFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private View mainFragment;
    private Fragment lastFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        mainFragment = findViewById(R.id.main_fragment);
        if (savedInstanceState == null) {
            Fragment eventsFragment = new EventsFragment();
            loadFragment(eventsFragment);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void loadFragment(@NonNull Fragment fragment) {
        if (lastFragment != null && fragment.getClass().equals(lastFragment.getClass())) {
            Log.w(TAG, "will not load new fragment " + fragment.getClass() + " existing fragment is of same type");
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_main) {
            Fragment eventsFragment = new EventsFragment();
            loadFragment(eventsFragment);
        } else if (id == R.id.nav_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.nav_about) {
            Fragment aboutFragment = new AboutFragment();
            loadFragment(aboutFragment);
        } else if (id == R.id.nav_bug) {
            Toast.makeText(this, "bug report selected", Toast.LENGTH_LONG).show();
            BugReportDialogFragment bugReportDialogFragment = new BugReportDialogFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            bugReportDialogFragment.show(ft, "bug");

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
