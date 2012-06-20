package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.io.File;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.CatScan.ServerObjects.CatUser;
import com.parse.ParseUser;
import com.tools.SuccessReason;

public class Utils {

	public static final String APP_TAG = "CatScan";
	public static final char pathsep = '/';
	public static final String FOLDER_NAME = "Cat Scan Pictures";
	public static final String CAMERA_DATA_KEY = "data";
	public static final int IMAGE_QUALITY = 90;
	public static final String APP_URL = "https://market.android.com/search?q=pname:com.CatScan";
	
	/**
	 * Return the current User. If there is no user, then an anonymous one is created in the background.
	 * Null will be returned when there is no user.
	 * @return
	 */
	public static CatUser getCurrentUser(){
		return new CatUser(ParseUser.getCurrentUser());
	}
	
	/**
	 * Write picture data to the private external storage of this app.
	 * @param ctx The context required to write data
	 * @param name The name of the file (.jpg will be appended)
	 * @param data The byte data to write
	 * @return the filepath or null if no successfull save
	 */
	public static String createExternalStoragePrivatePicture(Context ctx, String name, byte[] data){ 

		try{
			File file = getExternalStoreFile(name);

			SuccessReason out = com.tools.ImageProcessing.saveByteDataToFile(
					ctx.getApplicationContext(),
					data,
					false,
					file.getAbsolutePath(),
					null,
					false);
			if (!out.getSuccess()){
				Log.d(Utils.APP_TAG, "file not saved to external storage" + out.getReason());
				return null;
			}
			return file.getAbsolutePath();
		}catch(Exception e){
			Log.e(Utils.APP_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
	
	/**
	 * Return a string that is the path to top level app picture storage.
	 * @return
	 */
	private static String getExternalStorageDirectory(){

		// grab the path
		File external = Environment.getExternalStorageDirectory();
		String path = external.getAbsolutePath();

		// now put / if not there
		if (path.length() > 1 && path.charAt(path.length()-1) != pathsep)
			path += pathsep;

		// now add this app directory
		path += FOLDER_NAME + pathsep;
		
		// now make the folder
		File dir = new File(path);
		if(!dir.exists() && !dir.mkdirs())
			Log.e(Utils.APP_TAG, "could not create directory");

		return path;
	}
	
	/**
	 * Read file from external storage
	 * @param ctx Context required to read data
	 * @param name The picture name (.jpg will be appended)
	 * @return The byte[] of the picture or null if not found
	 */
	public static byte[] getExternalStoragePrivatePicture(Context ctx, String name){
		try{
			File file = getExternalStoreFile(name);
			if (!file.exists())
				return null;
			Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			return byteArray;
		}catch(Exception e){
			Log.e(Utils.APP_TAG, Log.getStackTraceString(e));
			return null;
		}
	}
	
	/**
	 * The path to the picture with this name.
	 * @param name
	 * @return the file stored externally
	 */
	public static File getExternalStoreFile(String name){
		File path = new File(getExternalStorageDirectory());
		File file = new File(path, name + ".jpg");
		return file;
	}
}
