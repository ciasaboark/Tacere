package org.ciasaboark.tacere;

import org.ciasaboark.tacere.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {
	
	private static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "MainActivity onCreate() called");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//start the background service
		startService(new Intent(this, PollService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClickSettings(View view) {
		startActivity(new Intent("org.ciasaboark.tacere.SettingsActivity"));
	}
	
	public void onClickAbout(View view) {
		startActivity(new Intent("org.ciasaboark.tacere.AboutActivity"));
	}
	
	public void onStart() {
		Log.d(TAG, "MainActivity onStart() called");
		super.onStart();
	}
	
	public void onRestart() {
		Log.d(TAG, "MainActivity onRestart() called");
		super.onRestart();
	}
	
	public void onResume() {
		Log.d(TAG, "MainActivity onResume() called");
		super.onResume();
	}

	public void onPause() {
		Log.d(TAG, "MainActivity onPause() called");
		super.onPause();
	}
	
	public void onStop() {
		Log.d(TAG, "MainActivity onStop() called");
		super.onStop();
	}
	
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "MainActivity onDestroy() called");
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	        case R.id.action_settings:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, SettingsActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
	        case R.id.action_about:
	        	Intent i = new Intent(this, AboutActivity.class);
	        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	startActivity(i);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
		}
	}
}
