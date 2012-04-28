package com.CatScan;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import serverObjects.CatPicture;

import com.tools.TwoObjects;

import android.graphics.Bitmap;

public class MemoryCache {
	// private variables
    private HashMap<CatPicture, SoftReference<Bitmap>> cache = 
    	new HashMap<CatPicture, SoftReference<Bitmap>>(); 			// Hashmap holding thumbnail and full image bmp
    
    public Bitmap get(CatPicture cat){
        if(!cache.containsKey(cat))
            return null;
        SoftReference<Bitmap> ref=cache.get(cat);
        return ref.get();
    }
    
    public void put(CatPicture cat, Bitmap bitmap){
        cache.put(cat, new SoftReference<Bitmap>(bitmap));
    }

    public void clear() {
        cache.clear();
    }
}