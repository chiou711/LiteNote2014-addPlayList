package com.cwc.litenote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class SlideshowPlayer extends FragmentActivity
{
   private static final String TAG = "SLIDESHOW"; // error logging tag
   
   private static final String MEDIA_INDEX = "MEDIA_INDEX";
   private static final String MEDIA_TIME = "MEDIA_TIME";
   private static final String IMAGE_INDEX = "IMAGE_INDEX";
   
   private static final int DURATION = 5000; // 5 seconds per slide
   private static final int DURATION_1S = 1000; // 1 seconds per slide
   private ImageView imageView; // displays the current image
   private SlideshowInfo slideshow; // slide show being played
   private Handler imageHandler; // used to update the slide show
   private Handler mediaHandler; // used to update the slide show
   private int imageIndex; // index of the next image to display
   private int mediaIndex; // index of current media to play
   private int mediaTime; // time in miniSeconds from which media should play 
   private int mdeiaDuration; // media length
   private MediaPlayer mediaPlayer; // plays the background music, if any
   private BroadcastReceiver mReceiver;
   
   private boolean bShowImage; 
   
   // initializes the SlideshowPlayer Activity
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.slideshow_player);
      
      imageView = (ImageView) findViewById(R.id.imageView);

      // set full screen
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		 		 		   WindowManager.LayoutParams.FLAG_FULLSCREEN);
      
      // disable screen saving
      getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
      getWindow().addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
      
      Bundle extras = getIntent().getExtras();
      
 	  System.out.println(" ");
      if (savedInstanceState == null) // Activity starting
      {
    	 System.out.println("_onCreate / savedInstanceState == null"); 
         mediaTime = 0; // position in media clip
         mediaIndex = extras.getInt("NEW_MEDIA_INDEX");  // could be 0 or an index for continuing
         
         imageIndex = 0; // start from first image
      }
      else // Activity resuming
      {
         mediaIndex = savedInstanceState.getInt(MEDIA_INDEX);  
         System.out.println("\n" + "_onCreate / mediaIndex = " + mediaIndex);
         // get the play position that was saved when config changed
         mediaTime = savedInstanceState.getInt(MEDIA_TIME); 
         System.out.println("_onCreate / mediaTime = " + mediaTime);
         // get index of image that was displayed when config changed 
         imageIndex = savedInstanceState.getInt(IMAGE_INDEX);     
      }       
      
      // get SlideshowInfo for slideshow to play
      slideshow = SlideshowEditor.slideshow;  

      mediaHandler = new Handler();
      
      if(slideshow.size() > 0)
    	  bShowImage = true;
      else
    	  bShowImage = false;
      
      if(bShowImage)
    	  imageHandler = new Handler(); // create handler to control slideshow
      
      IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
      filter.addAction(Intent.ACTION_SCREEN_OFF);
      mReceiver = new SlideshowScreenReceiver();
      registerReceiver(mReceiver, filter);   
      
      // restore saved bitmap
      if(bShowImage)
    	  mRestoredPictureBmp = (Bitmap) getLastCustomNonConfigurationInstance(); 
   }
   
   @Override
   protected void onRestart()
   {
	   super.onRestart();
	   System.out.println("_onRestart");
   }   

   
   // called after onCreate and sometimes onStop
   @Override
   protected void onStart()
   {
      super.onStart();
      System.out.println("_onStart");
   }

   @Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
   {
		super.onRestoreInstanceState(savedInstanceState);
	
		mediaIndex = savedInstanceState.getInt(MEDIA_INDEX);  
		System.out.println("_onRestoreInstanceState / mediaIndex = " + mediaIndex);
		mediaTime = savedInstanceState.getInt(MEDIA_TIME); 
		imageIndex = savedInstanceState.getInt(IMAGE_INDEX); 		
	}
   
   // called after onStart or onPause
   @Override
   protected void onResume()
   {
      super.onResume();
	  
      if(bShowImage)
    	  imageHandler.post(runSlideshow); // post updateSlideshow to execute

	  mediaHandler.post(runMediaPlay);
      
      System.out.println("_onResume");
   }

   // called when the Activity is paused
   @Override
   protected void onPause()
   {
      super.onPause();
      System.out.println(" ");
      System.out.println("_onPause");
      if ((mediaPlayer != null) && mediaPlayer.isPlaying())
    		  mediaPlayer.pause(); // pause play back
      
      if(bShowImage)
    	  imageHandler.removeCallbacks(runSlideshow);
      
      mediaHandler.removeCallbacks(runMediaPlay);      
   }

   // save slide show state so it can be restored in onCreate
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);
      // if there is a mediaPlayer, store media's current position 
      if( (slideshow.getMediaList().size() > 0) &&
    	  (slideshow.getMediaMarking(mediaIndex) == 1) )
      {
	      if (mediaPlayer != null)
	      {
	    	 System.out.println("_onSaveInstanceState / mediaIndex = " + mediaIndex);
	         outState.putInt(MEDIA_INDEX, mediaIndex); 
	         outState.putInt(MEDIA_TIME, mediaPlayer.getCurrentPosition());
	      }  
      }
      // save nextItemIndex and slideshowName
      imageIndex--;
      if(imageIndex<0)
    	  imageIndex =0;
      outState.putInt(IMAGE_INDEX, imageIndex); 
   }    
   
   @Override
   public Object onRetainCustomNonConfigurationInstance()
   {
	  if(bShowImage)
	  {
		  final Bitmap pictureSaved = mPictureBmp;
		  System.out.println("_onRetainNonConfigurationInstance");
		  return pictureSaved;
	  }
	  else
		  return null;
   }
   
   // called when the Activity stops
   @Override
   protected void onStop()
   {
      super.onStop();
      System.out.println("_onStop");
   }

   // called when the Activity is destroyed
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      unregisterReceiver(mReceiver);
      if (mediaPlayer != null) 
      {
          mediaPlayer.release(); // release MediaPlayer resources
          mediaPlayer = null;
      }
      System.out.println("_onDestroy");
   }


   // Runnable: updateSlideshow
   Bitmap mRestoredPictureBmp;
   Bitmap mPictureBmp;
   private Runnable runSlideshow = new Runnable()
   {
      @Override
      public void run()
      {
    	  if(imageIndex >= slideshow.size())
    		  imageIndex = 0;

    	  String item = slideshow.getImageAt(imageIndex);
    	  System.out.println(" Runnable updateSlideshow / imageIndex = " + imageIndex);
    	  if(SlideshowScreenReceiver.wasScreenOn)
    	  {
    		  new LoadImageTaskWeakReference(imageView).execute(Uri.parse(item));
        	  imageIndex++; 
    	  }
    	  else
    		  imageHandler.postDelayed(runSlideshow, DURATION);
    	  
      }
      
      // load bitmap: set weak reference for OOM issue
      class LoadImageTaskWeakReference extends AsyncTask<Uri, Object, Bitmap> 
      {
    	  private WeakReference<ImageView> imgInputView;
    	  private WeakReference<Bitmap> rotateBitmap;

    	  public LoadImageTaskWeakReference(ImageView imgInputView)
    	  {
    		  this.imgInputView = new WeakReference<ImageView>(imgInputView);
    	  }

    	  @Override
    	  protected Bitmap doInBackground(Uri... params) {
    		  BitmapFactory.Options options = new BitmapFactory.Options();
    		  
    		  // test experience of setting inSampleSize below
    		  // set 1: - keep image quality, but sound could be stopped shortly when image loading
    		  //        - OOM issue occurred sometimes
    		  // set 2: avoid OOM issue and can keep sound quality 
    		  options.inSampleSize = 2; 
    		  
    		  if(mRestoredPictureBmp != null)
    		  {
    			  mPictureBmp = mRestoredPictureBmp;
    			  mRestoredPictureBmp = null;
    		  }
    		  else
    			  mPictureBmp = getBitmap(params[0], getContentResolver(), options);  
    	    		
    		  Matrix matrix = null;
    		  try {
    			  matrix = UtilImage.getMatrix(params[0]);
    		  } catch (IOException e) {
    			  e.printStackTrace();
    		  }
    		  
    		  if(mPictureBmp != null)
    		  {
    			  rotateBitmap = new WeakReference<Bitmap>(Bitmap.createBitmap(mPictureBmp,
			    				  											0, 
			    				  											0,
			    				  											mPictureBmp.getWidth(),
			    				  											mPictureBmp.getHeight(),
			    				  											matrix,
			    				  											true));
    		  }
    		  else
    			  return null;
    		  
    		  return rotateBitmap.get();
    	  }

    	  @Override
    	  protected void onPostExecute(Bitmap result) 
    	  {
    		  BitmapDrawable next = new BitmapDrawable(null, result);
    		  next.setGravity(android.view.Gravity.CENTER);
    		  Drawable previous = imageView.getDrawable();
                
    		  // if previous is a TransitionDrawable, get its second drawable item
    		  if (previous instanceof TransitionDrawable)
    			  previous = ((TransitionDrawable) previous).getDrawable(1);
                
    		  if (previous == null)
    		  {
    			  imgInputView.get().setImageBitmap(result);
    		  }
    		  else
    		  {
    			  Drawable[] drawables = { previous, next };
    			  TransitionDrawable transition = new TransitionDrawable(drawables);
    			  imgInputView.get().setImageBitmap(result);
    			  transition.startTransition(1000);
    		  }
    		  imageHandler.postDelayed(runSlideshow, DURATION);    	    	
    	  }
      }
      
      // utility method to get a Bitmap from a Uri
      public Bitmap getBitmap(Uri uri, ContentResolver cr, BitmapFactory.Options options)
      {
         Bitmap bitmap = null;
         // get the image
         try
         {
            InputStream input = cr.openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(input, null, options);
         }
         catch (FileNotFoundException e) 
         {
            Log.v(TAG, e.toString());
         }
         return bitmap;
      }
   }; 
   
   // Runnable: media play 
   private Runnable runMediaPlay = new Runnable()
   {   @Override
       public void run()
	   {	
         // media player
          if (slideshow.getMediaAt(mediaIndex) != null)
          {  
        	 if(slideshow.getMediaMarking(mediaIndex) == 1)
        	 { 
	        	 // if media is null, try to create a MediaPlayer to play the music
	        	 if(mediaPlayer == null)
	        	 {
	    	         try
	    	         {
	    	        	 mediaPlayer = new MediaPlayer(); 
	    	        	 System.out.println("Runnable updateMediaPlay / new media player");
	       				 mediaPlayer.reset();
	       				 mediaPlayer.setDataSource(SlideshowPlayer.this, Uri.parse(slideshow.getMediaAt(mediaIndex)));
	       				 mediaPlayer.prepare(); // prepare the MediaPlayer to play
	       				 mediaPlayer.start();	    	            
	       				 mdeiaDuration = mediaPlayer.getDuration();
	       				 mediaPlayer.seekTo(mediaTime); // seek to mediaTime, after start() sounds better
		        		 
		        		 //??? below, set 1S will cause media player abnormal on Power key short click
	       				 mediaHandler.postDelayed(runMediaPlay,DURATION_1S * 2); 
	    	         }
	    	         catch (Exception e)
	    	         {
	    	        	 Log.v(TAG, e.toString());
	    	         }
	        	 }
	        	 else
	             {	 // set looping: if media playing is not playing when screen is ON 
			         if (!mediaPlayer.isPlaying() )
		        	 {
		        		 System.out.println("Runnable updateMediaPlay / mediaIndex = "  + mediaIndex);
		        		 System.out.println("Runnable updateMediaPlay / stop playing");
		        		 // increase media index
		        		 if(isMediaEndWasMet())	 
		        		 {
			        		 mediaPlayer.release();
				        	 mediaPlayer = null;
				        	 mediaTime = 0;
			        		 
			       			 mediaIndex++;
			       			 if(mediaIndex == slideshow.getMediaList().size())
			        			 mediaIndex = 0;	// back to first index
		        		 }
		        		 else
		        			 mediaPlayer.start();
		        		 
		        		 mediaHandler.postDelayed(runMediaPlay,DURATION_1S);
		        	 }
		        	 else // playing
		        	 {
		        		 // endless loop
		        		 // do not set post() here, it will affect slide show timing
			       		 mediaHandler.postDelayed(runMediaPlay,DURATION_1S);			       		 
		        	 }
	             }
        	 }
        	 else
        	 {
        		 mediaIndex++;
     			 if(mediaIndex >= slideshow.getMediaList().size())
      			 mediaIndex = 0;	// back to first index
     			 
     			 mediaHandler.postDelayed(runMediaPlay,DURATION_1S);
        	 }	
          }
       } 
   };  
   
   
   boolean isMediaEndWasMet()
   {
		 mediaTime = mediaPlayer.getCurrentPosition();
		 mdeiaDuration = mediaPlayer.getDuration();
		 System.out.println("mediaTime / mdeiaDuration = " + (int)((mediaTime * 100.0f) /mdeiaDuration) + "%" );
		 System.out.println("mediaTime - mdeiaDuration = " + Math.abs(mediaTime - mdeiaDuration) );
		 return Math.abs(mediaTime - mdeiaDuration) < 1500; // toleration
   }
   
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) 
   {
       if (keyCode == KeyEvent.KEYCODE_BACK ) 
       {
		   	Intent intent = getIntent();
		   	intent.putExtra("CURRENT_MEDIA_INDEX",mediaIndex);
		   	System.out.println("key back / mediaIndex = " + mediaIndex);
		   	setResult(RESULT_OK, intent);
		   	finish();
   			return true;
       }
       return super.onKeyDown(keyCode, event);
   }
}