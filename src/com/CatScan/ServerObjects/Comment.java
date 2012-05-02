package com.CatScan.ServerObjects;

import java.util.ArrayList;
import java.util.List;


import android.util.Log;

import com.CatScan.Utils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class Comment{

	// the object name
	private static final String OBJECT_NAME = "Comment"; 			// the object name for the ParseObject
	
	// the fields
	private static final String POST = "POST";  					// The filename field for the picture
	private static final String USER = "USER"; 						// The title of the picture
	private static final String COMMENT_STRING = "COMMENT_STRING";	// The captions in the picture
	
	// misc private variables
	private ParseObject parse; 										// The parse object this wraps around
	
	/**
	 * Create a Comment on a post
	 * @param post the CatPicture post this comment is for
	 * @param user the user who is commenting
	 * @param data the string data of the actual comment
	 */
	public Comment(
			CatPicture post,
			CatUser user,
			String data){
		parse = new ParseObject(OBJECT_NAME);
		
		// put the data into the parse object
		parse.put(POST, post);
		parse.put(USER, user);
		parse.put(COMMENT_STRING, data);
	}
	
	/**
	 * Create a Comment from a representative ParseObject
	 * @param parse
	 */
	public Comment(ParseObject parse){
		this.parse = parse;
	}
	
	/**
	 * Convert a list of ParseObjects to Comments
	 * @param parse
	 * @return
	 */
	public static List<Comment> convertList(List<ParseObject> parse){
		List<Comment> out = new ArrayList<Comment>();
		for (ParseObject item : parse)
			out.add(new Comment(item));
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
	 * Return the comment string
	 * @return
	 */
	public String getCommentString(){
		return parse.getString(COMMENT_STRING);
	}
	
	/**
	 * The comment string
	 */
	public String toString(){
		return getCommentString();
	}
	
	/**
	 * Return the number of comments for a post <br>
	 * This queries the server, so should be called from a background thread
	 * @param post The post to query on
	 * @return the number of comments, returns -1 if there was an error
	 */
	public static int getNComments(CatPicture post){
		// query to get all teh votes associated with the input post
		ParseQuery query = new ParseQuery(OBJECT_NAME);
		query.whereEqualTo(POST, post);
		int nComments = -1;
		try {
			nComments = query.count();
		} catch (ParseException e) {
			e.printStackTrace();
			Log.e(Utils.APP_TAG, e.getMessage());
		}
		return nComments;	
	}
	
	private ParseObject getParse(){
		return getParse();
	}
	
	/**
	 * Put this comment into the given parseObject <br>
	 * this is equivalent to parseObject.put(key, comment.parse), however
	 * parse is not set to public, so we do not allow access to it except through this method 
	 * @param parseObject the parse object
	 * @param key the key to assign this vote to
	 */
	public void putComment(ParseObject parseObject, String key){
		parseObject.put(key, parse);
	}

}
