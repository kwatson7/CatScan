package com.CatScan.ServerObjects;

import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.CatScan.Utils;
import com.CatScan.ServerObjects.Vote.VoteCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.tools.WidthHeight;

public class CatPicture{

	// the object name
	public static final String OBJECT_NAME = "CatPicture"; 		// the object name for the ParseObject
	
	// the fields
	private static final String FILE = "FILE";  					// The filename field for the picture
	private static final String TITLE = "TITLE"; 					// The title of the picture
	private static final String CAPTIONS = "CAPTIONS"; 				// The captions in the picture
	private static final String USER = "USER";						// The user who created the post
	private static final String RATING = "RATING"; 					// The rating this post has
	private static final String N_REPORTS = "N_REPORTS";			// The number of reports this post has
	private static final String N_COMMENTS = "N_COMMENTS"; 			// The number of comments this post has
	private static final String NAME_WHO_POSTED = "NAME_WHO_POSTED";
	
	// other private constants
	private static final String DEFAULT_FILE_NAME = "FILE_NAME.JPG";// The filename to use when uploading
	
	// private variables
	private ParseObject parse; 										// the parse object this wraps around

	/**
	 * Create a CatPicture
	 * @param picData the picture data returned from camera or picture selector 
	 * @param title The title of the picture
	 * @param captions An array list of all the captions that are on the picture
	 * @param nameWhoPosted the name attached to this post
	 * @param user the user who posted it
	 * @throws ParseException 
	 */
	public CatPicture(
			byte[] picData,
			String title,
			ArrayList<String> captions,
			String nameWhoPosted,
			CatUser user) throws ParseException{

		parse = new ParseObject(OBJECT_NAME);
		
		// put the data into the parse object
		parse.put(TITLE, title);
		parse.put(CAPTIONS, captions);
		parse.put(USER, user.getParse());
		parse.put(RATING, 0);
		parse.put(N_COMMENTS, 0);
		parse.put(N_REPORTS, 0);	
		parse.put(NAME_WHO_POSTED, nameWhoPosted);
		
		// save the file
		ParseFile file = new ParseFile(DEFAULT_FILE_NAME, picData);
		file.save();
		parse.put(FILE, file);
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
	 * This is slow, so it should be called from a background thread.
	 * @param ctx required to read from device, pass null if we want to just read from server
	 * @param desiredWidth, null to do nothing, input a number of pixels to resize image to this width
	 * @return the bitmap data
	 */
	public Bitmap getPicture(Context ctx, Integer desiredWidth){
		// try reading data from local device first
		byte[] data = null;
		if (ctx != null)
			data = getPictureFromDevice(ctx);
		
		// if the data is null, then read from server
		if (data == null)
			data = getPictureFromServer(ctx);
		if (data == null)
			return null;
		
		// resize the data
		if (desiredWidth != null)
			data = com.tools.Tools.resizeByteArray(data, new WidthHeight(desiredWidth, desiredWidth), "blackBars", null, 0f);
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	/**
	 * Grab the picture from the server. Write it to local storage if we can grab it
	 * @param ctx required if we want to store the data locally. Pass null to skip this step
	 * @return the picture data or null if not found or error
	 */
	private byte[] getPictureFromServer(Context ctx){
		ParseFile file = (ParseFile) parse.get(FILE);
		byte[] data;
		try {
			data = file.getData();
		} catch (ParseException e) {
			Log.e(Utils.APP_TAG, e.getMessage());
			e.printStackTrace();
			return null;
		}catch(Exception e){
			Log.e(Utils.APP_TAG, "get picture error");
			e.printStackTrace();
			return null;
		}
		if (data == null)
			return null;
		
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
		//validateRating();
		
		// return the rating
		return parse.getInt(RATING);
	}
	
	/**
	 * Validate that the rating stored in this item is correct. <br>
	 * That there are actually votes cast that match the rating in this object. <br>
	 * If not, then we fix the internal rating and save to server.
	 */
	private void validateRating(){
		final CatPicture cat = this;
		final int internalRating = parse.getInt(RATING);
		new Thread(new Runnable() {
			public void run() {
				int rating = Vote.getRating(cat);
				int diff = rating - internalRating;
				if (diff != 0){
					parse.increment(RATING, diff);
					try {
						cat.save();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
		parse.save();
	}
	
	public void saveInBackground(SaveCallback saveCallback){
		if (saveCallback != null)
			parse.saveInBackground(saveCallback);
		else
			parse.saveInBackground();
	}
	
	/**
	 * Save the picture to the server whenever it can.
	 */
	public void saveEventuallyDONTUSE(){
		parse.saveEventually();
	}
	
	public ParseObject getParse(){
		return parse;
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

		Vote.getVote(Utils.getCurrentUser(), this, new VoteCallback() {

			@Override
			public void onDone(final Vote vote) {
				// only do something if it's changed
				boolean current = vote.getVote();
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
				saveInBackground(new SaveCallback() {

					@Override
					public void done(ParseException arg0) {
						if (arg0 == null)
							vote.saveInBackground(null);
						else
							Log.e(Utils.APP_TAG, arg0.getMessage());
					}
				});			
			}
		});
	}
}
