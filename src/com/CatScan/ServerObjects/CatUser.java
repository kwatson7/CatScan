package com.CatScan.ServerObjects;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.CatScan.Utils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

public class CatUser{
	
	// the fields
	private static final String NAME = "NAME";  					// The filename field for the picture
	
	// private variables
	private ParseUser parse; 										// the parse object
	
	/**
	 * Create a User
	 * @param name the name of the user
	 */
	public CatUser(
			String name){
		super();
		
		if (name == null)
			name = "";
		// put the data into the parse object
		parse.put(NAME, name);
	}
	
	/**
	 * Create a User from a representative ParseObject
	 * @param parse
	 */
	public CatUser(ParseUser parse){
		if (parse == null)
			parse = new ParseUser();
		this.parse = parse;
		
		// store data
	//	String name = parse.getString(NAME);
	//	if (name == null)
	//		name = "";
	//	parse.put(NAME, name);
	}
	
	/**
	 * Convert a list of ParseObjects to Users
	 * @param parse
	 * @return
	 */
	public static List<CatUser> convertList(List<ParseUser> parse){
		List<CatUser> out = new ArrayList<CatUser>();
		for (ParseUser item : parse)
			out.add(new CatUser(item));
		return out;
	}
	
	/**
	 * Return the name of the user
	 * @return
	 */
	public String getName(){
		if (parse == null)
			return "";
		String name = parse.getString(NAME);
		if (name == null)
			name = "";
		return name;
	}
	
	/**
	 * Set the user's name. Null will be converted to ""
	 * @param name The name
	 */
	public void setName(String name){
		if (name == null)
			name = "";
		parse.put(NAME, name);
	}
	
	/**
	 * Save the user to the server on this thread
	 * @return true if successful, false otherwise
	 */
	public boolean save(){
		try {
			parse.save();
			return true;
		} catch (ParseException e) {
			Log.e(Utils.APP_TAG, e.getMessage());
			return false;
		}
	}
	
	/**
	 * The user name
	 */
	public String toString(){
		return getName();
	}
	
	/**
	 * Allow access to internal parse, but only to use for query fields <br> ie .whereEquals(User, user.getParseForQuery)
	 * @return the internal parse object for this user
	 */
	public ParseUser getParseForQuery(){
		return parse;
	}
	
	/**
	 * Put this user into the given parseObject <br>
	 * this is equivalent to parseObject.put(key, user.parse), however
	 * parse is not set to public, so we do not allow access to it except through this method 
	 * @param parseObject the parse object
	 * @param key the key to assign this vote to
	 */
	public void putUser(ParseObject parseObject, String key){
		parseObject.put(key, parse);
	}
}
