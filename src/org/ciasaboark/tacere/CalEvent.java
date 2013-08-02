/*
 * Created by Jonathan Nelson
 * 
 * Copyright 2013 Jonathan Nelson
 *
 * Released under the BSD license.  For details see the COPYING file.
*/

package org.ciasaboark.tacere;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;

public class CalEvent {
	private static final String TAG = "CalEvent";
	
	private Integer id;
	private Integer cal_id;
	private String title;
	private Long begin;
	private Long end;		//in milliseconds from epoch
	private String description;
	private Integer ringerType;
	private Integer displayColor;
	private Boolean isFreeTime;
	private Boolean isAllDay;
	
	public static final int RINGER_TYPE_UNDEFINED = 0;
	public static final int RINGER_TYPE_NORMAL = 1;
	public static final int RINGER_TYPE_VIBRATE = 2;
	public static final int RINGER_TYPE_SILENT = 3;
	
	public static final long MILLISECONDS_IN_SECOND = 1000;
	public static final long MILLISECONDS_IN_MINUTE = MILLISECONDS_IN_SECOND * 60;
	public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_MINUTE * 60 * 24;
	

	
	public CalEvent(Context c) {
		super();
		this.id = null;
		this.cal_id = null;
		this.title = null;
		this.begin = null;
		this.end = null;
		this.description = null;
		this.ringerType = null;
		this.displayColor = null;
		this.isFreeTime = null;
		this.isAllDay = null;
	}
	
	public Boolean getIsFreeTime() {
		return isFreeTime;
	}

	public void setIsFreeTime(Boolean isFreeTime) {
		this.isFreeTime = isFreeTime;
	}

	public Boolean getIsAllDay() {
		return isAllDay;
	}

	public void setIsAllDay(Boolean isAllDay) {
		this.isAllDay = isAllDay;
	}

	public Integer getDisplayColor() {
		return displayColor;
	}

	public void setDisplayColor(Integer displayColor) {
		this.displayColor = displayColor;
	}

	public Long getBegin() {
		return begin;
	}

	public void setBegin(Long begin) {
		this.begin = begin;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getRingerType() {
		Integer result = ringerType;
		if (result == null) {
			result = 0;
		}
		return result;
	}

	public void setRingerType(Integer ringerType) {
		this.ringerType = ringerType;
	}

	public String getTitle() {
		String result;
		if (title == null || title.equals("")) {
			result = "(No title)";
		} else {
			result = title;
		}
		return result;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Integer getCal_id() {
		return cal_id;
	}

	public void setCal_id(Integer cal_id) {
		this.cal_id = cal_id;
	}

	public Long getEnd() {
		return end;
	}
	
	public void setEnd(Long end) {
		this.end = end;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getLocalBeginTime() {
		DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
		Date date = new Date(begin);
		return dateFormatter.format(date);
	}
	
	public String getLocalEndTime() {
		DateFormat dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
		Date date = new Date(end);
		return dateFormatter.format(date);
	}
	
	public String getLocalBeginDate() {
		DateFormat dateFormatter = DateFormat.getDateInstance();
		Date date;
		
		//according to the android calendar all day events start at
		//+ 8 PM the day before the event is scheduled.  This can
		//+ result in a wrong date being returned.
		if (isAllDay) {
			//shift ahead by one full day
			date = new Date(begin + MILLISECONDS_IN_DAY);
		} else {
			date = new Date(begin);
		}
		
		return dateFormatter.format(date);
	}
	
	public String getLocalEndDate() {
		DateFormat dateFormatter = DateFormat.getDateInstance();
		Date date = new Date(end);
		return dateFormatter.format(date);
	}
	
	public String toString() {
		DateFormat dateFormatter;
		dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
		Date date = new Date(end);
		String fdate = dateFormatter.format(date);
		return new String(title + ", ends " + fdate);
	}
	
	public boolean isValid() {
		boolean result = false;
		if (title != null && id != null && begin != null && end != null
				&& isFreeTime != null && isAllDay != null) {
			result = true;
		}
		return result;
	}
}
