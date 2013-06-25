package org.ciasaboark.tacere;

public class Preferences {
	   private static Preferences instance = null;
	   
	   //Defaults
	   public static final Boolean def_isActivated = false;
	   public static final Boolean def_silenceFreeTime = true;
	   public static final int def_ringerType = 3;
	   public static final Boolean def_adjustMedia = false;
	   public static final Boolean def_adjustAlarm = false;
	   public static final int def_mediaVolume = 100;
	   public static final int def_alarmVolume = 100;
	   public static final int def_quickSilenceMinutes = 5;
	   public static final int def_refreshInterval = 5;
	   
	   
	   
	   private Preferences() {
	      // Exists only to defeat instantiation.
	   }
	   public static Preferences getInstance() {
	      if(instance == null) {
	         instance = new Preferences();
	      }
	      return instance;
	   }
	}
