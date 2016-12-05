package com.cwc.litenote;

import java.util.List;

import com.cwc.litenote.lib.DragSortController;
import com.cwc.litenote.lib.DragSortListView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SlideshowEditor extends Activity
{
	static SlideshowInfo slideshow; 
	private static DB mDb;
	private MediaInfoAdapter mediaInfoAdapter;
	String mFinalPageViewed_tableId_string;
	DragSortListView plListView;
	private DragSortController mController;
	int currentMediaIndex;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.slideshow_editor);
       plListView = (DragSortListView) findViewById(R.id.plListView);
       slideshow = new SlideshowInfo();
       currentMediaIndex = 0;
       
       mFinalPageViewed_tableId_string = Util.getPrefFinalPageTableId(SlideshowEditor.this);
       DB.setNoteTableId(mFinalPageViewed_tableId_string);
       
       mDb = new DB(SlideshowEditor.this);
       
	   mDb.doOpen();
	   for(int i=0;i< mDb.getAllNotesCount() ;i++)
	   {
	       if(mDb.getNoteMarking(i) == 1)
	       {
	    	   String pictureUri = mDb.getNotePictureUri(i);
	    	   slideshow.addImage(pictureUri);
	       }
	   }
	   mDb.doClose();
	  
       Button addMusicButton = (Button) findViewById(R.id.addMusicButton);
       addMusicButton.setOnClickListener(addMediaButtonListener);

       Button playButton = (Button) findViewById(R.id.playButton);
       playButton.setOnClickListener(playButtonListener);
       
       mediaInfoAdapter = new MediaInfoAdapter(this, slideshow.getMediaList());       
       plListView.setAdapter(mediaInfoAdapter);
       mController = buildController(plListView);
       plListView.setFloatViewManager(mController);
       plListView.setOnTouchListener(mController);
       plListView.setDragEnabled(true);
       plListView.setDropListener(onDrop);
//       plListView.setDragListener(onDrag);
       plListView.setMarkListener(onMark);
       
       //fill data
       DB.setPlaylistId(mFinalPageViewed_tableId_string);
       mDb.doOpen();
       if(DB.isTableExisted(DB.getPlaylistTitle()))
       {
    	   mDb.doGetMediaCursor();
	       for(int i=0; i< mDb.getAllMediumCount(); i++)
	       {
				// insert Uri string to playlist
   	    		slideshow.addMedia(mDb.getMediaUriString(i));
   	    		
   	    		// set marking
   	    		slideshow.addMediaMarking(0); // just for initialization
   	    		if(mDb.getMediaMarking(i) == 1)
   	    			slideshow.setMediaMarking(i,1);
   	    		else
   	    			slideshow.setMediaMarking(i,0);
	       }
       }
       mDb.doClose();
       
       mediaInfoAdapter.notifyDataSetChanged();
       
	   //listener: click list view item to play media 
       //cf: http://stackoverflow.com/questions/5551042/onitemclicklistener-not-working-in-listview-android
       plListView.setOnItemClickListener(new OnItemClickListener()
	   {   @Override
    	   public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		   {
		       mDb.doOpen();
		       mDb.doGetMediaCursor();
		       for(int i=0; i< mDb.getAllMediumCount(); i++)
		       {
		    	   // update media info
		    	   slideshow.setMedia(i,mDb.getMediaUriString(i));

		    	   System.out.println(" mDb.getMediaMarking(i) = " + mDb.getMediaMarking(i));
		    	   if(mDb.getMediaMarking(i) == 1)
		    	   {
		    		   slideshow.setMediaMarking(i,1);
			    	   if(position == i)
			    		   currentMediaIndex = i;
		    	   }
		    	   else
		    		   slideshow.setMediaMarking(i,0);
		    	   

		       }
		       mDb.doClose();
		       
		       // create new Intent to launch the slideShow player Activity
		       if(mDb.getMediaMarking(position) == 1)
		       {
		    	   Intent playSlideshow = new Intent(SlideshowEditor.this, SlideshowPlayer.class);
		    	   playSlideshow.putExtra("NEW_MEDIA_INDEX", currentMediaIndex); // could be 0 or an index for continuing
		    	   startActivityForResult(playSlideshow, MEDIA_PLAY);	
		       }
			}
	   	}
	   	);
    }

	public DragSortController buildController(DragSortListView dslv)
    {
        // defaults are
        DragSortController controller = new DragSortController(dslv);
        controller.setSortEnabled(true);
        
        //drag
	  	controller.setDragInitMode(DragSortController.ON_DOWN); // click

	  	controller.setDragHandleId(R.id.img_dragger);// handler
	  	controller.setBackgroundColor(Color.argb(128,128,64,0));// background color when dragging
        
	  	// mark
        controller.setMarkEnabled(true);
        controller.setClickMarkId(R.id.img_check);
        controller.setMarkMode(DragSortController.ON_DOWN);

        return controller;
    }        
	
	
    // list view listener: on drop
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() 
    {   @Override
        public void drop(int startPosition, int endPosition)
        {   //reorder data base storage
			int loop = Math.abs(startPosition-endPosition);
			for(int i=0;i< loop;i++)
			{
				swapRows(startPosition,endPosition);
				if((startPosition-endPosition) >0)
					endPosition++;
				else
					endPosition--;
			}
			mediaInfoAdapter.notifyDataSetChanged();
        }
    };	

    // swap rows
    private static Long mId1 ;
	private static String mTitle1;
	private static String mUriString1;
	private static int mMarking1;
	private static Long mId2 ;
	private static String mUriString2;
	private static String mTitle2;
	private static int mMarking2; 
	
	protected static void swapRows(int startPosition, int endPosition) 
	{
		mDb.doOpen();
		mDb.doGetMediaCursor();

		mId1 = mDb.getMediaId(startPosition);
        mTitle1 = mDb.getMediaTitle(startPosition);
        mUriString1 = mDb.getMediaUriString(startPosition);
        mMarking1 = mDb.getMediaMarking(startPosition);

		mId2 = mDb.getMediaId(endPosition);
        mTitle2 = mDb.getMediaTitle(endPosition);
        mUriString2 = mDb.getMediaUriString(endPosition);
        mMarking2 = mDb.getMediaMarking(endPosition);
		
        mDb.updateMedia(mId2,
				 mTitle1,
				 mUriString1,
				 mMarking1);		        
		
		mDb.updateMedia(mId1,
		 		 mTitle2,
		 		 mUriString2,
		 		 mMarking2);	
    	mDb.doClose();
	}    

	// set IDs for each type of media result
	private static final int MEDIA_ADD = 1;
	private static final int MEDIA_PLAY = 2;
	
	// called when the user touches the "Add Music" Button
	private OnClickListener addMediaButtonListener = new OnClickListener()
	{
		// launch music choosing activity
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("audio/*");
			startActivityForResult(Intent.createChooser(intent, 
										   getResources().getText(R.string.chooser_music)),
														MEDIA_ADD);
		}
	};

	// called when the user touches the "Play" Button
	private OnClickListener playButtonListener = new OnClickListener()
	{
		@Override
      	public void onClick(View v)
		{
		       mDb.doOpen();
	    	   mDb.doGetMediaCursor();
		       for(int i=0; i< mDb.getAllMediumCount(); i++)
		       {
		    	    // update media info
		    	    slideshow.setMedia(i,mDb.getMediaUriString(i));

	   	    		if(mDb.getMediaMarking(i) == 1)
	   	    			slideshow.setMediaMarking(i,1);
	   	    		else
	   	    			slideshow.setMediaMarking(i,0);
		       }
		       mDb.doClose();
		       
			// create new Intent to launch the slideShow player Activity
			Intent playSlideshow =
            new Intent(SlideshowEditor.this, SlideshowPlayer.class);
			playSlideshow.putExtra("NEW_MEDIA_INDEX", currentMediaIndex); // could be 0 or an index for continuing
			startActivityForResult(playSlideshow, MEDIA_PLAY);			
		}
	};



	// called when an Activity launched from this Activity returns
	@Override
	protected final void onActivityResult(int requestCode, int resultCode, 
			Intent data)
	{
		if (resultCode != RESULT_OK) 
			return;  
	   
		if (resultCode == RESULT_OK) // if there was no error
		{
			if (requestCode == MEDIA_ADD) // Activity returns music
			{
				Uri selectedUri = data.getData(); 
				System.out.println("selected Uri = " + selectedUri.toString());

				slideshow.addMedia(selectedUri.toString());
				slideshow.addMediaMarking(1); 
				
				mDb.doOpen();
				mDb.insertNewPlaylist(Integer.valueOf(mFinalPageViewed_tableId_string));
				mDb.doClose();
				
				String uriStr = selectedUri.toString();
				// insert Uri string to DB
				mDb.doOpen();
				mDb.insertMedia(Uri.parse(uriStr).getLastPathSegment(),	uriStr);
				mDb.doClose();
				
				mediaInfoAdapter.notifyDataSetChanged();				
			}
			else if(requestCode == MEDIA_PLAY)
			{
				Bundle extras = data.getExtras();
				currentMediaIndex = extras.getInt("CURRENT_MEDIA_INDEX");
				System.out.println("--currentMediaIndex " + currentMediaIndex);
			}
		}
	}	
	
	private static class ViewHolder
	{
		TextView mediaInfo; // refers to ListView item's ImageView
		ImageView imageCheck;
		Button deleteButton; // refers to ListView item's Button
	}

	// ArrayAdapter displaying media info
	private class MediaInfoAdapter extends ArrayAdapter<String>
	{
		private List<String> items; // list of image Uris
		private LayoutInflater inflater;
      
		public MediaInfoAdapter(Context context, List<String> items)
		{
			super(context, -1, items); // -1 indicates we're customizing view
			this.items = items;
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder; // holds references to current item's GUI
         
			// if convertView is null, inflate GUI and create ViewHolder;
			// otherwise, get existing ViewHolder
			if (convertView == null)
			{
				//ref: http://stackoverflow.com/questions/7312481/android-textview-not-filling-width-why
				convertView = inflater.inflate(R.layout.slideshow_edit_item, parent, false);
				// set up ViewHolder for this ListView item
				viewHolder = new ViewHolder();
				viewHolder.imageCheck = (ImageView) convertView.findViewById(R.id.img_check);
				viewHolder.mediaInfo = (TextView) convertView.findViewById(R.id.mediaInfo);
				viewHolder.deleteButton = (Button) convertView.findViewById(R.id.deleteButton);
				
				convertView.setTag(viewHolder); // store as View's tag
			}
			else // get the ViewHolder from the convertView's tag
				viewHolder = (ViewHolder) convertView.getTag();

		  	// marking
			mDb.doOpen();
			mDb.doGetMediaCursor();
			if( mDb.getMediaMarking(position) == 1) //??? exception
				viewHolder.imageCheck.setBackgroundResource(
		    			R.drawable.btn_check_on_holo_light);
			else
				viewHolder.imageCheck.setBackgroundResource(
						R.drawable.btn_check_off_holo_light);
			 mDb.doClose();
			// get media info 
			String item = items.get(position);
			
			mDb.doOpen();
			mDb.doGetMediaCursor();
			Uri uri = Uri.parse(mDb.getMediaUriString(position));
			viewHolder.mediaInfo.setText(uri.getLastPathSegment());
  		    mDb.doClose();

			// configure the "Delete" Button
			viewHolder.deleteButton.setTag(item);
			viewHolder.deleteButton.setOnClickListener(deleteButtonListener);

			return convertView;
		}
	}

    // list view listener: on mark
    private DragSortListView.MarkListener onMark =
        new DragSortListView.MarkListener() 
		{   @Override
            public void mark(int position) 
			{
                mDb.doOpen();
                mDb.doGetMediaCursor();
                String title = mDb.getMediaTitle(position);
                String uriString = mDb.getMediaUriString(position);
                Long id =  (long) mDb.getMediaId(position);
			
                if(mDb.getMediaMarking(position) == 0)
                {
              	  	mDb.updateMedia(id, title, uriString,1);
              	  	slideshow.setMediaMarking(position,1);              	  
                }
                else
                {
              	  	mDb.updateMedia(id, title, uriString,0);
              	  	slideshow.setMediaMarking(position,0); 
                }
                
                mDb.doClose();
              
                mediaInfoAdapter.notifyDataSetChanged();
                
                currentMediaIndex = 0; // set to default when the list is changed
                return;
            }
        };   	
	
	private OnClickListener deleteButtonListener = new OnClickListener()
	{
		// removes the image
		@Override
		public void onClick(View v)
		{
			mediaInfoAdapter.remove((String) v.getTag());
			mDb.doOpen();
			System.out.println("deleteButtonListener / position = " + plListView.getPositionForView(v));
			mDb.deleteMedia(mDb.getMediaId(plListView.getPositionForView(v)));
			currentMediaIndex = 0; // set to default when the list is changed
			mDb.doClose();
		}
	};
}