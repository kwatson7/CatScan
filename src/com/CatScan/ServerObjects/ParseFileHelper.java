package com.CatScan.ServerObjects;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

import android.util.Log;

import com.CatScan.Utils;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.tools.Tools;

public class ParseFileHelper {
	
	// member variables
	private ParseFile parsefile; 										// the internal parse file of this helper
	private ParseObject fileHolder; 									// the internal object that this parse file resides within
	private ArrayList<ParseFileCallback> callbackStack =  				// the stack of callbacks to call when parse file is done retrieving
		new ArrayList<ParseFileHelper.ParseFileCallback>();
	private WeakReference<byte[]> fileData = null;
	private ParseException parseException = null;	
	private Date date = null;
	
	// constants
	private static final long TIMEOUT = 10000;  						// timeout to wait for finish
	
	/**
	 * Use this helper to grab data from a parse file. It handles multiple queries to the file without erroring out
	 * @param fileHolder The object that holds this file
	 * @param parseFile The actual file
	 */
	public ParseFileHelper(ParseObject fileHolder, ParseFile parseFile){
		if (fileHolder == null || parseFile == null)
			throw new IllegalArgumentException("neither fileHolder nor parseFile cannot be null");
		
		// store the data
		this.fileHolder = fileHolder;
		this.parsefile = parseFile;
	}
	
	/**
	 * Get the file data. This will handle multiple calls to the server and wait until the first call is finished.
	 * We only wait 30s which is the timeout length, and then return null or []
	 * @return The byte data of file, may be null or [].
	 * @throws ParseException
	 */
	public byte[] getData()
	throws ParseException{
		String str = Tools.randomString(5);
		Log.i(Utils.APP_TAG, parsefile.getName() + " " + str + " getData called");
		
		// launch the call
		if (fileData == null || fileData.get() == null || fileData.get().length == 0)
			getDataFromServer();
		
		// we may error out of the call above because of other queries on other threads, so we will wait to be notified
		while((fileData == null || fileData.get() == null || fileData.get().length == 0) &&
				parseException == null){
			try {
				synchronized (this) {
					
					Log.i(Utils.APP_TAG, parsefile.getName() +" "+ str+ " wait ");
					Date date = new Date();
					this.wait(TIMEOUT);
					Log.i(Utils.APP_TAG, parsefile.getName() +" "+ str+ " wait finished");
					if ((new Date()).getTime() - date.getTime() > TIMEOUT && ((fileData == null || fileData.get() == null || fileData.get().length == 0) &&
							parseException == null)){
						Log.i(Utils.APP_TAG, parsefile.getName() +" "+ str+ " from server re-called");
						parsefile.cancel();
						getDataFromServer();
					}
				}
			} catch (InterruptedException e) {
				Log.e(Utils.APP_TAG, Log.getStackTraceString(e));
			}
		}
		if (fileData == null || fileData.get() == null || fileData.get().length == 0)
			Log.e(Utils.APP_TAG, "timeout");
		
		// we have an exception
		if (parseException != null){
			ParseException e = parseException;
			parseException = null;
			throw e;
		}
		
		// return the data
		if (fileData != null)
			return fileData.get();
		else
			return null;
	}
	
	/**
	 * Get the file data, but will not wait if we cannot immediately get it from the server.
	 * We only wait 30s which is the timeout length, and then return null or []
	 * @return The byte data of file, may be null or [].
	 * @throws ParseException
	 */
	public byte[] getDataNoWait()
	throws ParseException{
		String str = com.tools.Tools.randomString(5);
		Log.i(Utils.APP_TAG, parsefile.getName() + " " + str + " getDataNoWait called");
		
		// launch the call
		if (fileData == null || fileData.get() == null || fileData.get().length == 0)
			getDataFromServer();
		
		int l = 0;
		if (fileData != null && fileData.get() != null)
			l = fileData.get().length;
		Log.i(Utils.APP_TAG, parsefile.getName() + " " + str + " getDataNoWaitServer finished with length = " + l);
		
		// we have an exception
		if (parseException != null){
			ParseException e = parseException;
			parseException = null;
			throw e;
		}
		
		// return the data
		if (fileData != null)
			return fileData.get();
		else
			return null;
	}
	
	/**
	 * grab data from the file on a background thread, and call the callback when finished.
	 * If null passed, then no callback, but data still grabbed.
	 * @param callback
	 */
	public synchronized void getDataInBackground(ParseFileCallback callback){
		// add callback to stack
		if (callback != null)
			callbackStack.add(callback);
		
		// grab data in other thread
		new Thread(new Runnable() {
			public void run() {

				try {
					getData();
				} catch (ParseException e) {
					// this is caught here, but will be sent to callback
					Log.e("TAG", Log.getStackTraceString(e));
				}
			}
		}).start();	
	}
	
	/**
	 * Grab the most recent parseException and then null it out, so on next call we get null
	 * @return The most recent parseException
	 */
	private ParseException popParseExceptionDONTUSE(){
		ParseException e = parseException;
		parseException = null;
		return e;
	}
	
	/**
	 * Grab the data from the server and store internally in this class. 
	 * This may error out if we are already querying server. See getData to avoid this.
	 * @return
	 */
	private void getDataFromServer(){
		
		// null the exception
		parseException = null;
		
		// initialze data
		byte[] data = null;
		
		try{
			// fetch data 
		//	fileHolder.fetchIfNeeded();
		
			// grab the data from the file
			data = parsefile.getData();
		} catch (ParseException e) {
			
			// we have a parse error, stop execution and inform all requestors of data
			parseException = e;
			Log.e(Utils.APP_TAG, e.getMessage());
			synchronized (this) {
				this.notifyAll();
			}
			return;
		}catch(java.lang.ArrayIndexOutOfBoundsException e){
			Log.d(Utils.APP_TAG, Log.getStackTraceString(e));
			getDataFromServer();
			return;
		}catch(Exception e){
			Log.d(Utils.APP_TAG, Log.getStackTraceString(e));
			if (date == null){
				date = new Date();
			}else{
				Date newDate = new Date();
				if(newDate.getTime() - date.getTime() > TIMEOUT){
					parsefile.cancel();
					date = newDate;
					getDataFromServer();
				}
			}
			return;
		}
		
		// we are done grabbing data, so store and notify
		if (data != null && data.length != 0)
			fileData = new WeakReference<byte[]>(data);

		// also call callbacks
		callAllCallbacks(parseException);
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	/**
	 * Call all the waiting callbacks oldest first.
	 */
	private void callAllCallbacks(ParseException e){
		
		// the data
		byte[] data = null;
		if (fileData != null)
			data = fileData.get();
		
		// call the callbacks
		for (ParseFileCallback callback : callbackStack){
			callback.onParseFileRetrieved(data, e);
		}
		
		// clear stack
		callbackStack.clear();
	}
	
	public interface ParseFileCallback{
		/**
		 * This will be called when the data is finally retrieved
		 * @param fileData the file data
		 * @param e any ParseException encountered
		 */
		public void onParseFileRetrieved(byte[] fileData, ParseException e);
	}

}
