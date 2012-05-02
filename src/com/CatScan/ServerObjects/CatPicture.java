package com.CatScan.ServerObjects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import com.CatScan.R;
import com.CatScan.Utils;
import com.CatScan.ServerObjects.Vote.VoteCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tools.WidthHeight;

public class CatPicture{

	// the object name
	private static final String OBJECT_NAME = "CatPicture"; 		// the object name for the ParseObject

	// the fields
	private static final String FILE = "FILE";  					// The filename field for the picture
	private static final String RAW = "RAW_FILE"; 					// The raw picture with no captions
	private static final String TITLE = "TITLE"; 					// The title of the picture
	private static final String CAPTIONS = "CAPTIONS"; 				// The captions in the picture
	private static final String USER = "USER";						// The user who created the post
	private static final String RATING = "RATING"; 					// The rating this post has
	private static final String N_REPORTS = "N_REPORTS";			// The number of reports this post has
	private static final String N_COMMENTS = "N_COMMENTS"; 			// The number of comments this post has
	private static final String NAME_WHO_POSTED = "NAME_WHO_POSTED";

	// other private constants
	private static final String DEFAULT_FILE_NAME = "FILE_NAME.JPG";// The filename to use when uploading
	private static final int DEFAULT_N_TRIES = 3;

	// private variables
	private ParseObject parse; 										// the parse object this wraps around
	private CatPicture catPicture = this;
	private ParseFile imageFile;
	private ParseFile rawFile;	
	
	//enums for sort order
	private enum SortOrder {
		NEWEST, RATING, COMBO_ALGORITHM;
	}
	private static SortOrder sortOrder = SortOrder.NEWEST;
	
	/**
	 * Set the sort order for all future queries
	 * @param order
	 */
	public static void setSortOrder(SortOrder order){
		sortOrder = order;
	}

	/**
	 * Create a CatPicture
	 * @param picData the picture data returned from camera or picture selector 
	 * @param rawPicture data for the raw picture without captions
	 * @param title The title of the picture
	 * @param captions An array list of all the captions that are on the picture
	 * @param nameWhoPosted the name attached to this post
	 * @param user the user who posted it
	 * @throws ParseException 
	 */
	public CatPicture(
			byte[] picData,
			byte[] rawPicture,
			String title,
			ArrayList<String> captions,
			String nameWhoPosted,
			CatUser user){

		parse = new ParseObject(OBJECT_NAME);

		// put the data into the parse object
		parse.put(TITLE, title);
		if (captions == null)
			captions = new ArrayList<String>(0);
		parse.put(CAPTIONS, captions);
		user.putUser(parse, USER);
		parse.put(RATING, 0);
		parse.put(N_COMMENTS, 0);
		parse.put(N_REPORTS, 0);	
		parse.put(NAME_WHO_POSTED, nameWhoPosted);

		// store the files, we will save them, when we save the picture
		ParseFile file = new ParseFile(DEFAULT_FILE_NAME, picData);
		ParseFile raw = new ParseFile(DEFAULT_FILE_NAME, rawPicture);
		this.imageFile = file;
		this.rawFile = raw;
	}
	
	/**
	 * Are we save to save this object
	 * @return
	 */
	public boolean isSafeToSave(){
		return( isImageFileSaved() && isRawImageFileSaved());
	}
	
	/**
	 * Return wheither the image file is saved yet
	 * @return
	 */
	public boolean isImageFileSaved(){
		ParseFile file = getImageFile();
		if (file == null)
			return false;
		if(file.isDataAvailable() && file.isDirty())
			return false;
		else
			return true;
	}
	
	/**
	 * Return wheither the raw image file is saved yet
	 * @return
	 */
	public boolean isRawImageFileSaved(){
		ParseFile file = getRawFile();
		if (file == null)
			return false;
		if(file.isDataAvailable() && file.isDirty())
			return false;
		else
			return true;
	}

	/**
	 * Create a CatPicture from a representative ParseObject
	 * @param parse
	 */
	public CatPicture(ParseObject parse){
		//TODO: check inputs for other server objects
		if (!parse.getClassName().equals(OBJECT_NAME))
			throw new IllegalArgumentException("Only " + OBJECT_NAME + " can be passed into CatPicture");
		this.parse = parse;
	}

	/**
	 * Convert a list of ParseObjects to CatPictures
	 * @param parse
	 * @return
	 */
	public static List<CatPicture> convertList(List<ParseObject> parse){
		List<CatPicture> out = new ArrayList<CatPicture>();
		for (ParseObject item : parse)
			out.add(new CatPicture(item));
		return out;
	}

	/**
	 * Return the title of this picture
	 * @return
	 */
	public String getTitle(){
		return parse.getString(TITLE);
	}

	/**
	 * Return the list of captions that are stored in this object
	 * @return
	 */
	public List<String> getCaptions(){
		return parse.getList(CAPTIONS);
	}

	/**
	 * Get the name of the person who posted the picture
	 * @return
	 */
	public String getNameWhoPosted(){
		return parse.getString(NAME_WHO_POSTED);
	}

	/**
	 * Return the picture stored as a bitmap. Null if unsuccessful. <br>
	 * We will attempt to read from the device first, and then from the web if unsuccessful <br>
	 * *** This is slow, so it should be called from a background thread. ***
	 * @param ctx required to read from device, pass null if we want to just read from server
	 * @param desiredWidth, null to do nothing, input a number of pixels to resize image to this width
	 * @param desiredHeight, null to do nothing, input a number of pixels to resize imate to this height
	 * 
	 * @return the bitmap data
	 */
	public Bitmap getPicture(Context ctx, Integer desiredWidth, Integer desiredHeight){
		Log.i(Utils.APP_TAG, getId()+" getPicture");
		// try reading data from local device first
		byte[] data = null;
		if (ctx != null)
			data = getPictureFromDevice(ctx);

		// if the data is null, then read from server
		if (data == null || data.length == 0)
			data = getPictureFromServer(ctx, DEFAULT_N_TRIES);

		// if we're still null, then convert to missing picture
		if (data == null || data.length == 0){
			Log.e(Utils.APP_TAG, "could not get picture");
			return null;
		}
			//return null;
			//return ((BitmapDrawable)ctx.getResources().getDrawable(R.drawable.missing_picture)).getBitmap();

		// resize the data
		if (desiredWidth != null && desiredHeight != null)
			data = com.tools.Tools.resizeByteArray(data, new WidthHeight(desiredWidth, desiredHeight), "resizeSmall", null, 0f);
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	/**
	 * Return the raws picture path on device. Null if unsuccessful. <br>
	 * We will create the picture first and then return the path
	 * *** This is slow, so it should be called from a background thread. ***
	 * @param ctx required to write to device
	 * 
	 * @return the path of the raw picture file
	 */
	public String getRawPicturePath(Context ctx){
		
		// grab the file
		ParseFile file = getRawFile();
		if (file == null)
			return null;
		
		// now grab the actual data
		byte[] data;
		try {
			if (!file.isDataAvailable())
				file.cancel();
			data = file.getData();
			if (data == null || data.length == 0){
				parse.fetchIfNeeded();
				file = getRawFile();
				file.cancel();
				data = file.getData();
			}
		} catch (ParseException e) {
			Log.e(Utils.APP_TAG, e.getMessage());
			return null;
		}catch(Exception e){
			String err = "get picture error";
			if (e != null && e.getMessage() != null)
				err = e.getMessage();
			Log.e(Utils.APP_TAG, err);
			return null;
		}
		if (data == null)
			return null;
		
		// write the data
		return Utils.createExternalStoragePrivatePicture(ctx, getId() + "raw", data);
	}

	/**
	 * Grab the picture from the server. Write it to local storage if we can grab it
	 * @param ctx required if we want to store the data locally. Pass null to skip this step
	 * @param nTries number of tries before quitting, enter >= 1
	 * @return the picture data or null if not found or error
	 */
	private byte[] getPictureFromServer(Context ctx, int nTries){
		Log.i(Utils.APP_TAG, getId()+" getServerPicture");
		if (nTries <= 0)
			return null;		
		byte[] data;
		ParseFile file = getImageFile();
		try {
			if (!file.isDataAvailable())
				file.cancel();
			data = file.getData();
			if (data == null || data.length == 0){
				parse.fetchIfNeeded();
				file = getImageFile();
				file.cancel();
				data = file.getData();
			}
		} catch (ParseException e) {
			Log.e(Utils.APP_TAG, e.getMessage());
			return getPictureFromServer(ctx, nTries-1);
		}catch(Exception e){
			Log.e("TAG", Log.getStackTraceString(e));
			return getPictureFromServer(ctx, nTries-1);
		}
		if (data == null || data.length == 0)
			return getPictureFromServer(ctx, nTries-1);

		// write the data
		if (ctx != null)
			Utils.createExternalStoragePrivatePicture(ctx, getId(), data);

		return data;
	}

	/**
	 * Grab the picture from the device
	 * @param ctx
	 * @return The byte[] of the picture or null if unsuccessful
	 */
	private byte[] getPictureFromDevice(Context ctx){
		return Utils.getExternalStoragePrivatePicture(ctx, getId());
	}
	
	/**
	 * Return the path to the external storage of this picture
	 * @return
	 */
	public String getPathToExternalStorage(Context ctx){
		File file = Utils.getExternalStoreFile(getId());
		if (file.exists())
			return file.getAbsolutePath();
		else
			return null;
	}
	
	/**
	 * Return the path to the external storage of this picture
	 * @return
	 */
	public String getPathToExternalStorageRaw(Context ctx){
		File file = Utils.getExternalStoreFile(getId()+"raw");
		if (file.exists())
			return file.getAbsolutePath();
		else
			return null;
	}

	public String getId(){
		return parse.getObjectId();
	}

	/**
	 * Return the rating for this post. <br>
	 * On a background thread we also validate that the rating is correct
	 * @return
	 */
	public int getRating(){
		// verify on the background that we have the right rating
		validateRating();

		// return the rating
		return parse.getInt(RATING);
	}

	/**
	 * Validate that the rating stored in this item is correct. <br>
	 * That there are actually votes cast that match the rating in this object. <br>
	 * If not, then we fix the internal rating and save to server.
	 */
	private void validateRating(){
		//TODO: we are somehow sometimes uploading the same user same post two votes
		final CatPicture cat = this;
		final int internalRating = parse.getInt(RATING);
		new Thread(new Runnable() {
			public void run() {
				int rating = Vote.getRating(cat);
				int diff = rating - internalRating;
				if (diff != 0){
					try{
						cat.parse.put(RATING, rating);
						try {
							cat.save();
						} catch (ParseException e) {
							Log.e(Utils.APP_TAG, e.getMessage());
						}
					}catch(Exception e){
						Log.e(Utils.APP_TAG, e.getMessage());
					}
				}
			}
		}).start();
	}

	/**
	 * Return the number of comments for this post
	 * @return
	 */
	public int getNComments(){
		return parse.getInt(N_COMMENTS);
	}

	/**
	 * Get the user who created the post <br>
	 * *** Make sure any query inludes user, else this will be very slow ***
	 * @return
	 */
	public CatUser getUser(){
		return new CatUser(parse.getParseUser(USER));
	}

	/**
	 * Return the title of the post
	 */
	public String toString(){
		return getTitle();
	}

	public void save()
	throws ParseException{
		ParseFile file = getImageFile();
		if (!isImageFileSaved())
			file.save();
		ParseFile raw = this.rawFile;
		if (!isRawImageFileSaved())
			raw.save();
		parse.save();
	}
	
	/**
	 * Grab the image file stored in this object. First looks in parse data, and then looks in local data if null
	 * @return
	 */
	private ParseFile getImageFile(){
		Object o = parse.get(FILE);
		ParseFile file = null;
		if (o != null)
			file = (ParseFile) o;
		if (file == null)
			file = this.imageFile;
		return file;
	}
	
	/**
	 * Grab the raw image file stored in this object. First looks in parse data, and then looks in local data if null
	 * @return
	 */
	private ParseFile getRawFile(){
		Object o = parse.get(RAW);
		ParseFile file = null;
		if (o != null)
			file = (ParseFile) o;
		if (file == null)
			file = this.rawFile;
		return file;
	}

	public void saveInBackground(final SaveCallback saveCallback){
		// save the file first if we have to
		ParseFile file = getImageFile();
		ParseFile raw = getRawFile();
		SaveFilesTask task = new SaveFilesTask(file, raw, new SaveFilesCallback() {
			
			@Override
			public void onSave(CatPicture cat, boolean isSafeToSave, ParseException e) {
				
				// now save the object if it's safe to do so
				if (isSafeToSave && e == null){
					if (saveCallback != null)
						parse.saveInBackground(saveCallback);
					else
						parse.saveInBackground();
				}else if (e != null && isSafeToSave){
					Log.e(Utils.APP_TAG, "save to save returned true, but we have an exception, this shouldn't happend");
					saveCallback.done(e);
				}else if (e == null && !isSafeToSave){
					Log.e(Utils.APP_TAG, "save to save returned false, but we had no exception, this shouldn't happend");
					saveCallback.done(new ParseException(-100, "save to save returned false, but we had no exception, this shouldn't happend"));
				}else{
					saveCallback.done(e);
				}
			}
		});
		task.execute();
	}
	
	/**
	 * Allow access to internal parse, but only to use for query fields <br> ie .whereEquals(POST, cat.getParseForQuery)
	 * @return the internal parse object for this cat picture
	 */
	public ParseObject getParseForQuery(){
		return parse;
	}
	
	/**
	 * Put this cat picture into the given parseObject <br>
	 * this is equivalent to parseObject.put(key, cat.parse), however
	 * parse is not set to public, so we do not allow access to it except through this method 
	 * @param parseObject the parse object
	 * @param key the key to assign this vote to
	 */
	public void putCat(ParseObject parseObject, String key){
		parseObject.put(key, parse);
	}

	/**
	 * Upvote the rating for this picture
	 */
	private void upVote(){
		parse.increment(RATING);
	}

	/**
	 * Downvote the given picture
	 */
	private void downVote(){
		parse.increment(RATING, -1);
	}

	/**
	 * Assign the given vote for the current user to the picture
	 * @param decision
	 */
	public void vote(final boolean decision){

		Log.i(Utils.APP_TAG, "vote method called");
		// get the current vote
		Vote.getVote(Utils.getCurrentUser(), this, new VoteCallback() {

			@Override
			public void onDone(final Vote vote) {

				// get the current value of the vote
				boolean current = vote.getVote();
				Log.i(Utils.APP_TAG, "old vote = "+current);

				// only do something if it's changed
				if (decision != current){
					try{
						vote.setVote(decision);
						if (decision)
							upVote();
						else
							downVote();

					}catch(Exception e){
						Log.e(Utils.APP_TAG, e.getMessage());
						return;
					}
				}else
					return;

				// save the vote and picture in the background
				Log.i(Utils.APP_TAG, "vote starting save");
				saveInBackground(new SaveCallback() {

					@Override
					public void done(ParseException arg0) {
						Log.i(Utils.APP_TAG, "vote saved");
						// no error, so save the vote
						if (arg0 == null)
							vote.saveInBackground(null);
						else
							Log.e(Utils.APP_TAG, arg0.getMessage());

					}
				});			
			}
		});
	}

	/**
	 * Sets the sort order according to current default settings
	 * @param query
	 */
	private static void setQueryOrder(ParseQuery query){
		query.orderByDescending("createdAt");
	}

	/**
	 * Query the cat pictures
	 * @param skip The number of queries to skip
	 * @param nNewQueries the number of new queries to get
	 * @param findCallback the callback to perform when done
	 */
	public static void queryCats(int skip, int nNewQueries, FindCallback findCallback){

		// initialize query
		ParseQuery query = new ParseQuery(CatPicture.OBJECT_NAME);

		// set parameters of query
		setQueryOrder(query);
		query.setSkip(skip);
		query.setLimit(nNewQueries);
		query.include(USER);

		// now perform the query
		query.findInBackground(findCallback);
	}
	
	private class SaveFilesTask
	extends AsyncTask<Void, Void, Void>{

		private ParseFile image;
		private ParseFile raw;
		private SaveFilesCallback callback;
		public SaveFilesTask(ParseFile image, ParseFile raw, SaveFilesCallback callback){
			this.image = image;
			this.raw = raw;
			this.callback = callback;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			ParseException finalException = null;
			// first we try to save the raw file
			if (!isRawImageFileSaved()){
				for (int i = 0; i < DEFAULT_N_TRIES; i++){
					try {
						raw.save();
					} catch (ParseException e) {
						finalException = e;
						Log.e(Utils.APP_TAG, e.getMessage());
						continue;
					}
					finalException = null;
					break;
				}
			}
			
			// now the normal file
			if (!isImageFileSaved()){
				for (int i = 0; i < DEFAULT_N_TRIES; i++){
					try {
						image.save();
					} catch (ParseException e) {
						finalException = e;
						Log.e(Utils.APP_TAG, e.getMessage());
						continue;
					}
					if (isRawImageFileSaved())
						finalException = null;
					break;
				}
			}
			
			// now we put the data
			if (isRawImageFileSaved())
				parse.put(RAW, raw);
			if (isImageFileSaved())
				parse.put(FILE, image);
			callback.onSave(catPicture, catPicture.isSafeToSave(), finalException);
			
			return null;
		}
	}
	
	public interface SaveFilesCallback{
		/**
		 * This is called when we are done save the raw and image files
		 * @param cat The cat object
		 * @param isSafeToSave if we are now safe to save this object (because the files have been saved)
		 */
		public void onSave(CatPicture cat, boolean isSafeToSave, ParseException e);
	}
}
