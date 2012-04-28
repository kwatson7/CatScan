package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import serverObjects.CatPicture;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.tools.CustomActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Gallery;
import android.widget.ListView;

public class CatScanActivity
extends CustomActivity {

	// graphics
	private Gallery picturesList;  				// the main listView

	// misc variables
	private PicturesAdapter adapter; 				// The adapter holding the picture info
	private List<CatPicture> catsList; 				// The list of cat pictures
	private int pictureWindowWidth;
	private int currentAmountOfQueries = 0;
	
	// CONSTANTS
	private static final int QUERY_BATCH = 10;

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {
	//	testCreateCats();
		initializeLayout();	
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
						CatPicture cat = new CatPicture(data, "title "+(i+1), new ArrayList<String>(), Utils.getCurrentUser());
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
		picturesList = (Gallery) findViewById(R.id.pictures_list);
		
		// determine size of screen
		Display display = getWindowManager().getDefaultDisplay();
		pictureWindowWidth = display.getWidth();
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
		// grab cursor for all the groups
		getPictures();
		
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
		adapter = new PicturesAdapter(this, catsList, pictureWindowWidth);
		picturesList.setAdapter(adapter);
		
		// restore
		//picturesList.setSelectionFromTop(index, top);
		picturesList.setSelection(index, false);
	}

	/**
	 * Find the cursor required for pictures
	 */
	private void getPictures(){
		ParseQuery query = new ParseQuery("CatPicture");
		query.setCachePolicy(ParseQuery.CachePolicy.CACHE_ELSE_NETWORK );
		query.setSkip(currentAmountOfQueries);
		query.setLimit(QUERY_BATCH);
		query.findInBackground(new FindCallback() {
			
			@Override
			public void done(List<ParseObject> data, ParseException e) {
				if (e == null){
					if (catsList == null)
						catsList = CatPicture.convertList(data);
					else
						catsList.addAll(CatPicture.convertList(data));
					currentAmountOfQueries = currentAmountOfQueries + data.size();
					fillPictures();
				}else{
					e.printStackTrace();
					Log.e(Utils.APP_TAG, e.getMessage());
				}
			}
		});
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
		// TODO Auto-generated method stub
	//	throw new UnsupportedOperationException("Not supported yet.");

	}

}