package com.cwc.litenote;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DB   
{

    private static Context mContext = null;
    private static DatabaseHelper mDbHelper ;
    private static SQLiteDatabase mDb;
    private static final String DB_NAME = "notes.db";
    private static int DB_VERSION = 1;
    private static String DB_NOTETABLE_PREFIX = "notesTable";
    private static String DB_NOTETABLE_NAME; // Note: name = prefix + id
    private static String DB_PLAYLIST_PREFIX = "playlist";
    private static String DB_PLAYLIST_NAME; // Note: name = prefix + id    
    private static String mNoteTableId;
    private static String mPlaylistId;
    
    public static final String KEY_NOTE_ID = "_id"; //do not rename _id for using CursorAdapter 
    public static final String KEY_NOTE_TITLE = "note_title";
    public static final String KEY_NOTE_BODY = "note_body";
    public static final String KEY_NOTE_MARKING = "note_marking";
    public static final String KEY_NOTE_PICTURE_URI = "note_picture";
    public static final String KEY_NOTE_CREATED = "note_created";
    
    public static final String KEY_TABINFO_ID = "tabInfo_id"; //can rename _id for using BaseAdapter
    public static final String KEY_TABINFO_NOTE_TABLE_TITLE = "tabInfo_note_table_title";
    public static final String KEY_TABINFO_NOTE_TABLE_ID = "tabInfo_note_table_id";
    public static final String KEY_TABINFO_PLAYLIST_TITLE = "tabInfo_playlist_title";
    public static final String KEY_TABINFO_PLAYLIST_ID = "tabInfo_playlist_id";
    public static final String KEY_TABINFO_STYLE = "tabInfo_style";
    public static final String KEY_TABINFO_CREATED = "tabInfo_created";
    
    public static final String KEY_MEDIA_ID = "media_id";
    public static final String KEY_MEDIA_TITLE = "media_title";
    public static final String KEY_MEDIA_URI_STRING = "media_uri_string";
    public static final String KEY_MEDIA_MARKING = "media_marking";
    public static final String KEY_MEDIA_CREATED = "media_created";
    
	private static int DEFAULT_TAB_COUNT = 5;//10; //first time
	

    /** Constructor */
    public DB(Context context) {
        DB.mContext = context;
    }

    public DB open() throws SQLException 
    {
        mDbHelper = new DatabaseHelper(mContext); 
        
        // will call DatabaseHelper.onCreate()first time when database is not created yet
        mDb = mDbHelper.getWritableDatabase();
        return this;  
    }

    public void close() {
        mDbHelper.close(); 
    }
    
    
    private static class DatabaseHelper extends SQLiteOpenHelper
    {  
        public DatabaseHelper(Context context) 
        {  
            super(context, DB_NAME , null, DB_VERSION);
        }

        @Override
        //Called when the database is created ONLY for the first time.
        public void onCreate(SQLiteDatabase db)
        {   
        	String tableCreated;
        	String DB_CREATE;
        	
        	// tables for notes
        	for(int i=1;i<=DEFAULT_TAB_COUNT;i++)
        	{
        		tableCreated = DB_NOTETABLE_PREFIX.concat(String.valueOf(i));
	            DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" + 
				            		KEY_NOTE_ID + " INTEGER PRIMARY KEY," +
				            		KEY_NOTE_TITLE + " TEXT," +
				            		KEY_NOTE_PICTURE_URI + " TEXT," +
				    				KEY_NOTE_BODY + " TEXT," + 
				    				KEY_NOTE_MARKING + " INTEGER," +
				    				KEY_NOTE_CREATED + " INTEGER);";
	            db.execSQL(DB_CREATE);         
        	}
        	
        	// table for Tab info
        	tableCreated = "TAB_INFO";
            DB_CREATE = "CREATE TABLE IF NOT EXISTS " + tableCreated + "(" + 
			            		KEY_TABINFO_ID + " INTEGER PRIMARY KEY," +
			            		KEY_TABINFO_NOTE_TABLE_TITLE + " TEXT," +
			            		KEY_TABINFO_NOTE_TABLE_ID + " INTEGER," +
			            		KEY_TABINFO_PLAYLIST_TITLE + " TEXT," + ///
			            		KEY_TABINFO_PLAYLIST_ID + " INTEGER," + ///
			            		KEY_TABINFO_STYLE + " INTEGER," +
			            		KEY_TABINFO_CREATED + " INTEGER);";
            db.execSQL(DB_CREATE);  
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        { //??? how to upgrade?
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onUpgrade / drop DB / DATABASE_NAME = " + DB_NAME);
     	    onCreate(db);
        }
        
        @Override
        public void onDowngrade (SQLiteDatabase db, int oldVersion, int newVersion)
        { 
//            db.execSQL("DROP DATABASE IF EXISTS "+DATABASE_TABLE); 
//            System.out.println("DB / _onDowngrade / drop DB / DATABASE_NAME = " + DB_NAME);
     	    onCreate(db);
        }
    }
    
    /*
     * DB functions
     * 
     */
	public static Cursor mTabInfoCursor;
	public Cursor mNoteCursor;
	public Cursor mMediaCursor;
	
	void doOpen() {
		this.open();
		mTabInfoCursor = this.getAllTabInfo();
		mNoteCursor = this.getAllNotes();
	}
	
	void doGetMediaCursor() {
		mMediaCursor = this.getAllMedium();
	}	
	
	void doClose()	{
		this.close();
	}
	
    // delete DB
	public static void deleteDB()
	{
        mDb = mDbHelper.getWritableDatabase();
        try {
	    	mDb.beginTransaction();
	        mContext.deleteDatabase(DB_NAME);
	        mDb.setTransactionSuccessful();
	    }
	    catch (Exception e) {
	    }
	    finally {
	    	Toast.makeText(mContext,R.string.config_delete_DB_toast,Toast.LENGTH_SHORT).show();
	    	mDb.endTransaction();
	    }
	}         
	
	public static boolean isTableExisted(String tableName) 
	{
	    Cursor cursor = mDb.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'", null);
	    if(cursor!=null) 
	    {
	        if(cursor.getCount()>0) 
	        {
	        	cursor.close();
	            return true;
	        }
	        cursor.close();
	    }
	    return false;
	}		
	
    /**
     *  Note table
     * 
     */
    // table columns: for note
    String[] strNoteColumns = new String[] {
          KEY_NOTE_ID,
          KEY_NOTE_TITLE,
          KEY_NOTE_PICTURE_URI,
          KEY_NOTE_BODY,
          KEY_NOTE_MARKING,
          KEY_NOTE_CREATED
      };

    //insert new note table
    public void insertNewNoteTable(int id)
    {   
    	{
    		//format "notesTable1"
        	DB_NOTETABLE_NAME = DB_NOTETABLE_PREFIX.concat(String.valueOf(id));
            String dB_insert_table = "CREATE TABLE IF NOT EXISTS " + DB_NOTETABLE_NAME + "(" +
            							KEY_NOTE_ID + " INTEGER PRIMARY KEY," +
            							KEY_NOTE_TITLE + " TEXT," +  
            							KEY_NOTE_PICTURE_URI + " TEXT," +  
            							KEY_NOTE_BODY + " TEXT," +
            							KEY_NOTE_MARKING + " INTEGER," +
            							KEY_NOTE_CREATED + " INTEGER);";
            mDb.execSQL(dB_insert_table);         
    	}
    }

    //delete table
    public void dropNoteTable(int id)
    {   
    	{
    		//format "notesTable1"
        	DB_NOTETABLE_NAME = DB_NOTETABLE_PREFIX.concat(String.valueOf(id));
            String dB_drop_table = "DROP TABLE IF EXISTS " + DB_NOTETABLE_NAME + ";";
            mDb.execSQL(dB_drop_table);         
    	}
    }    
    
    // select all notes
    public Cursor getAllNotes() {
    	///
//    	Cursor cursor = null;
//    	try
//    	{
//    		cursor = mDb.query(DB_TABLE_NAME, 
//                strCols,
//                null, 
//                null, 
//                null, 
//                null, 
//                null  
//                );  
//    	}
//    	catch(Exception e)
//    	{
//    		while(cursor == null)
//    		{
//	        	int tblNum = Integer.valueOf(mTableNum);
//            	System.out.println("table number:" + tblNum + " is not found");
//            	System.out.println(DB_TABLE_NAME + " is not found");
//
//            	///
////        		mDb.dropTable(iTabTableId);
////        		mDb.deleteTabInfo("TAB_INFO",TabId);            	
//            	
//            	///
//	        	tblNum--;
//	        	mTableNum = String.valueOf(tblNum);
//	            DB_TABLE_NAME = DB_TABLE_NAME_PREFIX.concat(mTableNum);
//	            try
//	            {
//	            	cursor = mDb.query(DB_TABLE_NAME, 
//		                    strCols,
//		                    null, 
//		                    null, 
//		                    null, 
//		                    null, 
//		                    null  
//		                    ); 
//	            }
//	            catch(Exception ex)
//	            {
//	            	cursor = null; // still not found
//	            }
//    		}
//    	}
//		
//    	if(null == cursor)
//			System.out.println(DB_TABLE_NAME + " is not found");
//		
//    	return cursor; 
    	///
    	
        return mDb.query(DB_NOTETABLE_NAME, 
             strNoteColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }   
    
    //set note table id
    public static void setNoteTableId(String id)
    {
    	mNoteTableId = id;
    	
    	// table number initialization: name = prefix + id
        DB_NOTETABLE_NAME = DB_NOTETABLE_PREFIX.concat(mNoteTableId);
    	System.out.println("DB / _setNoteTableId mNoteTableId=" + mNoteTableId);
    }  
    
    //get note table id
    public static String getNoteTableId()
    {
    	return mNoteTableId;
    }  
    
    //get note table name, name = prefix + id
    public static String getNoteTableName()
    {
    	return DB_NOTETABLE_NAME;
    }     

    // Insert note
    // createTime: 0 will update time
    public long insertNote(String title,String picStr, String body, Long createTime)
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_NOTE_TITLE, title);   
        args.put(KEY_NOTE_PICTURE_URI, picStr);
        args.put(KEY_NOTE_BODY, body);
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, now.getTime());
        else
        	args.put(KEY_NOTE_CREATED, createTime);
        	
        args.put(KEY_NOTE_MARKING,0);
        return mDb.insert(DB_NOTETABLE_NAME, null, args);  
    }  
    
    public boolean deleteNote(long rowId) {  
        return mDb.delete(DB_NOTETABLE_NAME, KEY_NOTE_ID + "=" + rowId, null) > 0;
    }
    
    //query note
    public Cursor queryNote(long rowId) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
					                DB_NOTETABLE_NAME,
					                new String[] {KEY_NOTE_ID,
				  								  KEY_NOTE_TITLE,
				  								  KEY_NOTE_PICTURE_URI,
        										  KEY_NOTE_BODY,
        										  KEY_NOTE_MARKING,
        										  KEY_NOTE_CREATED},
					                KEY_NOTE_ID + "=" + rowId,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    // update note
    // 		createTime:  0 for Don't update time
    public boolean updateNote(long rowId, String title, String picStr, String body, long marking, long createTime) { 
        ContentValues args = new ContentValues();
        args.put(KEY_NOTE_TITLE, title);
        args.put(KEY_NOTE_PICTURE_URI, picStr);
        args.put(KEY_NOTE_BODY, body);
        args.put(KEY_NOTE_MARKING, marking);
        
        if(createTime == 0)
        	args.put(KEY_NOTE_CREATED, mNoteCursor.getLong(mNoteCursor.getColumnIndex(KEY_NOTE_CREATED)));
        else
        	args.put(KEY_NOTE_CREATED, createTime);

        int cUpdateItems = mDb.update(DB_NOTETABLE_NAME, args, KEY_NOTE_ID + "=" + rowId, null);
        return cUpdateItems > 0;
    }    

	int getAllNotesCount()
	{
		return mNoteCursor.getCount();
	}

	public int getMaxNoteId() {
		Cursor cursor = this.getAllNotes();
		int total = cursor.getColumnCount();
		int iMax =1;
		int iTemp = 1;
		for(int i=0;i< total;i++)
		{
			cursor.moveToPosition(i);
			iTemp = cursor.getInt(cursor.getColumnIndex(KEY_NOTE_ID));
			iMax = (iTemp >= iMax)? iTemp: iMax;
		}
		return iMax;
	}
	
	int getCheckedNoteItemsCount()
	{
		int cCheck =0;
		for(int i=0;i< getAllNotesCount() ;i++)
		{
			if(getNoteMarking(i) == 1)
				cCheck++;
		}
		return cCheck;
	}		
	
	
	// get note by Id
	public String getNoteTitleStringById(Long mRowId) 
	{
		return queryNote(mRowId).getString(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_TITLE));
	}
	
	public String getNoteBodyStringById(Long mRowId) 
	{
		return  queryNote(mRowId).getString(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_BODY));
	}

	public String getNotePictureStringById(Long mRowId)
	{
        Cursor noteCursor = queryNote(mRowId);
		String pictureFileName = noteCursor.getString(noteCursor
														.getColumnIndexOrThrow(DB.KEY_NOTE_PICTURE_URI));
		return pictureFileName;
	}
	
	public Long getNoteMarkingById(Long mRowId) 
	{
		return  queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_MARKING));
	}

	public Long getNoteCreatedTimeById(Long mRowId)
	{
		return  queryNote(mRowId).getLong(queryNote(mRowId)
											.getColumnIndexOrThrow(DB.KEY_NOTE_CREATED));
	}

	// get note by position
	Long getNoteId(int position)
	{
		mNoteCursor.moveToPosition(position);
        return (long) mNoteCursor.getInt(mNoteCursor.getColumnIndex(KEY_NOTE_ID));
	}
	
	String getNoteTitle(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_TITLE));
	}
	
	String getNoteBodyString(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_BODY));
	}

	String getNotePictureUri(int position)
	{
		mNoteCursor.moveToPosition(position);
        return mNoteCursor.getString(mNoteCursor.getColumnIndex(KEY_NOTE_PICTURE_URI));
	}
	
	int getNoteMarking(int position)
	{
		mNoteCursor.moveToPosition(position);
		return mNoteCursor.getInt(mNoteCursor.getColumnIndex(KEY_NOTE_MARKING));
	}
	
	Long getNoteCreateTime(int position)
	{
		mNoteCursor.moveToPosition(position);
		return mNoteCursor.getLong(mNoteCursor.getColumnIndex(KEY_NOTE_CREATED));
	}

    /*
     * Tab information
     * 
     */
	
    // table columns: for tab info
    String[] strTabInfoColumns = new String[] {
            KEY_TABINFO_ID,
            KEY_TABINFO_NOTE_TABLE_TITLE,
            KEY_TABINFO_NOTE_TABLE_ID,
            KEY_TABINFO_PLAYLIST_TITLE,
            KEY_TABINFO_PLAYLIST_ID,
            KEY_TABINFO_STYLE,
            KEY_TABINFO_CREATED
        };   

    // select all tab info
    public Cursor getAllTabInfo() {
        return mDb.query("TAB_INFO", 
             strTabInfoColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }     
    
    // insert tab info
    public long insertTabInfo(String table,String title,long ntId, String plTitle, long plId, int style) 
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_TABINFO_NOTE_TABLE_TITLE, title);
        args.put(KEY_TABINFO_NOTE_TABLE_ID, ntId);
        args.put(KEY_TABINFO_PLAYLIST_TITLE, plTitle);
        args.put(KEY_TABINFO_PLAYLIST_ID, plId);
        args.put(KEY_TABINFO_STYLE, style);
        args.put(KEY_TABINFO_CREATED, now.getTime());
        return mDb.insert(table, null, args);  
    }    
    
    // delete tab info
    public long deleteTabInfo(String table,int id) 
    { 
        return mDb.delete(table, KEY_TABINFO_ID + "='" + id +"'", null);  
    }

    //query single tab info
    public Cursor queryTabInfo(String table, long id) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
        							table,
					                new String[] {KEY_TABINFO_ID,
        										  KEY_TABINFO_NOTE_TABLE_TITLE,
        										  KEY_TABINFO_NOTE_TABLE_ID,
        										  KEY_TABINFO_PLAYLIST_TITLE,
        										  KEY_TABINFO_PLAYLIST_ID,
        										  KEY_TABINFO_STYLE,
        										  KEY_TABINFO_CREATED},
					                KEY_TABINFO_ID + "=" + id,
					                null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    
    //update new table of tab info
    public boolean updateTabInfo(long id, String title, long ntId, String plTitle, long plId, int style) 
    { 
        ContentValues args = new ContentValues();
        Date now = new Date(); 
        args.put(KEY_TABINFO_NOTE_TABLE_TITLE, title);
        args.put(KEY_TABINFO_NOTE_TABLE_ID, ntId);
        args.put(KEY_TABINFO_PLAYLIST_TITLE, plTitle);
        args.put(KEY_TABINFO_PLAYLIST_ID, plId);
        args.put(KEY_TABINFO_STYLE, style);
        args.put(KEY_TABINFO_CREATED, now.getTime());
        DB_NOTETABLE_NAME = "TAB_INFO";
        return mDb.update(DB_NOTETABLE_NAME, args, KEY_TABINFO_ID + "=" + id, null) > 0;
    }    
    
	static int getAllTabInfoCount()	
	{
		return mTabInfoCursor.getCount();
	}

	int getTabInfoId(int position) 
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getInt(mTabInfoCursor.getColumnIndex(KEY_TABINFO_ID));
	}

    //get current tab info title
    public static String getCurrentTabInfoTitle()
    {
    	String title = null;
    	for(int i=0;i< getAllTabInfoCount(); i++ )
    	{
    		if( Integer.valueOf(getNoteTableId()) == getTabInfoTableId(i))
    		{
    			title = getTabInfoTitle(i);
    		}
    	}
    	return title;
    }     	

	static int getTabInfoTableId(int position)	
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getInt(mTabInfoCursor.getColumnIndex(KEY_TABINFO_NOTE_TABLE_ID));
	}
	
	static int getTabInfoPlaylistId(int position)	
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getInt(mTabInfoCursor.getColumnIndex(KEY_TABINFO_PLAYLIST_ID));
	}
	
	static String getTabInfoPlaylistTitle(int position)	
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getString(mTabInfoCursor.getColumnIndex(KEY_TABINFO_PLAYLIST_TITLE));
	}		
	
	static String getTabInfoTitle(int position) 
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getString(mTabInfoCursor.getColumnIndex(KEY_TABINFO_NOTE_TABLE_TITLE));
	}
	
	int getTabInfoStyle(int position)	
	{
		mTabInfoCursor.moveToPosition(position);
        return mTabInfoCursor.getInt(mTabInfoCursor.getColumnIndex(KEY_TABINFO_STYLE));
	}
    
    /*
     * Media
     * 
     */
    // table columns: for play list
    String[] strMediaColumns = new String[] {
            KEY_MEDIA_ID,
            KEY_MEDIA_TITLE,
            KEY_MEDIA_URI_STRING,
            KEY_MEDIA_MARKING,
            KEY_MEDIA_CREATED
        };    
    
    // select all medium
    public Cursor getAllMedium() {
        return mDb.query(DB_PLAYLIST_NAME, 
             strMediaColumns,
             null, 
             null, 
             null, 
             null, 
             null  
             );    
    }      
    
    //set note table id
    public static void setPlaylistId(String id)
    {
    	mPlaylistId = id;
    	
    	// table number initialization: name = prefix + id
        DB_PLAYLIST_NAME = DB_PLAYLIST_PREFIX.concat(id);
    	System.out.println("DB / _setPlaylistId mPlaylistId=" + mPlaylistId);
    }  
    
    //get note table id
    public static String getPlaylistId()
    {
    	return mPlaylistId;
    }  
    
    //get note table name, name = prefix + id
    public static String getPlaylistTitle()
    {
    	return DB_PLAYLIST_NAME;
    }     
    
    //insert new playlist
    public void insertNewPlaylist(int id)
    {   
    	{
    		
        	// table for Media
    		DB_PLAYLIST_NAME = DB_PLAYLIST_PREFIX.concat(String.valueOf(id));
    		String dB_insert_playlist = "CREATE TABLE IF NOT EXISTS " + DB_PLAYLIST_NAME + "(" + 
			            		KEY_MEDIA_ID + " INTEGER PRIMARY KEY," +
			            		KEY_MEDIA_TITLE + " TEXT," + 
			            		KEY_MEDIA_URI_STRING + " TEXT," + 
			            		KEY_MEDIA_MARKING + " INTEGER," + 
			            		KEY_MEDIA_CREATED + " INTEGER);";
            mDb.execSQL(dB_insert_playlist);  
    	}
    }
    
    // insert media
    public long insertMedia(String title,String uri) 
    { 
        Date now = new Date();  
        ContentValues args = new ContentValues(); 
        args.put(KEY_MEDIA_TITLE, title);
        args.put(KEY_MEDIA_URI_STRING, uri);
        args.put(KEY_MEDIA_MARKING, 1); // default: marked
        args.put(KEY_MEDIA_CREATED, now.getTime());
        return mDb.insert(DB_PLAYLIST_NAME, null, args);  
    }    
    
    // delete tab info
    public long deleteMedia(long id) 
    { 
        return mDb.delete(DB_PLAYLIST_NAME, KEY_MEDIA_ID + "='" + id +"'", null);  
    }

    //query single tab info
    public Cursor queryMedia(String tbl, long id) throws SQLException 
    {  
        Cursor mCursor = mDb.query(true,
        						   DB_PLAYLIST_NAME,
        						   new String[] {KEY_MEDIA_ID,
        										 KEY_MEDIA_TITLE,
        										 KEY_MEDIA_URI_STRING,
        										 KEY_MEDIA_CREATED,
        										 KEY_MEDIA_MARKING},
        										 KEY_MEDIA_ID + "=" + id,
        										 null, null, null, null, null);

        if (mCursor != null) { 
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    //update new table of tab info
    public boolean updateMedia(long id, String title, String uriStr, int marking) 
    { 
        ContentValues args = new ContentValues();
        Date now = new Date(); 
        args.put(KEY_MEDIA_ID, id);
        args.put(KEY_MEDIA_TITLE, title);
        args.put(KEY_MEDIA_URI_STRING, uriStr);
        args.put(KEY_MEDIA_MARKING, marking);
        args.put(KEY_MEDIA_CREATED, now.getTime());
        return mDb.update(DB_PLAYLIST_NAME, args, KEY_MEDIA_ID + "=" + id, null) > 0;
    }    
    
	int getAllMediumCount()	
	{
		return mMediaCursor.getCount();
	}

	long getMediaId(int position) 
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getLong(mMediaCursor.getColumnIndex(KEY_MEDIA_ID));
	}

	String getMediaTitle(int position) 
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getString(mMediaCursor.getColumnIndex(KEY_MEDIA_TITLE));
	}
	
	String getMediaUriString(int position)	
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getString(mMediaCursor.getColumnIndex(KEY_MEDIA_URI_STRING));
	}
	
	int getMediaMarking(int position)	
	{
		mMediaCursor.moveToPosition(position);
        return mMediaCursor.getInt(mMediaCursor.getColumnIndex(KEY_MEDIA_MARKING));
	}	
        
}