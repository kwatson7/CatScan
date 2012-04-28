package com.CatScan;

import serverObjects.CatUser;
import com.parse.ParseUser;

public class Utils {

	public static final String APP_TAG = "CatScan";
	/**
	 * Return the current User. If there is no user, then an anonymous one is created in the background.
	 * Null will be returned when there is no user.
	 * @return
	 */
	public static CatUser getCurrentUser(){
		return new CatUser(ParseUser.getCurrentUser());
	}
}
