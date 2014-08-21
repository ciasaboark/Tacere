/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.ciasaboark.tacere.database.SimpleCalendarEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presents an abstracted view of the preferences that can be modified by the user
 *
 * @author Jonathan Nelson <ciasaboark@gmail.com>
 */
public class Prefs {
    private static final String TAG = "Prefs";
    private static final String PREFERENCES_NAME = "org.ciasaboark.tacere.preferences";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    public Prefs(Context ctx) {
        if (Prefs.sharedPreferences == null) {
            Prefs.sharedPreferences = ctx.getSharedPreferences(PREFERENCES_NAME,
                    Context.MODE_PRIVATE);
        }

        if (Prefs.editor == null) {
            editor = sharedPreferences.edit();
        }

    }

    public List<Long> getSelectedCalendars() {
        String calendars = sharedPreferences.getString(Keys.SELECTED_CALENDARS, "");
        String[] calendarIdArray = calendars.split(",");
        List<Long> calendarList = new ArrayList<Long>();
        for (String calendarId : calendarIdArray) {
            try {
                long id = Long.parseLong(calendarId);
                calendarList.add(id);
            } catch (NumberFormatException e) {
                Log.e(TAG, "unable to read calendar id '" + calendarId + "' as integer" +
                        " value, ignoring");
            }
        }
        return calendarList;
    }

    public void setSelectedCalendars(List<Long> calendarList) {
        //setting specific calendar ids to sync should clear the preference to sync all calendars
        setSyncAllCalendars(false);
        List<Long> noDuplicates = new ArrayList<Long>();
        for (Long l : calendarList) {
            if (!noDuplicates.contains(l)) {
                noDuplicates.add(l);
            }
        }

        String commaSeparatedCalIds = "";
        for (long id : noDuplicates) {
            commaSeparatedCalIds += id + ",";
        }
        editor.putString(Keys.SELECTED_CALENDARS, commaSeparatedCalIds).commit();
    }

    public void setSyncAllCalendars(boolean syncAllCalendars) {
        //syncing all calendars will remove any previously selected calendars to sync
        if (syncAllCalendars) {
            setSelectedCalendars(new ArrayList());
        }
        editor.putBoolean(Keys.SYNC_ALL_CALENDARS, syncAllCalendars).commit();
    }

    public boolean shouldAllCalendarsBeSynced() {
        boolean syncAllCalendars = sharedPreferences.getBoolean(Keys.SYNC_ALL_CALENDARS, DefaultPrefs.SYNC_ALL_CALENDARS);
        return syncAllCalendars;
    }

    public Boolean isServiceActivated() {
        return sharedPreferences.getBoolean(Keys.IS_SERVICE_ACTIVATED, DefaultPrefs.IS_SERVICE_ACTIVATED);
    }

    public void setIsServiceActivated(Boolean isServiceActivated) {
        editor.putBoolean(Keys.IS_SERVICE_ACTIVATED, isServiceActivated).commit();
    }

    public Boolean getSilenceFreeTimeEvents() {
        return sharedPreferences.getBoolean(Keys.SILENCE_FREE_TIME_EVENTS, DefaultPrefs.SILENCE_FREE_TIME_EVENTS);
    }

    public void setSilenceFreeTimeEvents(Boolean silenceFreeTimeEvents) {
        editor.putBoolean(Keys.SILENCE_FREE_TIME_EVENTS, silenceFreeTimeEvents).commit();

    }

    public Boolean getSilenceAllDayEvents() {
        return sharedPreferences.getBoolean(Keys.SILENCE_ALL_DAY_EVENTS, DefaultPrefs.SILENCE_ALL_DAY_EVENTS);
    }

    public void setSilenceAllDayEvents(Boolean silenceAllDayEvents) {
        editor.putBoolean(Keys.SILENCE_ALL_DAY_EVENTS, silenceAllDayEvents).commit();
    }

    public int getRingerType() {
        return sharedPreferences.getInt(Keys.RINGER_TYPE, DefaultPrefs.RINGER_TYPE);
    }

    public void setRingerType(int ringerType) {
        editor.putInt(Keys.RINGER_TYPE, ringerType).commit();
    }

    public Boolean getAdjustMedia() {
        return sharedPreferences.getBoolean(Keys.ADJUST_MEDIA, DefaultPrefs.ADJUST_MEDIA);
    }

    public void setAdjustMedia(Boolean adjustMedia) {
        editor.putBoolean(Keys.ADJUST_MEDIA, adjustMedia).commit();
    }

    public Boolean getAdjustAlarm() {
        return sharedPreferences.getBoolean(Keys.ADJUST_ALARM, DefaultPrefs.ADJUST_ALARM);
    }

    public void setAdjustAlarm(Boolean adjustAlarm) {
        editor.putBoolean(Keys.ADJUST_ALARM, adjustAlarm).commit();
    }

    public int getCurMediaVolume() {
        return sharedPreferences.getInt(Keys.MEDIA_VOLUME, DefaultPrefs.MEDIA_VOLUME);
    }

    public void setCurMediaVolume(int curMediaVolume) {
        editor.putInt(Keys.MEDIA_VOLUME, curMediaVolume).commit();
    }

    public int getCurAlarmVolume() {
        return sharedPreferences.getInt(Keys.ALARM_VOLUME, DefaultPrefs.ALARM_VOLUME);
    }

    public void setCurAlarmVolume(int curAlarmVolume) {
        editor.putInt(Keys.ALARM_VOLUME, curAlarmVolume).commit();
    }

    public int getQuicksilenceMinutes() {
        return sharedPreferences.getInt(Keys.QUICKSILENCE_MINUTES,
                DefaultPrefs.QUICK_SILENCE_MINUTES);
    }

    public void setQuicksilenceMinutes(int quicksilenceMinutes) {
        editor.putInt(Keys.QUICKSILENCE_MINUTES, quicksilenceMinutes).commit();
    }

    public int getQuickSilenceHours() {

        return sharedPreferences.getInt(Keys.QUICKSILENCE_HOURS, DefaultPrefs.QUICK_SILENCE_HOURS);
    }

    public void setQuickSilenceHours(int quickSilenceHours) {
        editor.putInt(Keys.QUICKSILENCE_HOURS, quickSilenceHours).commit();
    }

    public int getBufferMinutes() {
        return sharedPreferences.getInt(Keys.BUFFER_MINUTES, DefaultPrefs.BUFFER_MINUTES);
    }

    public void setBufferMinutes(int bufferMinutes) {
        editor.putInt(Keys.BUFFER_MINUTES, bufferMinutes).commit();
    }

    public int getLookaheadDays() {
        return sharedPreferences.getInt(Keys.LOOKAHEAD_DAYS, DefaultPrefs.LOOKAHEAD_DAYS);
    }

    public void setLookaheadDays(int lookaheadDays) {
        editor.putInt(Keys.LOOKAHEAD_DAYS, lookaheadDays).commit();
    }

    public boolean getDoNotDisturb() {
        return sharedPreferences.getBoolean(Keys.DO_NOT_DISTURB, DefaultPrefs.DO_NOT_DISTURB);
    }

    public void setDoNotDisturb(boolean doNotDisturb) {
        editor.putBoolean(Keys.DO_NOT_DISTURB, doNotDisturb).commit();
    }

    public int getDefaultMediaVolume() {
        return DefaultPrefs.MEDIA_VOLUME;
    }

    public int getDefaultAlarmVolume() {
        return DefaultPrefs.ALARM_VOLUME;
    }

    public boolean getBoolean(String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getBoolean(key, true);
        } else {
            throw new IllegalArgumentException("key " + key + " not found in preferences");
        }
    }

    public int readInt(String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getInt(key, Integer.MIN_VALUE);
        } else {
            throw new IllegalArgumentException("key " + key + " not found in preferences");
        }
    }

    public String readString(String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getString(key, null);
        } else {
            throw new IllegalArgumentException("key " + key + " not found in preferences");
        }
    }

    public long readLong(String key) {
        if (sharedPreferences.contains(key)) {
            return sharedPreferences.getLong(key, Long.MIN_VALUE);
        } else {
            throw new IllegalArgumentException("key " + key + " not found in preferences");
        }
    }

    private String convertMapToString(Map<Integer, Integer> map) {
        String line = "";
        for (Integer k : map.keySet()) {
            String key = k.toString();
            String value = map.get(k).toString();
            line += key + ":" + value + ",";
        }
        return line;
    }

    private Map<Integer, Integer> convertStringToIntegerMap(String line) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        if (line != "") {
            String[] keyValueTuples = line.split(",");
            for (String tuple : keyValueTuples) {
                try {
                    String[] keyValuePair = tuple.split(":");
                    Integer key = Integer.parseInt(keyValuePair[0]);
                    Integer value = Integer.parseInt(keyValuePair[1]);
                    map.put(key, value);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "error reading event id and ringer type from string: " + tuple);
                }
            }
        }

        return map;
    }

    private Map<Integer, Integer> getEventRingersMap() {
        //Event string should be in the format of <event id (int)>:<ringer type (int)>,...
        String eventsString = sharedPreferences.getString(Keys.EVENT_RINGERS, "");
        return convertStringToIntegerMap(eventsString);
    }

    private void setEventsRingerMap(Map<Integer, Integer> map) {
        String eventRingers = convertMapToString(map);
        editor.putString(Keys.EVENT_RINGERS, eventRingers).commit();
    }

    public int getRingerForEventSeries(int eventId) {
        int ringerType = SimpleCalendarEvent.RINGER.UNDEFINED;
        Map<Integer, Integer> map = getEventRingersMap();
        if (map.containsKey(eventId)) {
            ringerType = map.get(eventId);
        }
        return ringerType;
    }

    public void setRingerForEventSeries(int eventId, int ringerType) {
        Map<Integer, Integer> eventsMap = getEventRingersMap();
        eventsMap.put(eventId, ringerType);
        setEventsRingerMap(eventsMap);
    }

    public void unsetRingerTypeForEventSeries(int eventId) {
        Map<Integer, Integer> eventsMap = getEventRingersMap();
        eventsMap.remove(eventId);
        setEventsRingerMap(eventsMap);
    }


    public int getRingerForCalendar(long calendarId) {
        int ringerType = SimpleCalendarEvent.RINGER.UNDEFINED;
        Map<Integer, Integer> map = getCalendarRingersMap();
        if (map.containsKey(calendarId)) {
            ringerType = map.get(calendarId);
        }
        return ringerType;
    }

    private Map<Integer, Integer> getCalendarRingersMap() {
        String eventsString = sharedPreferences.getString(Keys.CALENDAR_RINGERS, "");
        return convertStringToIntegerMap(eventsString);
    }

    private void setCalendarRingersMap(Map<Integer, Integer> map) {
        String calendarRingers = convertMapToString(map);
        editor.putString(Keys.CALENDAR_RINGERS, calendarRingers).commit();
    }

    public void setRingerForCalendar(int calendarId, int ringerType) {
        Map<Integer, Integer> calendarMap = getCalendarRingersMap();
        calendarMap.put(calendarId, ringerType);
        setCalendarRingersMap(calendarMap);
    }

    public void unsetRingerTypeForCalendar(int calendarId) {
        Map<Integer, Integer> calendarMap = getCalendarRingersMap();
        calendarMap.remove(calendarId);
        setCalendarRingersMap(calendarMap);
    }

    public <V> void storePreference(String key, V value) {
        SharedPreferences.Editor sharedPrefEdit = sharedPreferences.edit();
        if (value instanceof String) {
            sharedPrefEdit.putString(key, (String) value);
        } else if (value instanceof Integer) {
            sharedPrefEdit.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            sharedPrefEdit.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            sharedPrefEdit.putBoolean(key, (Boolean) value);
        } else {
            throw new IllegalArgumentException(TAG
                    + " unable to store preference with unsupported type: "
                    + value.getClass().getName());
        }

        sharedPrefEdit.apply();
    }

    public void restoreDefaultPreferences() {
        sharedPreferences.edit().clear().commit();
    }

    public void remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("unable to remove null key");
        }
        sharedPreferences.edit().remove(key).apply();
    }


    private static class Keys {
        public static final String IS_SERVICE_ACTIVATED = "IS_ACTIVATED";
        public static final String SILENCE_FREE_TIME_EVENTS = "SILENCE_FREE_TIME_EVENTS";
        public static final String SILENCE_ALL_DAY_EVENTS = "SILENCE_ALL_DAY_EVENTS";
        public static final String RINGER_TYPE = "RINGER_TYPE";
        public static final String ADJUST_MEDIA = "ADJUST_MEDIA";
        public static final String ADJUST_ALARM = "ADJUST_ALARM";
        public static final String MEDIA_VOLUME = "MEDIA_VOLUME";
        public static final String ALARM_VOLUME = "ALARM_VOLUME";
        public static final String QUICKSILENCE_MINUTES = "QUICK_SILENCE_MINUTES";
        public static final String QUICKSILENCE_HOURS = "QUICK_SILENCE_HOURS";
        public static final String BUFFER_MINUTES = "BUFFER_MINUTES";
        public static final String LOOKAHEAD_DAYS = "LOOKAHEAD_DAYS";
        public static final String DO_NOT_DISTURB = "DO_NOT_DISTURB";
        public static final String SELECTED_CALENDARS = "SELECTED_CALENDARS";
        public static final String SYNC_ALL_CALENDARS = "SYNC_ALL_CALENDARS";

        //TODO work these in as replacements for adjusting alarm and media volume
        public static final String SILENCE_MEDIA = "SILENCE_MEDIA";
        public static final String SILENCE_ALARM = "SILENCE_ALARM";
        public static final String EVENT_RINGERS = "EVENT_RINGERS";
        public static final String CALENDAR_RINGERS = "CALENDAR_RINGERS";
    }
}