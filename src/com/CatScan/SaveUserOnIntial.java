package com.CatScan;

import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseException;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;

class SaveUserOnInitial extends CustomAsyncTask<Void, Void, String>{

	public SaveUserOnInitial(
			CustomActivity act,
			int requestId) {
		super(act,
				requestId,
				true,
				false,
				null);
	}

	@Override
	protected void onPreExecute() {	}

	@Override
	protected String doInBackground(Void... params) {
		// save user on first use
		if (Prefs.getNumberTimesOpened(applicationCtx) == 0)
			if(!Utils.getCurrentUser().save())
				return "Cannot use app without Server";
		
		return null;
	}

	@Override
	protected void onProgressUpdate(Void... progress) {	}

	@Override
	protected void onPostExectueOverride(String result) {
		if (result != null && result.length() > 0)
			Toast.makeText(applicationCtx, result, Toast.LENGTH_LONG).show();
		else{
			// keep track of how many times we use the app
			Prefs.incrementNumberTimesUsed(applicationCtx);
		}
		
	}

	@Override
	protected void setupDialog() {
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Initial Setup");
			dialog.setMessage("Please Wait...");
			dialog.setIndeterminate(true);
		}
	}
}