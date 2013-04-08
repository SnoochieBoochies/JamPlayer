package com.niall.mohan.jamplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MusicDbHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "jam_player.db";
	private static final String TAG = "MusicDbHelper";
	private static final int DB_VERSION = 2;
	public static final String TABLE_NAME = "medias";
	public static final String _ID = "_id";
	public static final String TITLE = "title";
	public static final String ALBUM = "album";
	public static final String ARTIST = "artist";
	public static final String URI_PATH = "uri";
	public static final String URL_PATH = "url";
	public static final String DURATION = "duration";
	public static final String DISPLAY_NAME = "display_name";
	public static final String PLAYLIST = "playlist";
	public static final String IS_CLOUD = "is_cloud";
	public static final String ARTWORK_URI = "";
	
	private Context context;
	private static MusicDbHelper mInstance = null;
	public static MusicDbHelper getInstance(Context context) {
		if(mInstance ==null)
			mInstance = new MusicDbHelper(context);
		return mInstance;
	}
	public MusicDbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}
	
	public MusicDbHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG,"onCreate()");	
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY, "+ IS_CLOUD+" smallint NOT NULL  DEFAULT 0, "
				+ TITLE + " VARCHAR," + URI_PATH + " VARCHAR,"+ URL_PATH + " VARCHAR,"+ ALBUM + " VARCHAR," +  DURATION + " VARCHAR," + ARTIST + " VARCHAR," 
				+ DISPLAY_NAME + " VARCHAR)" );
	}
	//I've made this onUpgrade so simple as columns won't really be added/removed very often at all.
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}
	
}
