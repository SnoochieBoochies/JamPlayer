package com.niall.mohan.jamplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;
import android.util.Log;

import com.niall.mohan.jamplayer.adapters.JamSongs;

/* This class handles all of the database insertions and queries.*/
public class MusicTable {
	public static final String TAG = "MediaSqlite";
	private static final String DB_NAME = "jam_player.db";
	private static final int DB_VERSION = 1;
	public static final String TABLE_NAME = "medias";
	public static final String _ID = "_id";
	public static final String TITLE = "title";
	public static final String TRACK_NUM = "num";
	public static final String ALBUM = "album";
	public static final String ARTIST = "artist";
	public static final String PATH = "uri";
	public static final String DURATION = "duration";
	public static final String ARTWORK_URI = "artwork";
	public static final String SERVICE_TYPE = "service";
	public static final String TRACK_ID = "trackid";
	public static final String ALBUM_ID = "album_id"; //just for local getting of artwork.
	private static String service;
	private static MusicDbHelper dbHelper;
	private SQLiteDatabase db;
	private final Context context;
	public static String [] MEDIA_PROJECTION = new String [] {
		MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM_ID
	};
	public static String [] SELECTION = new String [] {
		_ID, TITLE, ALBUM, ARTIST, PATH,
		DURATION, SERVICE_TYPE, TRACK_NUM, TRACK_ID, ARTWORK_URI, ALBUM_ID
	};

	public MusicTable(Context context) {
		this.context = context;
		dbHelper = MusicDbHelper.getInstance(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		//db.delete(TABLE_NAME, "1", null);
		//queryAll();
		db.close();
	}
	public MusicTable open() throws SQLException {
		dbHelper = new MusicDbHelper(context);
		db = dbHelper.getWritableDatabase();
		return this;
	}
	public void close() {
		if(dbHelper != null) {
			dbHelper.close();
		}
	}
	public MusicDbHelper getInstance() {
		if(dbHelper == null) {
			return dbHelper = new MusicDbHelper(null);
		} else return dbHelper;
	}

	/*The insert operation*/
	public void insert(JamSongs info) {
		Cursor cursor = query(info.getTitle(), 1,info.getService());
		if(cursor.getCount() != 0) {
			cursor.close();
			return;
		}
		cursor.close();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = setInfo(info);
		try {
			db.insert(TABLE_NAME, null, values);
		} catch(SQLiteException cons) {
			//Nothing to do. this is just from trying to insert duplicates.
			Log.i(TAG, "duplicates found");
		}
		db.close();
	}
	public Cursor query(String value, int cmd, String service) {
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		String clause;
		switch (cmd) {
		case 0:
			clause = TITLE;
			break;
		case 1:
			clause = ALBUM;
			break;
		case 2:
			clause = ARTIST;
			break;
		case 3:
			clause = PATH;
			break;
		default:
			return null;
		}
		Cursor cursor = database.query(true,TABLE_NAME, null,
				clause + "= ?"+ " AND "+SELECTION[7]+" like '%"+service+"%'", new String[] { value }, null, null, null,null);
		// database.close();
		return cursor;
	}
	/*Sets the params of a JamSongs object into a ContentValues, then gets
	 * inserted into the DB.
	*/
	private ContentValues setInfo(JamSongs mediaInfo) {
		ContentValues values = new ContentValues();
		values.put(TITLE, mediaInfo.getTitle());
		values.put(ARTIST, mediaInfo.getArtist());
		values.put(ALBUM, mediaInfo.getAlbum());
		values.put(DURATION, mediaInfo.getDuration());
		values.put(PATH, mediaInfo.getPath());
		values.put(SERVICE_TYPE, mediaInfo.getService());
		values.put(TRACK_NUM, mediaInfo.getTrackNum());
		values.put(TRACK_ID, mediaInfo.getId());
		values.put(ARTWORK_URI, mediaInfo.getArtwork());
		values.put(ALBUM_ID, mediaInfo.getAlbumId());
		return values;
	}

	/*-------Query operations for artists/albums/songs.--------------*/
	public Cursor getArtistsByService(String service) {
		db = dbHelper.getReadableDatabase();
		Log.w(TAG,"service = "+service);
		Cursor mCursor = null;
		if(service == null || service.length() == 0) {
			mCursor = db.query(TABLE_NAME, MEDIA_PROJECTION, 
				     null, null, null, null, null);
		} else {
			mCursor = db.query(true, TABLE_NAME, new String [] {_ID, ARTIST,ALBUM, SERVICE_TYPE}, SELECTION[6]+ " like '%"+service+"%'", null, SELECTION[3], null, SELECTION[3], null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));
		}
		return mCursor;
	}
	public Cursor getArtistsAlbumsByService(String service, String artist) {
		db = dbHelper.getReadableDatabase();
		Cursor mCursor = null;
		if(artist == null || artist.length() == 0) {
			mCursor = db.query(TABLE_NAME, MEDIA_PROJECTION, 
				     null, null, null, null, null);
		} else {
			mCursor = db.query(false, TABLE_NAME, new String [] {_ID,ARTIST,ALBUM, SERVICE_TYPE, ALBUM_ID}, SELECTION[3] + " like '%"+artist+"%'"+" AND "+SELECTION[6]+ " like '%"+service+"%'",
					null, SELECTION[2], null, SELECTION[2], null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null album");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));
		}
		db.close();
		return mCursor;
	}
	public Cursor getAlbumSongs(String service, String artist, String album) {
		db = dbHelper.getReadableDatabase();
		Log.i(TAG, String.valueOf(db.getPageSize()));
		Cursor mCursor = null;
		if(album == null || album.length() == 0) {
			Log.i(TAG, "album null");
			mCursor = db.query(true,TABLE_NAME, SELECTION, SELECTION[6]+ " like '%"+service+"%'", null, SELECTION[1], null, null, null);
		} else {
			mCursor = db.query(true, TABLE_NAME, SELECTION, SELECTION[3] + " like '%"+artist+"%' AND "+SELECTION[2] + " like '%"+album+"%' AND "+SELECTION[6]+ " like '%"+service+"%'", null, SELECTION[1], null, SELECTION[7], null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null songs");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));
		}
		db.close();
		return mCursor;
	}
	/*----------------end of query operations----------------------*/
	/*This inner class represents the actual creation/upgrade of the Database from
	 * extending SQLiteOpenHelper*/
	private static class MusicDbHelper extends SQLiteOpenHelper {

		private Context context;
		private static MusicDbHelper mInstance;
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
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY, " +
					 TITLE + " VARCHAR," + PATH + " VARCHAR,"+ ALBUM + " VARCHAR,"+ TRACK_NUM + " INTEGER," +  DURATION + " VARCHAR," + ARTIST + " VARCHAR," 
					+ SERVICE_TYPE +" VARCHAR, "+TRACK_ID+" VARCHAR, "+ ARTWORK_URI+" VARCHAR, "+ALBUM_ID+" INTEGER)");
		}
		//I've made this onUpgrade so simple as columns won't really be added/removed very often at all.
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "onUpgrade()");
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}

	}
}
