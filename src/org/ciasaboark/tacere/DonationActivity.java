package org.ciasaboark.tacere;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DonationActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donation);
	}
	 
	@Override
    public void onStart() {
		super.onStart();
    	
		Button closeButton = (Button)findViewById(R.id.donation_button_close);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences preferences = getApplicationContext().getSharedPreferences("org.ciasaboark.tacere.preferences", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putBoolean("show_donation_thanks", false);
				editor.apply();
				finish();
			}
		});
    }
    
}
