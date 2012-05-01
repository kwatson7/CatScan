package com.CatScan.ServerObjects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.util.Log;

import com.CatScan.Utils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

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
		parse.put(POST, post.getParse());
		parse.put(USER, user.getParse());
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
		// query to get all the votes associated with the input post
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(POST, post.getParse());
		// Include the post data with each comment
		query.include(POST);
		query.include(USER);
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
	
	/**
	 * This will grab the vote on a background thread that relates to the given user and cat picture post
	 * @param user The user in question
	 * @param picture The picture in question
	 * @param callback Callback to run when the call finishes
	 */
	public static void getVote(
			final CatUser user,
			final CatPicture picture,
			final VoteCallback callback){
		
		// query the database to get any votes that connect the user and picture
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(POST, picture.getParse());
		query.whereEqualTo(USER, user.getParse());
		query.include(POST);
		query.include(USER);
		query.findInBackground(new FindCallback() {

			@Override
			public void done(List<ParseObject> data, ParseException e) {
				Vote vote = new Vote(picture, user, false);
				if (e == null){
					if (data.size() > 0){
						vote = new Vote(data.get(0));
					}
				}else{
					e.printStackTrace();
					Log.e(Utils.APP_TAG, e.getMessage());
				}
				
				// post result back to callback
				callback.onDone(vote);
			}
		});
	}
	
	public static HashSet<String> getPostIdsUserLikes(
			CatUser user,
			final LikedPostsCallback likedPostsCallback){
		
		// query the database to get any votes that connect the user
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(USER, user.getParse());
		query.include(POST);
		
		// no callback, then just address search on this thread
		if (likedPostsCallback == null){
			// query to find the votes
			List<Vote> votesArray = null;
			try {
				votesArray = Vote.convertList(query.find());
			} catch (ParseException e) {
				Log.e(Utils.APP_TAG, e.getMessage());
				e.printStackTrace();
				return null;
			}

			// now find all the posts that we liked
			HashSet<String> posts = new HashSet<String>();
			for (Vote vote : votesArray)
				if (vote.getVote())
					posts.add(vote.getPost().getId());

			// return the value
			return posts;
		}else{
			query.findInBackground(new FindCallback() {
				
				@Override
				public void done(List<ParseObject> data, ParseException e) {
					if (e != null){
						Log.e(Utils.APP_TAG, e.getMessage());
						e.printStackTrace();
						likedPostsCallback.onDone(null);
					}else{
						if (data == null){
							likedPostsCallback.onDone(new HashSet<String>(0));
						}else{
							List<Vote> votesArray = Vote.convertList(data);
							
							// now find all the posts that we liked
							HashSet<String> posts = new HashSet<String>();
							for (Vote vote : votesArray)
								if (vote.getVote())
									posts.add(vote.getPost().getId());
							likedPostsCallback.onDone(posts);
						}	
					}
				}
			});
		}
		return null;
			
	}
	
	/**
	 * Set the vote value 
	 * @param value
	 */
	public void setVote(boolean value){
		parse.put(VOTE_VALUE, value);
	}
	
	public interface VoteCallback{
		/**
		 * Called when we return from the server the result
		 * @param vote The vote value returned
		 */
		public void onDone(Vote vote);
	}
	
	public interface LikedPostsCallback{
		/**
		 * Called when we are done finding all the posts a user liked
		 * @param list of postIds the user liked
		 */
		public void onDone(HashSet<String> likedPostIds);
	}

	public ParseObject getParse(){
		return parse;
	}
	
	/**
	 * Save the vote to the server whenever it can.
	 */
	public void saveEventuallyDONTUSE(){
		parse.saveEventually();
	}
	
	public void saveInBackground(SaveCallback saveCallback){
		if (saveCallback != null)
			parse.saveInBackground(saveCallback);
		else
			parse.saveInBackground();
	}
}
