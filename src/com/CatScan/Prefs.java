package com.CatScan;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	
	// strings for accessing preferences file
	private static final String PREF_FILE = "appInfo.prefs";
	private static final int MODE_PRIVATE = Context.MODE_PRIVATE;
	private static final String N_TIMES_USED = "N_TIMES_USED";
	private static final String KEY_NAME = "KEY_NAME";
	
	// default values
	public static final String DEFAULT_STRING = "";
	public static final long DEFAULT_LONG = -1;
	public static final int DEFAULT_INT = -1;
	
	/**
	 * get the number of times, the user has opend the app
	 * @return the user name. Null if it does not exist
	 */
	public static int getNumberTimesOpened(Context ctx){
		int val = getIntPref(ctx, N_TIMES_USED);
		if (val < 0){
			setIntPref(ctx, N_TIMES_USED, 0);
			val = 0;
		}
		return val;
	}
	
	/**
	 * get the number of times, the user has opend the app
	 * @return the user name. Null if it does not exist
	 */
	public static void incrementNumberTimesUsed(Context ctx){
		int n = getNumberTimesOpened(ctx);
		n++;
		setIntPref(ctx, N_TIMES_USED, n);
	}
	
	/**
	 * get the name of the person using this phone
	 * @return the user's name. "" if not exist
	 */
	public static String getName(Context ctx){
		return getStringPref(ctx, KEY_NAME);
	}
	
	/**
	 * set the name of the person using this phone
	 */
	public static void setName(Context ctx, String name){
		if (name == null)
			name = "";
		setStringPref(ctx, KEY_NAME, name);
	}
	
	// private methods used for helper inside this class
	/**
	 * Get the preference object
	 * @param ctx The context used to access object
	 * @return
	 */
	private static SharedPreferences getPrefs(Context ctx) {
		return ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE);
    }
	
    private static String getStringPref(Context ctx, String pref) {
        return getPrefs(ctx).getString(pref, DEFAULT_STRING);
    }
    public static void setStringPref(Context ctx, String pref, String value) {
        getPrefs(ctx).edit().putString(pref, value).commit();
    }
    
    private static long getLongPref(Context ctx, String pref) {
        return getPrefs(ctx).getLong(pref, DEFAULT_LONG);
    }
    public static void setLongPref(Context ctx, String pref, long value) {
        getPrefs(ctx).edit().putLong(pref, value).commit();
    }
    
    private static int getIntPref(Context ctx, String pref) {
        return getPrefs(ctx).getInt(pref, DEFAULT_INT);
    }
    public static void setIntPref(Context ctx, String pref, int value) {
        getPrefs(ctx).edit().putInt(pref, value).commit();
    }
}