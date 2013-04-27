package com.niall.mohan.jamplayer;

import java.util.ArrayList;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.provider.MediaStore;
import android.util.Log;

public class MusicTable {
	public static final String TAG = "MediaSqlite";
	private static final String DB_NAME = "jam_player.db";
	private static final int DB_VERSION = 9;
	public static final String TABLE_NAME = "medias";
	public static final String _ID = "_id";
	public static final String TITLE = "title";
	public static final String TRACK_NUM = "num";
	public static final String ALBUM = "album";
	public static final String ARTIST = "artist";
	public static final String PATH = "uri";
	public static final String DURATION = "duration";
	public static final String ARTWORK_URI = "";
	public static final String SERVICE_TYPE = "service";
	private static String service;
	private static MusicDbHelper dbHelper;
	private SQLiteDatabase db;
	private final Context context;
	public static String [] MEDIA_PROJECTION = new String [] {
		MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.TRACK
	};
	public static String [] SELECTION = new String [] {
		_ID, TITLE, ALBUM, ARTIST, PATH,
		DURATION, SERVICE_TYPE, TRACK_NUM};
	//public MusicTable() {
	//	dbHelper = getInstance();
	//	SQLiteDatabase db = dbHelper.getWritableDatabase();
	//	queryAll();
	//	db.close();
	//}
	public MusicTable(Context context) {
		this.context = context;
		dbHelper = MusicDbHelper.getInstance(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
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

	public void insert(JamSongs info) {
		//Log.i(TAG,info._title);
		Cursor cursor = query(info.getTitle(), 1,"local");
		//Log.i("INFO", info.title);
		if(cursor.getCount() != 0) {
			cursor.close();
			Log.i(TAG,"cursor is not empty " + cursor.getCount());
			return;
		}
		Log.i(TAG,"cursor is empty");
		cursor.close();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = setInfo(info);
		db.insert(TABLE_NAME, null, values);
		db.close();
	}

	public List<JamSongs> queryAll() {
		SQLiteDatabase database = dbHelper.getReadableDatabase();

		Cursor cursor = database.query(TABLE_NAME,
				SELECTION, null, null, null,
				null, null);

		List<JamSongs> medias = new ArrayList<JamSongs>();
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {
			JamSongs mediaInfo =new JamSongs(cursor.getString(1),cursor.getString(4),cursor.getString(7), cursor.getString(2), cursor.getString(6), cursor.getString(3),
					cursor.getInt(7));
			medias.add(mediaInfo);
			//Log.i("LIST: ", String.valueOf(medias.get(i)._cloud));
			cursor.moveToNext();
		}
		cursor.close();
		//Log.i(TAG, cursor.getCount() + "");
		database.close();
		return medias;
	}
	public List<String> query(String[] projection) {
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		ArrayList<String> playlists = new ArrayList<String>();
		Cursor cursor = database.query(TABLE_NAME,
				projection, null, null, null, null, null);
		if (cursor == null)
			return null;
		cursor.moveToFirst();
		final int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			String father = cursor.getString(0);
			playlists.add(father);
			cursor.moveToNext();
		}
		cursor.close();
		database.close();
		MediaUtils.unduplicate(playlists);
		return playlists;
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
	public void deleteByName(String name, int cmd) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
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
		default:
			return;
		}
		database.delete(TABLE_NAME, clause + " = ?",
				new String[] { name });
		database.close();
	}
	private ContentValues setInfo(JamSongs mediaInfo) {
		ContentValues values = new ContentValues();
		values.put(TITLE, mediaInfo.getTitle());
		values.put(ARTIST, mediaInfo.getArtist());
		values.put(ALBUM, mediaInfo.getAlbum());
		values.put(DURATION, mediaInfo.getDuration());
		values.put(PATH, mediaInfo.getPath());
		values.put(SERVICE_TYPE, mediaInfo.getService());
		//values.put(MusicDbHelper.PLAYLIST, mediaInfo._playlist);
		return values;
	}


	public Cursor getArtistsByService(String service) {
		this.service = service;
		db = dbHelper.getReadableDatabase();
		Log.w(TAG,"service = "+service);
		Cursor mCursor = null;
		if(service == null || service.length() == 0) {
			mCursor = db.query(TABLE_NAME, MEDIA_PROJECTION, 
				     null, null, null, null, null);
		} else {
			//just try and implement the adapter like in the ExpandAdapter.
			//mCursor = db.query(true, TABLE_NAME, SELECTION, SELECTION[7]+ " like '%"+ service+"%'", null, null, null,null,null);
			//mCursor = db.query(true,TABLE_NAME, new String [] {_ID,ARTIST},  SELECTION[7]+ " like '%"+service+"%'",null, null, null, null,null);
			mCursor = db.query(true, TABLE_NAME, new String [] {_ID, ARTIST,ALBUM}, SELECTION[6]+ " like '%"+service+"%'", null, SELECTION[3], null, null, null);
			//mCursor = db.query(true,TABLE_NAME, new String [] {_ID,ARTIST},  SELECTION[7] + " = ?",null, null, null, null,null);
			//mCursor = db.rawQuery("SELECT _id,artist,album,title FROM "+TABLE_NAME+" WHERE service = '%"+service+"%'", null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));
		}
		return mCursor;
	}
	public Cursor getArtistsAlbumsByService(String artist) {
		db = dbHelper.getReadableDatabase();
		Cursor mCursor = null;
		if(artist == null || artist.length() == 0) {
			mCursor = db.query(TABLE_NAME, MEDIA_PROJECTION, 
				     null, null, null, null, null);
		} else {
			mCursor = db.query(false, TABLE_NAME, new String [] {_ID,ARTIST,ALBUM}, SELECTION[3] + " like '%"+artist+"%'", null, SELECTION[2], null, null, null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null album");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));
		}
		return mCursor;
	}
	public Cursor getAlbumSongs(String artist, String album) {
		Cursor mCursor = null;
		if(album == null || album.length() == 0) {
			mCursor = db.query(true,TABLE_NAME, SELECTION, SELECTION[6]+ " like '%"+service+"%'", null, SELECTION[1], null, null, null);
		} else {
			mCursor = db.query(true, TABLE_NAME, SELECTION, SELECTION[3] + " like '%"+artist+"%' AND "+SELECTION[2] + " like '%"+album+"%'", null, SELECTION[1], null, null, null);
		}
		if(mCursor != null) {
			Log.i(TAG, "not null songs");
			mCursor.moveToFirst();
			final int count = mCursor.getCount();
			Log.i(TAG, String.valueOf(count));

		}
		return mCursor;
	}
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
					+ SERVICE_TYPE +" VARCHAR)");
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
