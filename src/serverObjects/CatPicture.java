package serverObjects;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.tools.WidthHeight;

public class CatPicture{

	// the object name
	private static final String OBJECT_NAME = "CatPicture"; 		// the object name for the ParseObject
	
	// the fields
	private static final String FILE = "FILE";  					// The filename field for the picture
	private static final String TITLE = "TITLE"; 					// The title of the picture
	private static final String CAPTIONS = "CAPTIONS"; 				// The captions in the picture
	private static final String USER = "USER";						// The user who created the post
	private static final String RATING = "RATING"; 					// The rating this post has
	private static final String N_REPORTS = "N_REPORTS";			// The number of reports this post has
	private static final String N_COMMENTS = "N_COMMENTS"; 			// The number of comments this post has
	
	// other private constants
	private static final String DEFAULT_FILE_NAME = "FILE_NAME.JPG";// The filename to use when uploading
	
	// private variables
	private ParseObject parse; 										// the parse object this wraps around

	/**
	 * Create a CatPicture
	 * @param picData the picture data returned from camera or picture selector 
	 * @param title The title of the picture
	 * @param captions An array list of all the captions that are on the picture
	 * @throws ParseException 
	 */
	public CatPicture(
			byte[] picData,
			String title,
			ArrayList<String> captions,
			CatUser user) throws ParseException{

		parse = new ParseObject(OBJECT_NAME);
		
		// put the data into the parse object
		parse.put(TITLE, title);
		parse.put(CAPTIONS, captions);
		//parse.put(USER, user.getParse());
		parse.put(RATING, 0);
		parse.put(N_COMMENTS, 0);
		parse.put(N_REPORTS, 0);
		
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
		// verify objec type		
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
	 * Return the picture stored as a bitmap. Null if unsuccessful. <br>
	 * This is slow, so should be called from a background thread.
	 * @param desiredWidth, null to do nothing, input a number of pixels to resize image to this width
	 * @return
	 */
	public Bitmap getPicture(Integer desiredWidth){
		ParseFile file = (ParseFile) parse.get(FILE);
		byte[] data;
		try {
			data = file.getData();
		} catch (ParseException e) {
			Log.e("CatPicture", e.getMessage());
			e.printStackTrace();
			return null;
		}
		if (data == null)
			return null;
		// resize data first
		if (desiredWidth != null)
			data = com.tools.Tools.resizeByteArray(data, new WidthHeight(desiredWidth, desiredWidth), "blackBars", null, 0f);
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	/**
	 * Return the rating for this post.
	 * @return
	 */
	public int getRating(){
		return parse.getInt(RATING);
	}
	
	/**
	 * Return the number of comments for this post
	 * @return
	 */
	public int getNComments(){
		return parse.getInt(N_COMMENTS);
	}
	
	/**
	 * Get the user who created the post
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
		parse.saveInBackground(saveCallback);
	}
	
	public ParseObject getParse(){
		return parse;
	}
}
