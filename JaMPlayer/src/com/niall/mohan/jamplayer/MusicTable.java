package com.niall.mohan.jamplayer;

import java.util.ArrayList;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;
//TODO for cloud music,maybe mediametadataretreiver could be of use
public class MusicTable {
	public static final String TAG = "MediaSqlite";
	private static final String DB_NAME = "jam_player.db";
	private static final int DB_VERSION = 1;
	private static MusicDbHelper dbHelper;
	
	public static String [] MEDIA_PROJECTION = new String [] {
		MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DISPLAY_NAME 
	};
	public static String [] CLOUD_MEDIA_PROJECTION = new String [] {
		MusicDbHelper.ARTIST,MusicDbHelper.URI_PATH, MusicDbHelper.IS_CLOUD
	};
	public MusicTable() {
		dbHelper = getInstance();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		queryAll();
		db.close();
	}
	public MusicTable(Context context) {
		dbHelper = MusicDbHelper.getInstance(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		queryAll();
		db.close();
	}
	public MusicDbHelper getInstance() {
		if(dbHelper == null) {
			return dbHelper = new MusicDbHelper(null);
		} else return dbHelper;
	}
	public void update(JamSongs info) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = setInfo(info);
		db.update(MusicDbHelper.TABLE_NAME, values, MusicDbHelper.PLAYLIST +" = ?", new String []{info.playlist});
		db.close();
	}
	public void insert(JamSongs info) {
		//Log.i(TAG,info._title);
		Cursor cursor = query(info.getTitle(), 1);
		Log.i("INFO", info.title);
		if(cursor.getCount() != 0) {
			cursor.close();
			Log.i(TAG,"cursor is not empty " + cursor.getCount());
			return;
		}
		Log.i(TAG,"cursor is empty");
		cursor.close();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = setInfo(info);
		db.insert(MusicDbHelper.TABLE_NAME, null, values);
		db.close();
	}

	public List<JamSongs> queryAll() {
		SQLiteDatabase database = dbHelper.getReadableDatabase();

		Cursor cursor = database.query(MusicDbHelper.TABLE_NAME,
				CLOUD_MEDIA_PROJECTION, null, null, null,
				null, null);

		List<JamSongs> medias = new ArrayList<JamSongs>();
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {
			JamSongs mediaInfo = new JamSongs(cursor.getString(0), cursor.getString(1),cursor.getInt(2),null,null,null, null);
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
		Cursor cursor = database.query(MusicDbHelper.TABLE_NAME,
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
	public Cursor query(String value, int cmd) {
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		String clause;
		switch (cmd) {
		case 0:
			clause = MusicDbHelper.PLAYLIST;
			break;
		case 1:
			clause = MusicDbHelper.TITLE;
			break;
		case 2:
			clause = MusicDbHelper.ALBUM;
			break;
		case 3:
			clause = MusicDbHelper.ARTIST;
			break;
		case 4:
			clause = MusicDbHelper.URI_PATH;
			break;
		case 5:
			clause = MusicDbHelper.URL_PATH;
			break;
		default:
			return null;
		}
		Cursor cursor = database.query(MusicDbHelper.TABLE_NAME, null,
				clause + "= ?", new String[] { value }, null, null, null);
		// database.close();
		return cursor;
	}
	public void deleteByName(String name, int cmd) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		String clause;
		switch (cmd) {
		case 0:
			clause = MusicDbHelper.PLAYLIST;
			break;
		case 1:
			clause = MusicDbHelper.TITLE;
			break;
		case 2:
			clause = MusicDbHelper.ALBUM;
			break;
		case 3:
			clause = MusicDbHelper.ARTIST;
		default:
			return;
		}
		database.delete(MusicDbHelper.TABLE_NAME, clause + " = ?",
				new String[] { name });
		database.close();
	}
	private ContentValues setInfo(JamSongs mediaInfo) {
		ContentValues values = new ContentValues();
		values.put(MusicDbHelper.TITLE, mediaInfo.title);
		values.put(MusicDbHelper.ARTIST, mediaInfo.artist);
		values.put(MusicDbHelper.ALBUM, mediaInfo.album);
		values.put(MusicDbHelper.ARTIST, mediaInfo.artist);
		values.put(MusicDbHelper.DURATION, mediaInfo.duration);
		values.put(MusicDbHelper.URI_PATH, mediaInfo.path);
		values.put(MusicDbHelper.IS_CLOUD, mediaInfo.cloud);
		//values.put(MusicDbHelper.PLAYLIST, mediaInfo._playlist);
		return values;
	}

	private List<JamSongs> getMediaInfo(Cursor cursor) {
		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}
		List<JamSongs> medias = new ArrayList<JamSongs>();
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {

			JamSongs mediaInfo = new JamSongs(cursor.getString(0), cursor.getString(1),0,cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getString(5));
			medias.add(mediaInfo);
			cursor.moveToNext();
		}
		cursor.close();
		return medias;
	}

}