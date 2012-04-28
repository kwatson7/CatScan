package com.CatScan;

import com.parse.Parse;
import com.parse.ParseACL;

import com.parse.ParseUser;

import android.app.Application;

public class CatScanApplication
extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		// Add your initialization code here
		Parse.initialize(this, "Haa5IF2Q2MLn7B97JmjBXGEfGjIg4viWhup4E44f",
				"IrzOAdNrV8Fda9Mk1GH0J4ExMNgnTS2VG2al61Kf"); 

		ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
		// Optionally enable public read access by default.
		defaultACL.setPublicReadAccess(true);
		ParseACL.setDefaultACL(defaultACL, true);
	}

}
