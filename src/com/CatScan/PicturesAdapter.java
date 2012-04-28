package com.CatScan;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.List;

import serverObjects.CatPicture;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class PicturesAdapter
extends BaseAdapter{

    private List<CatPicture> data;
    private static LayoutInflater inflater=null;
    public ImageLoaderZoomView imageLoader; 
    private int pictureWindowWidth;
	
	public PicturesAdapter(Activity act, List<CatPicture> pictures, int pictureWindowWidth){
        data = pictures;
        inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoaderZoomView(android.R.color.transparent, pictureWindowWidth, pictureWindowWidth);
        this.pictureWindowWidth = pictureWindowWidth;
	}
	
	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// inflate a new view if we have to
		View vi=convertView;
        if(convertView==null){
            vi = inflater.inflate(R.layout.picture_item, null);
            ImageView image2 = (ImageView)vi.findViewById(R.id.picture);
            FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) image2.getLayoutParams();
            params.height = pictureWindowWidth;
            params.width = pictureWindowWidth;
            image2.setLayoutParams(params);
        }

        // grab the items to display
        TextView comments = (TextView)vi.findViewById(R.id.comments);
      //  ImageView image = (ImageView)vi.findViewById(R.id.picture);
        ImageViewTouch image = (ImageViewTouch)vi.findViewById(R.id.picture);
        TextView title = (TextView)vi.findViewById(R.id.title);
        TextView rating = (TextView)vi.findViewById(R.id.rating);
        
        // get the data
        CatPicture cat = (CatPicture) getItem(position);
        
        // fill the items
        title.setText(cat.getTitle());
        int n = cat.getNComments();
        String str;
        if (n == 1)
        	str = "1 Comment";
        else
        	str = n + " Comments";
        comments.setText(str);
        rating.setText(Integer.toString(cat.getRating()));

        // show the view
        imageLoader.DisplayImage(cat, image);
        
        // return the view
        return vi;
	}

}
