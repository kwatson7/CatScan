package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.CatScan.ServerObjects.CatPicture;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.tools.ColorPickerDialog;
import com.tools.CustomActivity;
import com.tools.ImageCapture;

public class AddCaptionsToImage
extends CustomActivity{

	// constants
    private static int[] DEFAULT_COLOR = {255, 255, 255};
    private static int NULL_COLOR = Color.argb(0, 0, 0, 0);
    
	// member variables
	private Bitmap photo; 							// The actual image data
	private int[] currentColor = DEFAULT_COLOR;
	private ArrayList<String> captionsArray = new ArrayList<String>();

	// pointers to graphics
	private ImageView photoView; 					// The image showing
	private Button postPicture;
	private EditText title;
	private ViewGroup screen;
	private EditText captionEditor;
	private ViewGroup EditorView; 
	
	// motion parameters
	private View selected_item = null;
    private int offset_x = 0;
    private int offset_y = 0;

	@Override
	protected void onCreateOverride(Bundle savedInstanceState) {

		// read the photo passed in
		//TODO:this is slow, should be done in background
		photo = ImageCapture.getBitmap(this, getIntent().getExtras(), true);

		// get configuration data and copy over any old data from old configuration.
		ConfigurationProperties config = (ConfigurationProperties) getLastNonConfigurationInstance();
		if (config != null && config.customData != null){
			ConfigurationPropertiesCustom data = (ConfigurationPropertiesCustom) config.customData;
			if (data != null){
				if (data.photo != null)
					photo = data.photo;
			}
		}
		
		// no photo
		if (photo == null){
			Toast.makeText(ctx, "Photo Problem", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// initialize the layout
		initializeLayout();
		
		setupCaptionMovement();
	}

	@Override
	public void onAsyncExecute(int requestCode, AsyncTypeCall asyncTypeCall,
			Object data) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	protected void initializeLayout() {
		
		// make full screen
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		// set the main content
		setContentView(R.layout.edit_captions);

		// grab the pointers
		photoView = (ImageView) findViewById(R.id.photo);
		title = (EditText) findViewById(R.id.title);
		postPicture = (Button) findViewById(R.id.postPicture);
		screen = (ViewGroup) findViewById(R.id.screen);
		captionEditor = (EditText) findViewById(R.id.captionEditor);
		EditorView = (ViewGroup) findViewById(R.id.EditorView);

		// set the photo
		photoView.setImageBitmap(photo);
		
		// close keyboard when touching image
		photoView.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (captionEditor.hasFocus() || title.hasFocus())
					com.tools.Tools.hideKeyboard(ctx, captionEditor);
				//final View dummy = findViewById(R.id.dummyView);
				//(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
				return false;
			}
		});
	}
	
	/**
	 * Select the color and then place the caption
	 */
	private void changeColor(){
		
		// setup color picker
		ColorPickerDialog colorPickerDialog =
			new ColorPickerDialog(ctx, new ColorPickerDialog.OnColorChangedListener() {

				@Override
				public void colorChanged(int color, int red, int green, int blue) {
					// store the color
					currentColor[0] = red;
					currentColor[1] = green;
					currentColor[2] = blue;
					
					// launch makeCaption
					makeCaption();
				}
			},
			currentColor[0],
			currentColor[1],
			currentColor[2]);
		
		// show it
		colorPickerDialog.show();
	}
	
	/**
	 * Create a textView from the given caption and put it in center
	 * @param view
	 */
	public void makeCaptionClicked(View view){
		// first launch the color chooser
		changeColor();
	}
	
	/**
	 * Create a textView from the given caption and put it in center
	 */
	private void makeCaption(){
		
		// read the caption to create
		String captionText = captionEditor.getText().toString();
		
		// create the caption
		ViewGroup holder = (ViewGroup)((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
			inflate(R.layout.caption_text_view, null);
		TextView caption = (TextView)holder.findViewById(R.id.captionTextView);
		
		// set the text of the caption
		caption.setText(captionText.toUpperCase());
		
		// set the color
		caption.setTextColor(Color.rgb(currentColor[0], currentColor[1], currentColor[2]));
		caption.setTypeface(Typeface.SANS_SERIF);
		
		// make it moveable
		setupViewForMovement(caption);
		
		// erase the text in cpationEditor
		captionEditor.setText("");
		
		// add the view to the main window
		holder.removeView(caption);
		screen.addView(caption);
		
		// hide the keyboard
		com.tools.Tools.hideKeyboard(ctx, captionEditor);
		final View dummy = findViewById(R.id.dummyView);
		(new Handler()).post (new Runnable() { public void run() { dummy.requestFocus(); } });
		
		// keep track of captions
		captionsArray.add(captionText);
		
	}

	/**
	 * Post the picture to the server
	 * @param view
	 */
	public void postPicture(View view){
		
		// save the view to a bitmap
		ArrayList<View> hiddenViews = new ArrayList<View>(2);
		hiddenViews.add(title);
		hiddenViews.add(EditorView);
		Bitmap picture = 
			com.tools.Tools.saveViewToBitmap(
				screen,
				hiddenViews,
				screen,
				NULL_COLOR);
		if (picture == null){
			Toast.makeText(ctx, "Picture cold not be saved", Toast.LENGTH_LONG).show();
			return;
		}
		
		// read the title
		String titleString = title.getText().toString();

		// convert bitmap to byte[]
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		photo.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] rawArray = stream.toByteArray();
		ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
		picture.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
		byte[] picArray = stream2.toByteArray();
		

		// create the cat post
		CatPicture cat = new CatPicture(
				picArray,
				rawArray,
				titleString,
				captionsArray,
				Prefs.getName(ctx),
				Utils.getCurrentUser());
		
		// now save the cat picture
		cat.saveInBackground(new SaveCallback() {
			
			@Override
			public void done(ParseException e) {
				if (e != null){
					try{
						Toast.makeText(ctx, "Picture failed", Toast.LENGTH_LONG).show();
					}catch(Exception ee){}
					Log.e(Utils.APP_TAG, e.getMessage());
				}
				try{
					Toast.makeText(ctx, "Picture uploaded", Toast.LENGTH_LONG).show();
				}catch(Exception ee){}
				
			}
		});

		finish();
	}

	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationPropertiesCustom data = new ConfigurationPropertiesCustom();
		data.photo = photo;
		configurationProperties.customData = data;	
	}

	private class ConfigurationPropertiesCustom{
		public Bitmap photo; 							// The actual image data
	}

	@Override
	protected void onDestroyOverride() {
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("Not supported yet.");

	}

	/**
	 * Setup the main screen to capture movement
	 */
	private void setupCaptionMovement(){

		screen.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getActionMasked())
				{
				case MotionEvent.ACTION_MOVE:
					int x = (int)event.getX() - offset_x;
					int y = (int)event.getY() - offset_y;
					int w = getWindowManager().getDefaultDisplay().getWidth() - 100; // margin to keep from going off screen
					int h = getWindowManager().getDefaultDisplay().getHeight() - 100; // margin to keep from going off screen
					if(x > w)
						x = w;
					if(y > h)
						y = h;
					if (selected_item != null){
						FrameLayout.LayoutParams lp = (LayoutParams) selected_item.getLayoutParams();
						lp.setMargins(x, y, 0, 0);
						lp.gravity = Gravity.TOP|Gravity.LEFT;
						selected_item.setLayoutParams(lp);
						selected_item.invalidate();
					}
					break;
				default:
					break;
				}
				return true;
			}
		});
	}
	
	/**
	 * Setup the view to be movable and deletable (long click)
	 * @param view
	 */
	private void setupViewForMovement(View view){
		view.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getActionMasked())
				{
				case MotionEvent.ACTION_DOWN:
					offset_x = (int)event.getX();
					offset_y = (int)event.getY();
					selected_item = v;
					break;
				default:
					break;
				}

				return false;
			}
		});
		
		/*
		view.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				((ViewManager)v.getParent()).removeView(v);
				return false;
			}
		});
		*/
	}
}
