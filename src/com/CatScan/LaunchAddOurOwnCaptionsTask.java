package com.CatScan;

import android.app.ProgressDialog;
import android.content.Intent;

import com.CatScan.ServerObjects.CatPicture;
import com.tools.CustomActivity;
import com.tools.CustomAsyncTask;
import com.tools.ImageCapture;

public class LaunchAddOurOwnCaptionsTask <ACTIVITY_TYPE extends CustomActivity>
extends CustomAsyncTask<ACTIVITY_TYPE, Void, String>{

	private CatPicture cat;
	public LaunchAddOurOwnCaptionsTask(
			ACTIVITY_TYPE act,
			int requestId,
			CatPicture cat) {
		super(
				act,
				requestId,
				true,
				true,
				null);
		this.cat = cat;
	}

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected String doInBackground(Void... params) {
		return cat.getRawPicturePath(applicationCtx);
	}

	@Override
	protected void onProgressUpdate(Void... progress) {
		
	}

	@Override
	protected void onPostExectueOverride(String result) {
		if (callingActivity == null)
			return;

			// launch the activity
		Intent intent = ImageCapture.createIntentToPassPhoto(
				applicationCtx,
				AddCaptionsToImage.class,
				null,
				result);
		callingActivity.startActivity(intent);
	}

	@Override
	protected void setupDialog() {
		if (callingActivity != null){
			dialog = new ProgressDialog(callingActivity);
			dialog.setTitle("Downloading Raw Picture");
			dialog.setMessage("Please Wait...");
			dialog.setIndeterminate(true);
		}
	}
}
