package com.CatScan;

import java.util.List;

import com.CatScan.ServerObjects.CatPicture;
import com.CatScan.ServerObjects.Vote;
import com.CatScan.ServerObjects.Vote.VoteCallback;
import com.tools.images.ImageLoader;
import com.tools.images.ImageViewTouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class PicturesAdapter
extends BaseAdapter{

	//member variables 
    private List<CatPicture> data; 										// the list of cat data
    private static LayoutInflater inflater = null;						// inflator used for views
    public ImageLoader<String ,CatPicture, CatPicture> imageLoader;	// the imageloader used to load pictures
    private int pictureWindowWidth; 									// The size of the picture (will be square)
    private int pictureWindowHeight;
	private ImageSwitcher imageSwitcher = null;
	private CatScanActivity act;
    
	public PicturesAdapter(
			final CatScanActivity act,
			List<CatPicture> pictures,
			int pictureWindowWidth,
			int pictureWindowHeight){
        data = pictures;
        this.act = act;
        inflater = (LayoutInflater)act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageLoader = new ImageLoader<String, CatPicture, CatPicture>(
        		android.R.color.transparent,
        		pictureWindowWidth,
        		pictureWindowHeight,
        		true,
        		new ImageLoader.LoadImage<CatPicture, CatPicture>() {

        			@Override
        			public Bitmap onThumbnailLocal(CatPicture thumbnailData) {
        				return null;
        			}

        			@Override
        			public Bitmap onThumbnailWeb(CatPicture thumbnailData) {
        				return null;
        			}

        			@Override
        			public Bitmap onFullSizeLocal(CatPicture fullSizeData,
        					int desiredWidth, int desiredHeight) {
        				return null;
        			}

        			@Override
        			public Bitmap onFullSizeWeb(CatPicture fullSizeData,
        					int desiredWidth, int desiredHeight) {
        				Log.d(Utils.APP_TAG, fullSizeData.getTitle());
        				return fullSizeData.getPicture(act, desiredWidth, desiredHeight);
        			}

        			@Override
        			public void createThumbnailFromFull(CatPicture thumbnailData,
        					CatPicture fullSizeData) {
        				
        			}
        		});
		
       // imageLoader = new ImageLoaderZoomView(android.R.color.transparent, pictureWindowWidth, pictureWindowWidth);
        this.pictureWindowWidth = pictureWindowWidth;
        this.pictureWindowHeight = pictureWindowHeight;
	}
	
	/**
	 * Set an image switcher to be called when we switch images in the adapter
	 * @param switcher
	 */
	public void setImageSwitcher(ImageSwitcher switcher){
		imageSwitcher = switcher;
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
            params.height = pictureWindowHeight;
            params.width = pictureWindowWidth;
            image2.setLayoutParams(params);
        }

        // grab the items to display
        TextView comments = (TextView)vi.findViewById(R.id.comments);
        ImageViewTouch image = (ImageViewTouch)vi.findViewById(R.id.picture);
        TextView title = (TextView)vi.findViewById(R.id.title);
        TextView rating = (TextView)vi.findViewById(R.id.rating);
        final ImageView likeButton = (ImageView)vi.findViewById(R.id.thumbs_up);
        
        // get the data
        CatPicture cat = (CatPicture) getItem(position);
        
        // fill the items
        // determine who posted it
        String nameWhoPosted = cat.getUser().getName();
        if (nameWhoPosted == null || nameWhoPosted.length() == 0)
        	nameWhoPosted = cat.getNameWhoPosted();
        if (nameWhoPosted == null || nameWhoPosted.length() == 0)
        	nameWhoPosted = "Someone";
        
        // make the title
        String newline = System.getProperty("line.separator");
        String text = nameWhoPosted + " posted:";
        if (cat.getTitle().length() > 0)
        	text +=newline+ cat.getTitle();
        title.setText(text);
        
        // make the comments string
        int n = cat.getNComments();
        String str;
        if (n == 1)
        	str = "1 Comment";
        else
        	str = n + " Comments";
        comments.setText(str);
        
        // set the rating
        rating.setText(Integer.toString(cat.getRating()));
        
        // set the correct picture if we like the post
        likeButton.setImageDrawable(act.getResources().getDrawable(R.drawable.thumbs_up_gray));
        Vote.getVote(Utils.getCurrentUser(), cat, new VoteCallback() {
			
			@Override
			public void onDone(Vote vote) {
				if (vote.getVote()){
					likeButton.setImageDrawable(act.getResources().getDrawable(R.drawable.thumbs_up_selected));
					likeButton.invalidate();
				}
				else{
					likeButton.setImageDrawable(act.getResources().getDrawable(R.drawable.thumbs_up_normal));
					likeButton.invalidate();
				}
			}
		});       	

        // show the view
        imageLoader.DisplayImage(cat.getId(), cat, cat, image);
        
        // call the image switcher
        if (imageSwitcher != null)
        	imageSwitcher.onImageSwitch(position);
        
        // return the view
        return vi;
	}
	
	public interface ImageSwitcher{
		/**
		 * Called when the image is switched
		 * @param position The position in the gallery of the new image
		 */
		public void onImageSwitch(int position);
	}

}
