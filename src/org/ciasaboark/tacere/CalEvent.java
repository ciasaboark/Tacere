package org.ciasaboark.tacere;

import java.text.DateFormat;
import java.util.Date;

public class CalEvent {
	private String title;
	private long begin;		//in ms, time since Epoch
	private long end;		//dito
	private long duration;
	
	public CalEvent() {
		super();
		this.title = null;
		this.begin = 0;
		this.end = 0;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public long getBegin() {
		return begin;
	}
	
	public void setBegin(long begin) {
		this.begin = begin;
	}
	
	public long getEnd() {
		return end;
	}
	
	public void setEnd(long end) {
		this.end = end;
	}
	
	public String toString() {
		//DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy h:mm a");
		DateFormat dateFormatter = DateFormat.getDateTimeInstance();
		Date date = new Date(end);
		String fdate = dateFormatter.format(date);
		return new String("\"" + title + "\", ends " + fdate);
	}
	
	//return true if this CalEvent only holds default values
	public boolean isBlank() {
		boolean result = true;
		if (title != null && begin != 0 && end != 0) {
			result = false;
		}
		return result;
	}
}
