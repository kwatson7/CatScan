package serverObjects;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.CatScan.Utils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class Vote{

	// the object name
	private static final String OBJECT_NAME = "Vote"; 				// the object name for the ParseObject
	
	// the fields
	private static final String POST = "POST";  					// The filename field for the picture
	private static final String USER = "USER"; 						// The title of the picture
	private static final String VOTE_VALUE = "VOTE_VALUE";			// The captions in the picture
	
	// misc private variables
	private ParseObject parse; 										// The parse object this wraps around
	
	/**
	 * Create a vote on a post
	 * @param post the post this vote is for
	 * @param user The user who voted
	 * @param voteValue true to vote it up, and false to do nothing
	 */
	public Vote(
			CatPicture post,
			CatUser user,
			boolean voteValue){
		parse = new ParseObject(OBJECT_NAME);
		
		// put the data into the parse object
		parse.put(POST, post);
		parse.put(USER, user);
		parse.put(VOTE_VALUE, voteValue);
	}
	
	/**
	 * Create a vote from a representative ParseObject
	 * @param parse
	 */
	public Vote(ParseObject parse){
		this.parse = parse;
	}
	
	/**
	 * Convert a list of ParseObjects to Votes
	 * @param parse
	 * @return
	 */
	public static List<Vote> convertList(List<ParseObject> parse){
		List<Vote> out = new ArrayList<Vote>();
		for (ParseObject item : parse)
			out.add(new Vote(item));
		return out;
	}
	
	/**
	 * Return the post linked to this comment
	 * @return
	 */
	public CatPicture getPost(){
		return new CatPicture(parse.getParseObject(POST));
	}
	
	/**
	 * Return the user who created this comment
	 * @return
	 */
	public CatUser getUser(){
		return new CatUser(parse.getParseUser(USER));
	}
	
	/**
	 * Return the vote value
	 * @return
	 */
	public boolean getVote(){
		return parse.getBoolean(VOTE_VALUE);
	}
	
	/**
	 * Return the rating for a given post. <br>
	 * This queries the server, so this should be called from the background
	 * @param post
	 * @return The rating for this post, -1 if there is an exception
	 */
	public static int getRating(CatPicture post){
		// query to get all teh votes associated with the input post
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(POST, post);
		List<Vote> votes;
		try {
			votes = Vote.convertList(query.find());
		} catch (ParseException e) {
			e.printStackTrace();
			Log.e(Utils.APP_TAG, e.getMessage());
			return -1;
		}
		
		// count the votes
		int rating = 0;
		for (Vote item : votes){
			if (item.getVote())
				rating++;
		}
		
		return rating;	
	}
	
	public ParseObject getParse(){
		return parse;
	}
}
