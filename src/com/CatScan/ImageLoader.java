package com.CatScan;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import serverObjects.CatPicture;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.ImageView;

public class ImageLoader {
    
	// private variables
    private MemoryCache memoryCache = new MemoryCache(); 							// This stores the bitmaps in memory
    private Map<ImageView, PhotoToLoad> imageViews =
    	Collections.synchronizedMap(new WeakHashMap<ImageView, PhotoToLoad>()); 	// keeps track of links between views and pictures	
    private ExecutorService executorService;  										// run the threads
    private final int stub_id;	 													// The resource id of the default image
    private final int desiredWidth; 												// The desired width of full size image
    private final int desiredHeight; 												// The desired screen height of full size iamge
    
    // constants
    private static final int MAX_THREADS = 15; 										// max threads to spawn
    
    /**
     * Create an image loader that asynchonously loads images both from file and the webs. <br>
     * See stopThreads and restartThreads
     * @param defaultImageId The resource id of the default image to display when no data is available
     * @param desiredWidth The max desired width of the full size image on screen
     * @param desiredHeight The max desired height of the full size image on screen
     */
    public ImageLoader(int defaultImageId, int desiredWidth, int desiredHeight){
        executorService=Executors.newFixedThreadPool(MAX_THREADS);
        stub_id = defaultImageId;
        this.desiredHeight = desiredHeight;
        this.desiredWidth = desiredWidth;
    }
    
    /**
     * Launch async runnable to show this image
     * @param cat 	The cat object
     * @param imageView The imageView to put the image
     */
    public void DisplayImage(
    		CatPicture cat,
    		ImageView imageView)
    {
    	
    	// create the object containing all the relevant data
    	PhotoToLoad data =
    		new PhotoToLoad(cat, imageView);
    	
    	// store the links
        imageViews.put(imageView, data);
        
        // attempt to access cached full picture
        Bitmap bitmap = null;
        
        // no full picture, so queue the photo loader, and check for thumbnail
        bitmap = memoryCache.get(cat);
        if (bitmap == null)
        	queuePhoto(data);

        // see if we have a bitmap to access
        if(bitmap!=null){
            imageView.setImageBitmap(bitmap);
        } 
        // otherwise just show the default image
        else
        {
            imageView.setImageResource(stub_id);
        }
    }
        
    /**
     * Add this photo to the download queue
     * @param url The url of the file to download
     * @param imageView The imageView to put the bitmap
     */
    private void queuePhoto(PhotoToLoad data)
    {
    	if (executorService != null)
    		executorService.submit(new PhotosLoader(data));
    }
    
    /**
     * Read the picture from the cat object and if it exists return null if unsuffessful <br>
     * Make sure to NOT call on main UI thread
     * @param path
     * @return
     */
    private Bitmap getPicture(CatPicture cat){
    	return cat.getPicture(desiredWidth);

    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public CatPicture cat;
        public ImageView imageView;
       
        public PhotoToLoad(
        		CatPicture cat,
        		ImageView i){
            this.cat = cat;
            imageView=i;
        }
    }
    
    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad=photoToLoad;
        }

        @Override
        public void run() {
        	// this is a recycle view, so don't do anything
        	if(imageViewReused(photoToLoad))
        		return;

        	// should we grab the thumbnail first?
        	Bitmap bmp = getPicture(photoToLoad.cat);
        	memoryCache.put(photoToLoad.cat, bmp);

        	// recycled view
        	if(imageViewReused(photoToLoad))
        		return;

        	// load the bitmap on the ui thread
        	if (bmp != null){
        		BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
        		Activity a=(Activity)photoToLoad.imageView.getContext();
        		a.runOnUiThread(bd);
        	}
        }
    }

    /**
     * Check if this imageView is being re-used
     * @param photoToLoad
     * @return boolean if true
     */
    boolean imageViewReused(PhotoToLoad photoToLoad){
        CatPicture cat = imageViews.get(photoToLoad.imageView).cat;
        if(cat==null || !cat.equals(photoToLoad.cat))
            return true;
        return false;
    }
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;
        public BitmapDisplayer(Bitmap b, PhotoToLoad p){bitmap=b;photoToLoad=p;}
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null){
                photoToLoad.imageView.setImageBitmap(bitmap);
            }else{
                photoToLoad.imageView.setImageResource(stub_id);
            }
        }
    }

    public void clearCache() {
        memoryCache.clear();
     //   fileCache.clear();
    }
    
    /**
     * Stop background threads, usually call this on activity onPause
     */
    public void stopThreads(){
    	executorService.shutdown();
    	executorService = null;
    }
    
    /**
     * Restart running threads. Usually call this on activity onResume();
     * If threads already running, null operation.
     */
    public void restartThreads(){
    	if (executorService == null)
    		executorService=Executors.newFixedThreadPool(MAX_THREADS);
    }
    
    /**
     * Return the memory cache.<br>
     * **** This should only be used when storing this memory cache to be passed into again useing restoreMemoryCache
     * for example on orientation changes *****
     * @return
     */
    public MemoryCache getMemoryCache(){
    	return memoryCache;
    }
    
    /**
     * Set the memory cache to this new value, clearing old one.
     * @see getMemoryCache.
     * @param mem
     */
    public void restoreMemoryCache(MemoryCache mem){
    	if (memoryCache != null)
    		memoryCache.clear();
    	memoryCache = mem;
    }
}
