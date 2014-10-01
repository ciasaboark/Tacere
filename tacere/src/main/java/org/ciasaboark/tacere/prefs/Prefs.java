/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.ciasaboark.tacere.event.ringer.RingerType;

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
        //remove any duplicate entries
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

    public Boolean shouldAvailableEventsSilence() {
        return sharedPreferences.getBoolean(Keys.SILENCE_FREE_TIME_EVENTS, DefaultPrefs.SILENCE_FREE_TIME_EVENTS);
    }

    public void setSilenceFreeTimeEvents(Boolean silenceFreeTimeEvents) {
        editor.putBoolean(Keys.SILENCE_FREE_TIME_EVENTS, silenceFreeTimeEvents).commit();

    }

    public Boolean shouldAllDayEventsSilence() {
        return sharedPreferences.getBoolean(Keys.SILENCE_ALL_DAY_EVENTS, DefaultPrefs.SILENCE_ALL_DAY_EVENTS);
    }

    public void setSilenceAllDayEvents(Boolean silenceAllDayEvents) {
        editor.putBoolean(Keys.SILENCE_ALL_DAY_EVENTS, silenceAllDayEvents).commit();
    }

    public RingerType getRingerType() {
        final int NO_STORED_RINGER = -1;
        RingerType storedRinger = DefaultPrefs.RINGER_TYPE;
        int ringerInt = sharedPreferences.getInt(Keys.RINGER_TYPE, NO_STORED_RINGER);

        if (ringerInt != NO_STORED_RINGER) {
            storedRinger = RingerType.getTypeForInt(ringerInt);
        }
        return storedRinger;
    }

    public void setRingerType(RingerType ringerType) {
        if (ringerType == null) {
            throw new IllegalArgumentException("Given ringer type must not be null");
        }
        Log.d(TAG, "Storing new default ringer type: " + ringerType.toString());
        editor.putInt(Keys.RINGER_TYPE, ringerType.value).commit();
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

    public RingerType getRingerForEventSeries(long eventId) {
        RingerType ringer = RingerType.UNDEFINED;

        Map<Long, Integer> map = getEventRingersMap();
        if (map.containsKey(eventId)) {
            int ringerInt = map.get(eventId);
            try {
                ringer = RingerType.getTypeForInt(ringerInt);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
                throw e;
            }
        }

        return ringer;
    }


    private Map<Long, Integer> getEventRingersMap() {
        //Event string should be in the format of <event id (long)>:<ringer type (int)>,...
        //i.e.: 134:1,5:2,16000:0,
        String eventsString = sharedPreferences.getString(Keys.EVENT_RINGERS, "");
        return convertStringToLongIntegerMap(eventsString);
    }

    private Map<Long, Integer> convertStringToLongIntegerMap(String line) {
        Map<Long, Integer> map = new HashMap<Long, Integer>();

        if (line != "") {
            String[] keyValueTuples = line.split(",");
            for (String tuple : keyValueTuples) {
                try {
                    String[] keyValuePair = tuple.split(":");
                    Long key = Long.parseLong(keyValuePair[0]);
                    Integer value = Integer.parseInt(keyValuePair[1]);
                    map.put(key, value);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "error reading event id and ringer type from string: " + tuple);
                }
            }
        }

        return map;
    }

    public void setRingerForEventSeries(long eventId, RingerType ringerType) {
        Map<Long, Integer> eventsMap = getEventRingersMap();
        eventsMap.put(eventId, ringerType.value);
        setEventsRingerMap(eventsMap);
    }

    private void setEventsRingerMap(Map<Long, Integer> map) {
        String eventRingers = convertMapToString(map);
        editor.putString(Keys.EVENT_RINGERS, eventRingers).commit();
    }

    private String convertMapToString(Map<Long, Integer> map) {
        String line = "";
        for (Long k : map.keySet()) {
            String key = k.toString();
            String value = map.get(k).toString();
            line += key + ":" + value + ",";
        }
        return line;
    }

    public void unsetRingerTypeForEventSeries(long eventId) {
        Map<Long, Integer> eventsMap = getEventRingersMap();
        eventsMap.remove(eventId);
        setEventsRingerMap(eventsMap);
    }


    public RingerType getRingerForCalendar(long calendarId) {
        RingerType ringer = RingerType.UNDEFINED;

        Map<Long, Integer> map = getCalendarRingersMap();
        if (map.containsKey(calendarId)) {
            int ringerInt = map.get(calendarId);
            try {
                ringer = RingerType.getTypeForInt(ringerInt);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
                throw e;
            }
        }

        return ringer;
    }

    private Map<Long, Integer> getCalendarRingersMap() {
        String eventsString = sharedPreferences.getString(Keys.CALENDAR_RINGERS, "");
        return convertStringToLongIntegerMap(eventsString);
    }

    private void setCalendarRingersMap(Map<Long, Integer> map) {
        String calendarRingers = convertMapToString(map);
        editor.putString(Keys.CALENDAR_RINGERS, calendarRingers).commit();
    }

    public void setRingerForCalendar(long calendarId, int ringerType) {
        Map<Long, Integer> calendarMap = getCalendarRingersMap();
        calendarMap.put(calendarId, ringerType);
        setCalendarRingersMap(calendarMap);
    }

    public void unsetRingerTypeForCalendar(long calendarId) {
        Map<Long, Integer> calendarMap = getCalendarRingersMap();
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

    public boolean shouldMediaVolumeBeSilenced() {
        return sharedPreferences.getBoolean(Keys.SILENCE_MEDIA, DefaultPrefs.SILENCE_MEDIA);
    }

    public void setMediaVolumeShouldSilence(boolean shouldSilence) {
        editor.putBoolean(Keys.SILENCE_MEDIA, shouldSilence).commit();
    }

    public boolean shouldAlarmVolumeBeSilenced() {
        return sharedPreferences.getBoolean(Keys.SILENCE_ALARM, DefaultPrefs.SILENCE_ALARM);
    }

    public void setAlarmVolumeShouldSilence(boolean shouldSilence) {
        editor.putBoolean(Keys.SILENCE_ALARM, shouldSilence).commit();
    }

    public void storeMediaVolume(int curVolume) {
        editor.putInt(Keys.MEDIA_VOLUME, curVolume).commit();
    }

    public void storeAlarmVolume(int curVolume) {
        editor.putInt(Keys.ALARM_VOLUME, curVolume).commit();
    }

    public int getStoredMediaVolume() {
        return sharedPreferences.getInt(Keys.MEDIA_VOLUME, DefaultPrefs.MEDIA_VOLUME);
    }

    public int getStoredAlarmVolume() {
        return sharedPreferences.getInt(Keys.ALARM_VOLUME, DefaultPrefs.ALARM_VOLUME);
    }

    public boolean isFirstRun() {
        return sharedPreferences.getBoolean(Keys.IS_FIRSTRUN, true);
    }

    public void disableFirstRun() {
        editor.putBoolean(Keys.IS_FIRSTRUN, false).commit();
    }


    private static class Keys {
        public static final String IS_SERVICE_ACTIVATED = "IS_ACTIVATED";
        public static final String SILENCE_FREE_TIME_EVENTS = "SILENCE_FREE_TIME_EVENTS";
        public static final String SILENCE_ALL_DAY_EVENTS = "SILENCE_ALL_DAY_EVENTS";
        public static final String RINGER_TYPE = "RINGER_TYPE";
        public static final String QUICKSILENCE_MINUTES = "QUICK_SILENCE_MINUTES";
        public static final String QUICKSILENCE_HOURS = "QUICK_SILENCE_HOURS";
        public static final String BUFFER_MINUTES = "BUFFER_MINUTES";
        public static final String LOOKAHEAD_DAYS = "LOOKAHEAD_DAYS";
        public static final String DO_NOT_DISTURB = "DO_NOT_DISTURB";
        public static final String SELECTED_CALENDARS = "SELECTED_CALENDARS";
        public static final String SYNC_ALL_CALENDARS = "SYNC_ALL_CALENDARS";
        public static final String SILENCE_MEDIA = "SILENCE_MEDIA";
        public static final String SILENCE_ALARM = "SILENCE_ALARM";
        public static final String EVENT_RINGERS = "EVENT_RINGERS";
        public static final String CALENDAR_RINGERS = "CALENDAR_RINGERS";
        public static final String MEDIA_VOLUME = "MEDIA_VOLUME";
        public static final String ALARM_VOLUME = "ALARM_VOLUME";
        public static final String IS_FIRSTRUN = "IS_FIRSTRUN";
    }
}