package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import com.CatScan.PicturesAdapter.ImageSwitcher;
import com.CatScan.ServerObjects.CatPicture;
import com.CatScan.ServerObjects.Vote;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.tools.CustomActivity;
import com.tools.DialogWithInputBox;
import com.tools.ThreeObjects;
import com.tools.TwoStrings;
import com.tools.images.MemoryCache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

public class CatScanActivity
extends CustomActivity {

	// graphics
	private com.tools.images.CustomGallery picturesList;  	// the main gallery

	// misc variables
	private PicturesAdapter adapter; 				// The adapter holding the picture info
	private List<CatPicture> catsList; 				// The list of cat pictures
	private int pictureWindowWidth;
	private int currentAmountOfQueries = 0;
	private int currentPosition = 0; 				// The current position of the adapter
	private boolean currentlyQuerying = false;
	private Context ctx = this;
	private HashSet<Integer> grabbedPictures = new HashSet<Integer>();
	private HashSet<String> likedPosts = new HashSet<String>();
	
	// CONSTANTS
	private static final int QUERY_BATCH = 10;
	private static final int LIMIT_TO_REQUERY = 2;
	private static final int GRAB_NEXT_N_PICTURES = 3;
	
	//enums for activity calls
		private enum ACTIVITY_CALLS {
			ASK_USER_NAME;
			private static ACTIVITY_CALLS convert(int value)
			{
				return ACTIVITY_CALLS.class.getEnumConstants()[value];
			}
		}
	

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		
		// ask the user their name
		askUserName();
		
		// save user on first use
		//TODO: put spinning dialog while doing first connect
		try {
			if (Prefs.getNumberTimesOpened(ctx) == 0)
				Utils.getCurrentUser().getParse().save();
		} catch (ParseException e) {
			Toast.makeText(ctx, "Cannot use app without Server", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		// keep track of how many times we use the app
		Prefs.incrementNumberTimesUsed(ctx);
		
		// find all posts the user likes
		//TODO: this is slow and needs to be done somewhere else
		likedPosts = Vote.getPostIdsUserLikes(Utils.getCurrentUser(), null);
		
		// set the layout
		initializeLayout();	
	}
	
	/**
	 * On first time use, we need to ask the user their name
	 * @return true if we asked the user, false otherwise.
	 */
	private boolean askUserName(){
		// first time use, we need to ask the user their name
		if (Prefs.getNumberTimesOpened(ctx) == 0){
			
			// find the user's name off phone
			ThreeObjects<TwoStrings, HashSet<String>, HashSet<String>> self = 
					com.tools.CustomCursors.findSelfInAddressBook(this);
			String firstName = null;
			if (self != null && self.mObject1 != null)
				firstName = self.mObject1.mObject1;
			if (firstName == null)
				firstName = "";
			
			// start the dialog
			Intent intent = new Intent(this, DialogWithInputBox.class);
			intent.putExtra(DialogWithInputBox.HINT_BUNDLE, "Your Name");
			intent.putExtra(DialogWithInputBox.DEFAULT_TEXT, firstName);
			intent.putExtra(DialogWithInputBox.TITLE_BUNDLE, "What's your Name?");
			startActivityForResult(intent, ACTIVITY_CALLS.ASK_USER_NAME.ordinal());
			return true;
		}else
			return false;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		ACTIVITY_CALLS call = ACTIVITY_CALLS.convert(requestCode);
		
		// different types of calls
		switch(call){
		// we asked the user their name
		case ASK_USER_NAME:
			if (resultCode == Activity.RESULT_OK && data != null){
				// save the user and store on server
				String name = data.getStringExtra(DialogWithInputBox.RESULT);
				Prefs.setName(ctx, name);
			}
			break;
		}
	}

	public boolean doWeLikePost(String id){
		return likedPosts.contains(id);
	}
	
	private void testCreateCats(){
		// get the folder
		File externalStorage = Environment.getExternalStorageDirectory();
		File folder = new File(externalStorage.getAbsolutePath() + "/Share Bear/Cats_2012-03-30 22_05_52/thumbnails/");
		
		// get list of files
		File[] files = folder.listFiles();
		
		// create cats from these files
		for (int i = 0; i < Math.min(files.length, 10); i++) 
		{

			if (files[i].isFile()) 
			{
				String name = files[i].getName();
				if (name.endsWith(".jpg"))
				{
					// read the data
					Bitmap bmp = BitmapFactory.decodeFile(files[i].getAbsolutePath());
					ByteArrayOutputStream baos = new ByteArrayOutputStream();  
					bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
					byte[] data = baos.toByteArray();
					try {
						CatPicture cat = new CatPicture(data, "title "+(i+1), new ArrayList<String>(), Prefs.getName(ctx), Utils.getCurrentUser());
						cat.save();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	@Override
	protected void initializeLayout() {
		// set the layout
		setContentView(R.layout.pictures_thread);

		// grab references to graphics
		picturesList = (com.tools.images.CustomGallery) findViewById(R.id.pictures_list);
		picturesList.setImageViewTouchId(R.id.picture);
		
		// determine size of screen
		Display display = getWindowManager().getDefaultDisplay();
		pictureWindowWidth = display.getWidth();
		
		// grab cursor for all the groups
		getPictures();
	}

	@Override
	public void onPause(){
		// pause threads
		if (adapter != null)
			adapter.imageLoader.stopThreads();
		super.onPause();
	}

	@Override
	public void onResume(){
		
		// restart thread
		if (adapter != null)
			adapter.imageLoader.restartThreads();
		super.onResume();
	}

	// fill list with the pictures
	private void fillPictures() {
		
		// save index and top position
		//int index = picturesList.getFirstVisiblePosition();
		int index = picturesList.getSelectedItemPosition();
		View v = picturesList.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		
		// set adapter
		MemoryCache<String> cache = null;
		if (adapter != null){
			adapter.imageLoader.stopThreads();
			cache = adapter.imageLoader.getMemoryCache();
		}
		adapter = new PicturesAdapter(this, catsList, pictureWindowWidth);
		adapter.setImageSwitcher(imageSwitcher);
		if (cache != null)
			adapter.imageLoader.restoreMemoryCache(cache);
		picturesList.setAdapter(adapter);
		
		
		// restore
		//picturesList.setSelectionFromTop(index, top);
		picturesList.setSelection(index, false);
	}
	
	private ImageSwitcher imageSwitcher = new ImageSwitcher() {
		
		@Override
		public void onImageSwitch(final int position) {
			// did we change positions?
			if (position == currentPosition)
				return;
			
			// save the positions
			currentPosition = position;
			
			// fetch the next picture, so it's ready
			new Thread(new Runnable() {
				public void run() {
					for (int i = 1; i < GRAB_NEXT_N_PICTURES && catsList.size() > position+i; i++){
						if (!grabbedPictures.contains(position+i)){
							grabbedPictures.add(position+i);
							catsList.get(position+i).getPicture(ctx, pictureWindowWidth);
						}
					}
				}
			}).start();
			
			
			// if we are getting close to the end, we need to requery.
			if (currentPosition + LIMIT_TO_REQUERY >= currentAmountOfQueries && !currentlyQuerying)
				getPictures();
			
		}
	};

	/**
	 * Find the cursor required for pictures
	 */
	private void getPictures(){
		currentlyQuerying = true;
		ParseQuery query = new ParseQuery(CatPicture.OBJECT_NAME);
		//query.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK );
		query.setSkip(currentAmountOfQueries);
		query.setLimit(QUERY_BATCH);
		query.include("USER");
		query.findInBackground(new FindCallback() {
			
			@Override
			public void done(List<ParseObject> data, ParseException e) {
				currentlyQuerying = false;
				if (e == null){
					if (catsList == null)
						catsList = CatPicture.convertList(data);
					else{
						if (data.size() == 0)
							return;
						catsList.addAll(CatPicture.convertList(data));
					}
					currentAmountOfQueries = currentAmountOfQueries + data.size();
					fillPictures();
				}else{
					e.printStackTrace();
					Log.e(Utils.APP_TAG, e.getMessage());
				}
			}
		});
	}
	
	public void voteClicked(View view){
		catsList.get(currentPosition).vote(true);
	}
	
	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");	
	}

	@Override
	protected void additionalConfigurationStoring() {
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	protected void onDestroyOverride() {
		picturesList.setAdapter(null);
	}
}