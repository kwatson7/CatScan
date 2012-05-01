package com.CatScan.ServerObjects;

import java.util.ArrayList;
import java.util.List;

import com.parse.ParseObject;

public class Report{

	// the object name
	private static final String OBJECT_NAME = "Report"; 			// the object name for the ParseObject
	
	// the fields
	private static final String POST = "POST";  					// The filename field for the picture
	private static final String USER = "USER"; 						// The title of the picture
	private static final String VOTE_VALUE = "VOTE_VALUE";			// The captions in the picture
	
	// misc private variables
	private ParseObject parse; 										// The parse object this wraps around
	
	/**
	 * Create a Report
	 * @param post The post this is linked to
	 * @param user The user who is making this report
	 * @param voteValue, true to report violation, and false to not report
	 */
	public Report(
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
	 * Create a Report from a representative ParseObject
	 * @param parse
	 */
	public Report(ParseObject parse){
		this.parse = parse;
	}
	
	/**
	 * Convert a list of ParseObjects to Reports
	 * @param parse
	 * @return
	 */
	public static List<Report> convertList(List<ParseObject> parse){
		List<Report> out = new ArrayList<Report>();
		for (ParseObject item : parse)
			out.add(new Report(item));
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
	
	public ParseObject getParse(){
		return parse;
	}
}
