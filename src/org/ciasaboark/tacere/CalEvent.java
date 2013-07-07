/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class CalEvent {
	private String title;
	private long end;		//in milliseconds from epoch
	private static final String TAG = "CalEvent";

	
	public CalEvent() {
		super();
		this.title = null;
		this.end = 0;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public long getEnd() {
		return end;
	}
	
	public void setEnd(long end) {
		this.end = end;
	}
	
	public String toString() {
		String locale = Locale.getDefault().toString();
		DateFormat dateFormatter;
		Log.d(TAG, "Locale detected: " + locale);
		if (locale.equals("en_US")) {
			//this works well for the US local, and produces a more concise notification
			//+ but may not be desirable for users in other locales
			dateFormatter = new SimpleDateFormat("MM/dd/yy h:mm a", Locale.US);		
		} else {
			//the fallback should generate a date/time format specific to the
			//+ users locale
			dateFormatter = DateFormat.getDateTimeInstance();
		}
		
		Date date = new Date(end);
		String fdate = dateFormatter.format(date);
		return new String(title + ", ends " + fdate);
	}
	
	//return true if this CalEvent only holds default values
	public boolean isBlank() {
		boolean result = true;
		if (title != null && end != 0) {
			result = false;
		}
		return result;
	}
}
