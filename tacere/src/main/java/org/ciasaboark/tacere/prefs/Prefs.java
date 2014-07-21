/*
 * Copyright (c) 2014 Jonathan Nelson
 * Released under the BSD license.  For details see the COPYING file.
 */

package org.ciasaboark.tacere.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        String[] calendarArray = calendars.split(",");
        List<Long> calendarList = new ArrayList<Long>();
        for (String calendarId : calendarArray) {
            long id;
            try {
                id = Long.parseLong(calendarId);
                calendarList.add(id);
            } catch (NumberFormatException e) {
                Log.e(TAG, "unable to read calendar id '" + calendarId + "' as integer" +
                        " value, ignoring");
            }

        }
        return calendarList;
    }

    public void setSelectedCalendars(List<Long> calendarList) {
        String commaSeparatedCalIds = "";
        for (long id : calendarList) {
            commaSeparatedCalIds += id + ",";
        }
        editor.putString(Keys.SELECTED_CALENDARS, commaSeparatedCalIds).commit();
    }

    public void setSyncAllCalendars() {
        //TODO
    }

    public boolean shouldAllCalendarsBeSynced() {
        //TODO
        return false;
    }

    public Boolean getIsServiceActivated() {
        return sharedPreferences.getBoolean(Keys.IS_SERVICE_ACTIVATED, DefaultPrefs.IS_ACTIVATED);
    }

    public void setIsServiceActivated(Boolean isServiceActivated) {
        editor.putBoolean(Keys.IS_SERVICE_ACTIVATED, isServiceActivated).commit();
    }

    public Boolean getSilenceFreeTimeEvents() {
        return sharedPreferences.getBoolean(Keys.SILENCE_FREE_TIME, DefaultPrefs.SILENCE_FREE_TIME);
    }

    public void setSilenceFreeTimeEvents(Boolean silenceFreeTimeEvents) {
        editor.putBoolean(Keys.SILENCE_FREE_TIME, silenceFreeTimeEvents).commit();

    }

    public Boolean getSilenceAllDayEvents() {
        return sharedPreferences.getBoolean(Keys.SILENCE_ALL_DAY, DefaultPrefs.SILENCE_ALL_DAY);
    }

    public void setSilenceAllDayEvents(Boolean silenceAllDayEvents) {
        editor.putBoolean(Keys.SILENCE_ALL_DAY, silenceAllDayEvents).commit();
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

    public int getDefaultRinger() {
        return DefaultPrefs.RINGER_TYPE;
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
        this.setIsServiceActivated(DefaultPrefs.IS_ACTIVATED);
        this.setSilenceFreeTimeEvents(DefaultPrefs.SILENCE_FREE_TIME);
        this.setSilenceAllDayEvents(DefaultPrefs.SILENCE_ALL_DAY);
        this.setRingerType(DefaultPrefs.RINGER_TYPE);
        this.setAdjustAlarm(DefaultPrefs.ADJUST_ALARM);
        this.setAdjustMedia(DefaultPrefs.ADJUST_MEDIA);
        this.setCurAlarmVolume(DefaultPrefs.ALARM_VOLUME);
        this.setCurMediaVolume(DefaultPrefs.MEDIA_VOLUME);
        this.setQuickSilenceHours(DefaultPrefs.QUICK_SILENCE_HOURS);
        this.setQuicksilenceMinutes(DefaultPrefs.QUICK_SILENCE_MINUTES);
        this.setBufferMinutes(DefaultPrefs.BUFFER_MINUTES);
        this.setLookaheadDays(DefaultPrefs.LOOKAHEAD_DAYS);
    }

    public void remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("unable to remove null key");
        }
        sharedPreferences.edit().remove(key).apply();
    }

    /**
     * @author Jonathan Nelson <ciasaboark@gmail.com>
     */
    public static class Keys {
        //@formatter:off
        public static final String IS_SERVICE_ACTIVATED = "IS_ACTIVATED";
        public static final String SILENCE_FREE_TIME = "SILENCE_FREE_TIME";
        public static final String SILENCE_ALL_DAY = "SILENCE_ALL_DAY";
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
//		public static final String UPDATES_CHECKBOX 	= "UPDATES_CHECKBOX";


        //@formatter:on
    }
}