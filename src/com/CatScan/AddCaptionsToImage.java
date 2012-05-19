package com.CatScan;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
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
	private static final int DEFAULT_COLOR = Color.rgb(255, 255, 255);		// the default caption color
	private static final int NULL_COLOR = Color.argb(0, 0, 0, 0); 			// the color to remove  from edges of picture

	// member variables
	private Bitmap photo; 													// The actual image data
	private int currentColor = DEFAULT_COLOR; 								// the currently selected color	
	private CaptionsList captionsList = new CaptionsList();	
	private boolean isPosting = false;
	private CatPicture sourcePicture = null;

	// pointers to graphics
	private ImageView photoView; 					// The image showing
	private Button postPicture;
	private EditText title;
	private ViewGroup screen;
	private EditText captionEditor;
	private ViewGroup EditorView; 
	private ImageView trash;
	private ViewGroup topEdit;
	private Drawable postButtonBackground;

	// motion parameters
	private View selected_item = null;
	private int offset_x = 0;
	private int offset_y = 0;
	
	// public static variables used for passsing objects between activities, set to null once used
	public static CatPicture sourcePictureStatic;

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
				if (data.sourcePicture != null)
					sourcePicture = data.sourcePicture;
			}
		}
		
		// grab passed catPicture and set static to null
		if (sourcePictureStatic != null){
			sourcePicture = sourcePictureStatic;
			sourcePictureStatic = null;
		}

		// no photo
		if (photo == null){
			Toast.makeText(ctx, "Photo Problem - Make sure you have an SD card", Toast.LENGTH_LONG).show();
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
		trash = (ImageView) findViewById(R.id.trash);
		topEdit = (ViewGroup) findViewById(R.id.topEdit);
		postButtonBackground = postPicture.getBackground();

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

		// when done with caption, launch color picker
		captionEditor.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER){
					if (event.getAction() == KeyEvent.ACTION_UP)
						changeColor();
					return true;
				}

				return false;
			}
		});
	}

	private ColorPickerDialog colorPickerDialog = null;
	/**
	 * Select the color and then place the caption
	 */
	private void changeColor(){

		// make sure we have text to edit
		if(captionEditor.getText().toString() == null || captionEditor.getText().toString().length() == 0){
			Toast.makeText(ctx, "Enter caption text first", Toast.LENGTH_SHORT).show();
			return;
		}

		// setup color picker
		if (colorPickerDialog == null){
			colorPickerDialog =
				new ColorPickerDialog(ctx, new ColorPickerDialog.OnColorChangedListener() {

					@Override
					public void colorChanged(int color, int red, int green, int blue) {
						// store the color
						currentColor = Color.rgb(red, green, blue);

						// launch makeCaption
						makeCaption();
					}
				},
				Color.red(currentColor),
				Color.green(currentColor),
				Color.blue(currentColor));
		}

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

		// show a dialog showing how to use the caption editor
		com.tools.Tools.showOneTimeDialog(ctx,
				"showHelpForCaptions",
				getResources().getString(R.string.app_name),
				"1. You can move the caption by touching and dragging.<br>"
				+"2. You can delete the caption by moving it onto the trash. You'll see the trash when you touch one of your captions.<br>"
				+"3. You can enter as many captions as you'd like. Just type a new one",
				null);	
		
		// read the caption to create
		String captionText = captionEditor.getText().toString();

		// create the caption
		ViewGroup holder = (ViewGroup)((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
		inflate(R.layout.caption_text_view, null);
		TextView caption = (TextView)holder.findViewById(R.id.captionTextView);

		// set the text of the caption
		caption.setText(captionText.toUpperCase());

		// set the color
		caption.setTextColor(currentColor);
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
		captionsList.putCaption(new Caption(captionText, caption));	

		updateDoneButton();
	}

	private void updateDoneButton(){
		if (captionsList.nCaptions() >= 1){
			postPicture.setBackgroundResource(R.drawable.rounded_corners_turquoise);
		}else if (postButtonBackground != null)
			postPicture.setBackgroundDrawable(postButtonBackground);
		else
			postPicture.setBackgroundResource(R.drawable.rounded_corners);

		// also update caption editor hint
		if (captionsList.nCaptions() >= 1)
			captionEditor.setHint(R.string.captionAnotherHint);
		else
			captionEditor.setHint(R.string.captionHint);

	}

	/**
	 * Post the picture to the server button has been clicked
	 * @param view
	 */
	public void postPictureClicked(View view){

		// if there are no captions, ask if we're sure
		if (captionsList.nCaptions() < 1){
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						postPicture();
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// set focus to captions editor
						captionEditor.requestFocus();
						(new Handler()).postDelayed (new Runnable() { public void run() { com.tools.Tools.showKeyboard(ctx, captionEditor); } }, 100);
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder
			.setMessage("Are you sure you want to post the picture with no captions?")
			.setPositiveButton("Yes", dialogClickListener)
			.setNegativeButton("No", dialogClickListener)
			.show();
		}else{

			// otherwise, just post the picture
			postPicture();
		}
	}

	/**
	 * Actually post the picture to the server
	 */
	private void postPicture(){

		// don't allow double posts
		if (isPosting)
			return;
		isPosting = true;

		// save the view to a bitmap
		ArrayList<View> hiddenViews = new ArrayList<View>(2);
		hiddenViews.add(EditorView);
		hiddenViews.add(topEdit);
		Bitmap picture = 
			com.tools.Tools.saveViewToBitmap(
					screen,
					hiddenViews,
					screen,
					NULL_COLOR);
		if (picture == null){
			Toast.makeText(ctx, "Picture cold not be saved", Toast.LENGTH_LONG).show();
			isPosting = false;
			return;
		}

		// read the title
		String titleString = title.getText().toString();

		// if no title, then use the first caption
		if (titleString == null || titleString.length() == 0){
			Iterator<String> iterator = captionsList.getCaptions().iterator();
			while((titleString == null || titleString.length() == 0) && iterator.hasNext())
				titleString = iterator.next();
		}

		// convert bitmap to byte[]
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		photo.compress(Bitmap.CompressFormat.JPEG, Utils.IMAGE_QUALITY, stream);
		byte[] rawArray = stream.toByteArray();
		ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
		picture.compress(Bitmap.CompressFormat.JPEG, Utils.IMAGE_QUALITY, stream2);
		byte[] picArray = stream2.toByteArray();

		// create the cat post
		CatPicture cat = new CatPicture(
				picArray,
				rawArray,
				titleString,
				captionsList.getCaptions(),
				Prefs.getName(ctx),
				Utils.getCurrentUser());
		
		// if we have a passed in catPicture, use the passed cat pictures raw
		if (sourcePicture != null)
			cat.copyRawFile(sourcePicture);

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

		// show the toast
		Toast.makeText(ctx, "Uploading picture...", Toast.LENGTH_SHORT).show();
		
		finish();
		isPosting = false;
		
		
	}

	@Override
	protected void additionalConfigurationStoring() {
		ConfigurationPropertiesCustom data = new ConfigurationPropertiesCustom();
		data.photo = photo;
		data.sourcePicture = sourcePicture;
		configurationProperties.customData = data;	
	}

	private class ConfigurationPropertiesCustom{
		public Bitmap photo; 							// The actual image data
		public CatPicture sourcePicture; 				// source cat picture 
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
				int touchX = (int)event.getX();
				int touchY = (int)event.getY();
				int x = touchX - offset_x;
				int y = touchY - offset_y;
				switch(event.getActionMasked())
				{
				case MotionEvent.ACTION_MOVE:
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

					// highlight trashcan
					setTrashIcon(touchX, touchY);	
					break;
				case MotionEvent.ACTION_UP:
					if (isOverTrash(touchX, touchY) && selected_item != null){
						captionsList.removeCaption(selected_item);
						updateDoneButton();
					}
					selected_item = null;
					trash.setImageResource(R.drawable.trashcan);
					trash.setVisibility(View.INVISIBLE);
					break;
				case MotionEvent.ACTION_CANCEL:
					selected_item = null;
					trash.setImageResource(R.drawable.trashcan);
					trash.setVisibility(View.INVISIBLE);
					break;
				default:
					break;
				}
				return true;
			}
		});
	}

	/**
	 * Determine if we are hovered over the trash
	 * @param x current x position in pixels
	 * @param y current y position in pixels
	 * @return true if over trash, and false otherwise
	 */
	private boolean isOverTrash(int x, int y){
		int width = trash.getWidth();
		int height = trash.getHeight();
		int xPos = trash.getLeft();
		int yPos = trash.getTop();

		if (x >= xPos && x <= xPos + width &&
				y >= yPos && y <= yPos + height)
			return true;
		else
			return false;
	}

	/**
	 * Set the proper trashcan icon depending on current locationi
	 * @param x the current x location
	 * @param y the current y location
	 */
	private void setTrashIcon(int x, int y){
		if (isOverTrash(x, y))
			trash.setImageResource(R.drawable.trashcan_hover);
		else
			trash.setImageResource(R.drawable.trashcan);
	}

	/**
	 * Animate the trash can icon into view
	 */
	private void showTrash(){
		float increase = 1.5f;
		ScaleAnimation up = new ScaleAnimation(1, increase, 1, increase, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		ScaleAnimation down = new ScaleAnimation(increase, 1, increase, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		up.setDuration(250);
		down.setDuration(250);
		trash.setVisibility(View.VISIBLE);
		trash.startAnimation(up);
		trash.startAnimation(down);
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
					showTrash();
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


	private static class Caption{
		private View view;
		private String text;

		public Caption(String text, View view){
			this.text = text;
			this.view = view;
		}

		public void delete(){
			((ViewGroup)view.getParent()).removeView(view);
		}

		public static void deleteView(View view){
			((ViewGroup)view.getParent()).removeView(view);
		}
	}

	private static class CaptionsList{
		private HashMap<View, Caption> captions = new HashMap<View, AddCaptionsToImage.Caption>();

		public CaptionsList() {

		}

		public void putCaption(Caption caption){
			captions.put(caption.view, caption);
		}

		public void removeCaption(Caption caption){
			captions.remove(caption.view);
			caption.delete();
		}

		public void removeCaption(View view){
			captions.remove(view);
			Caption.deleteView(view);
		}

		public int nCaptions(){
			return captions.size();
		}

		public ArrayList<String> getCaptions(){
			ArrayList<String> strings = new ArrayList<String>(nCaptions());
			Collection<Caption> values = captions.values();
			for (Caption item : values)
				if (item.text != null && item.text.length() != 0)
					strings.add(item.text);

			return strings;
		}
	}
}