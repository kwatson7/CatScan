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
		Parse.initialize(this,
				"ZEkD9DX4sC7uRyxdzokND6GMMY7SVRTtF8Jk93L1",
				"JTx1zUX94kVINsCpLm4HLI4YDTFcxo4xwbRHlo6t"); 

		ParseUser.enableAutomaticUser();
		ParseACL defaultACL = new ParseACL();
		// Optionally enable public read access by default.
		defaultACL.setPublicReadAccess(true);
		defaultACL.setPublicWriteAccess(true);
		ParseACL.setDefaultACL(defaultACL, true);
	}

}
