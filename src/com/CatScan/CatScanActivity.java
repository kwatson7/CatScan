package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import com.CatScan.PicturesAdapter.ImageSwitcher;
import com.CatScan.ServerObjects.CatPicture;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.tools.CustomActivity;
import com.tools.DialogWithInputBox;
import com.tools.ImageCapture;
import com.tools.ThreeObjects;
import com.tools.TwoStrings;
import com.tools.images.MemoryCache;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CatScanActivity
extends CustomActivity {

	// graphics
	private com.tools.images.CustomGallery picturesList;  	// the main gallery

	// misc variables
	private PicturesAdapter adapter; 				// The adapter holding the picture info
	private List<CatPicture> catsList; 				// The list of cat pictures
	private int pictureWindowWidth;
	private int pictureWindowHeight;
	private int currentAmountOfQueries = 0;
	private int currentPosition = 0; 				// The current position of the adapter
	private boolean currentlyQuerying = false;
	private HashSet<Integer> grabbedPictures = new HashSet<Integer>();
	private ImageCapture imageCaptureHelper;
	
	// CONSTANTS
	private static final int QUERY_BATCH = 10;
	private static final int LIMIT_TO_REQUERY = 8;
	private static final int GRAB_NEXT_N_PICTURES = 5;
	private static final int verticalPadding = 100;
	
	//enums for activity calls
	private enum ACTIVITY_CALLS {
		ASK_USER_NAME, CAMERA_REQUEST, EDIT_PICTURE;
		private static ACTIVITY_CALLS convert(int value)
		{
			return ACTIVITY_CALLS.class.getEnumConstants()[value];
		}
	}
	
	//enums for task calls
	private enum TASK_CALLS {
		SAVE_USER_ON_INITIAL, LAUNCH_ADD_OWN_CAPTIONS;
		private static TASK_CALLS convert(int value)
		{
			return TASK_CALLS.class.getEnumConstants()[value];
		}
	}
	
	// menu items
	private enum MENU_ITEMS {
		REFRESH;
		private static MENU_ITEMS convert(int value)
		{
			return MENU_ITEMS.class.getEnumConstants()[value];
		}
	}


	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
		
		// ask the user their name
		askUserName();
		
		// save user on first use
		SaveUserOnInitial task = new SaveUserOnInitial(this, TASK_CALLS.SAVE_USER_ON_INITIAL.ordinal());
		addTask(task);
		task.execute();
		
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
			
		// we a took a picture
		case CAMERA_REQUEST:
			if (imageCaptureHelper == null){
				Toast.makeText(ctx, "Camera error", Toast.LENGTH_LONG).show();
				return;
			}
			
			// make the intent with all the data required to load the picture
			Intent intent = imageCaptureHelper.createIntentWithCorrectExtras(ctx, AddCaptionsToImage.class, resultCode, data);
			if (intent != null)
				startActivityForResult(intent, ACTIVITY_CALLS.EDIT_PICTURE.ordinal());
			break;
		}
	}
	
	/**
	 * We are going to launch an intent to take a picture
	 * @param view
	 */
	public void takePictureClicked(View view){
		imageCaptureHelper = new ImageCapture();
		Intent cameraIntent = imageCaptureHelper.createImageCaptureIntent(ctx);
		startActivityForResult(cameraIntent, ACTIVITY_CALLS.CAMERA_REQUEST.ordinal()); 
	}
	
	/**
	 * Share the current picture with an intent
	 * @param view
	 */
	public void sharePicture(View view){
		String path = catsList.get(currentPosition).getPathToExternalStorage(ctx);
		if (path == null){
			Toast.makeText(ctx, "sd card required to share picture!", Toast.LENGTH_LONG).show();
			return;
		}
		com.tools.Tools.sharePicture(
				ctx,
				Prefs.getName(ctx) + " shared a picture with you",
				"Here is a cool Lol cat. You can view more and make your own! Just download CatScan on the android market",
				path,
				"Choose a Method to Share");
	}
	
	/**
	 * Make your own captions with the given picture
	 * @param view
	 */
	public void makeYourOwnCaptions(View view){
		// grab the path
		String path = catsList.get(currentPosition).getPathToExternalStorageRaw(ctx);

		// if we have a picture, then launch the activity, otherwise we need to create it
		if (path != null){
			// launch the activity
			Intent intent = ImageCapture.createIntentToPassPhoto(
					ctx,
					AddCaptionsToImage.class,
					null, path);
			startActivity(intent);
		}else{
			
			// do a task to do it
			LaunchAddOurOwnCaptionsTask task = new LaunchAddOurOwnCaptionsTask(
					this,
					TASK_CALLS.LAUNCH_ADD_OWN_CAPTIONS.ordinal(),
					catsList.get(currentPosition));
			addTask(task);
			task.execute();
		}
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
					CatPicture cat = new CatPicture(
							data,
							data,
							"title "+(i+1),
							new ArrayList<String>(),
							Prefs.getName(ctx),
							Utils.getCurrentUser());
					try {
						cat.save();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						Log.e("TAG", Log.getStackTraceString(e));
					}

				}
			}
		}
	}
	
	@Override
	protected void initializeLayout() {
		
		final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	    
		// set the layout
		setContentView(R.layout.pictures_thread);
		
		if ( customTitleSupported ) {
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_layout);
	    }

		// grab references to graphics
		picturesList = (com.tools.images.CustomGallery) findViewById(R.id.pictures_list);
		picturesList.setImageViewTouchId(R.id.picture);
		
		// determine size of screen
		Display display = getWindowManager().getDefaultDisplay();
		pictureWindowWidth = display.getWidth();
		pictureWindowHeight = display.getHeight() - verticalPadding;
		
		// grab cursor for all the groups
		getPictures(false);
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
	
	/**
	 * Refresh the list of pictures and jump to the first picture
	 */
	private void refresh(){
		currentAmountOfQueries = 0;
		getPictures(true);
	}

	/**
	 * fill list with the pictures
	 * @param refresh true to jump back to the first item, false otherwise
	 */
	private void fillPictures(boolean refresh) {
		
		// save index and top position
		int index = picturesList.getSelectedItemPosition();
		
		// set adapter
		MemoryCache<String> cache = null;
		if (adapter != null){
			adapter.imageLoader.stopThreads();
			cache = adapter.imageLoader.getMemoryCache();
		}
		adapter = new PicturesAdapter(this, catsList, pictureWindowWidth, pictureWindowHeight);
		adapter.setImageSwitcher(imageSwitcher);
		if (cache != null)
			adapter.imageLoader.restoreMemoryCache(cache);
		picturesList.setAdapter(adapter);
		
		// restore
		if (refresh || index >= catsList.size())
			picturesList.setSelection(0, true);
		else
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
					for (int i = 1; i <= GRAB_NEXT_N_PICTURES && catsList.size() > position+i; i++){
						if (!grabbedPictures.contains(position+i)){
							grabbedPictures.add(position+i);
							catsList.get(position+i).fetchPictureFromServer(ctx);
						}
					}
				}
			}).start();
			
			
			// if we are getting close to the end, we need to requery.
			if (currentPosition + LIMIT_TO_REQUERY >= currentAmountOfQueries && !currentlyQuerying)
				getPictures(false);
			
		}
	};

	/**
	 * Find the cursor required for pictures
	 * @param refresh if true, then clear out old data and start fresh, else just append
	 */
	private void getPictures(final boolean refresh){
		// keep track if we are querying
		currentlyQuerying = true;
		if (refresh)
			currentAmountOfQueries = 0;
		
		// run the query
		CatPicture.queryCats(currentAmountOfQueries, QUERY_BATCH, new FindCallback() {
			
			@Override
			public void done(List<ParseObject> data, ParseException e) {
				// not querying anymore
				currentlyQuerying = false;
				
				// we got back data, either initialize the list, or add it to it.
				if (e == null){
					if (refresh)
						catsList = null;
					if (catsList == null)
						catsList = CatPicture.convertList(data);
					else{
						if (data.size() == 0)
							return;
						catsList.addAll(CatPicture.convertList(data));
					}
					
					// keep track of how many posts we have loaded
					currentAmountOfQueries = catsList.size();
					
					// fill the adapter
					fillPictures(refresh);
				}else{
					Log.e(Utils.APP_TAG, e.getMessage());
				}
			}
		});
	}
	
	public void voteClicked(View view){
		catsList.get(currentPosition).vote(true);
		ImageView image = (ImageView) view;
		image.setImageDrawable(this.getResources().getDrawable(R.drawable.thumbs_up_selected));
		// up the increment
		TextView rating = (TextView) findViewById(R.id.rating);
		int oldRating = 0;
		try{
			oldRating = Integer.parseInt(rating.getText().toString());
		}catch(NumberFormatException  e){}
		oldRating++;
		rating.setText(String.valueOf(oldRating));
	}
	
	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		TASK_CALLS call = TASK_CALLS.convert(requestCode);
		
		// different types of calls
		switch(call){
		
		case SAVE_USER_ON_INITIAL:
			switch (asyncTypeCall){
			case POST:
				if (!((Boolean) data))
						finish();
				break;
			}
			break;
		}
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem refresh = menu.add(0,MENU_ITEMS.REFRESH.ordinal(), 0, "Refresh");

		// add icons
		refresh.setIcon(R.drawable.ic_menu_refresh);
		
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {

		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		MENU_ITEMS call = MENU_ITEMS.convert(item.getItemId());
		// decide on what each button should od
		switch(call) {
		case REFRESH:
			refresh();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
}