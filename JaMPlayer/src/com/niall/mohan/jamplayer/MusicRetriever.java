package com.niall.mohan.jamplayer;

import java.util.ArrayList;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

//TODO NEED TO CHANGE THIS CLASS TO RETREIVE FROM DATABASE, IF THE DATABASE IS POPULATED.
public class MusicRetriever {
	final String TAG = "MusicRetriever";
	ContentResolver mResolver; //this is used to get metadata info...
	List<JamSongs> songs;
	MusicTable db;

	public MusicRetriever(Context context) {
	}

	
	public MusicRetriever(ContentResolver mResolver, Context context) {
		db = new MusicTable(context);
		this.mResolver = mResolver;
		//pushToDb();
		retrieveFromDatabase();
		//prepare();
	}
	public void pushToDb() {
		ContentResolver resolver = getContentResolver();
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				MusicTable.MEDIA_PROJECTION, null, null, null);
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {
				JamSongs mediaInfo = new JamSongs(cursor.getString(0), cursor.getString(1),"local",cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getString(5));
				//Log.i(TAG, mediaInfo._album);
				//MediaInfo mediaInfo = new MediaInfo(cursor.getString(0));
				db.insert(mediaInfo);
				//songs.add(cursor.getString(0));
				cursor.moveToNext();
		}
		cursor.close();
		Log.i(TAG, "Pushed to db");
				//showPlayList();
	}
	//make the calls from the database for songs.
	//still atm only retreiving from sd card.
	//need to put in shared preferences for the first startup of the app so i don't keep quering the sd card.
	public void retrieveFromDatabase() {
		ArrayList<JamSongs> lest = (ArrayList<JamSongs>) db.queryAll();
		for(JamSongs s: lest) {
			Log.i(TAG, s.getTitle());
		}
		//ContentResolver mResolver = getContentResolver();
		//Cursor mCursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,MusicTable.MEDIA_PROJECTION,null,null,null);
		//mCursor.moveToFirst();
		//do {
		//	Log.i(TAG, mCursor.getString(1));
		//} while(mCursor.moveToNext());
		//db = new MusicTable();
		//songs = db.queryAll(); //broadcast this?
		//mCursor.close();
		//Log.i(TAG, "list size from database is: "+ songs.size());

	}
	/*
	//like a copy of MEdiaplayer.prepare(). It's an async task for loading media
	public void prepare() {
		 Uri media = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;//read from disk
		 Log.i(TAG, "Querying media...");
	     Log.i(TAG, "URI: " + media.toString());
	     
	     //query external storage using the contentresolver, not managedQuery()
	     Cursor mCursor = mResolver.query(media, null, MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
	     Log.i(TAG, "Query finished. " + (mCursor == null ? "Returned NULL." : "Returned a cursor."));
	     if (mCursor == null) {
	            // Query failed...
	            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
	            return;
	     }
	     if (!mCursor.moveToFirst()) {
	    	 // Nothing to query. There is no music on the device. How boring.
	         Log.e(TAG, "Failed to move cursor to first row (no query results).");
	         return;
	     }
	     Log.i(TAG, "Listing...");
		
	     // retrieve the indices of the columns where the ID, title, etc. of the song are
		 int artistColumn = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
		 int titleColumn = mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
		 int albumColumn = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
		 int durationColumn = mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
		 int idColumn = mCursor.getColumnIndex(MediaStore.Audio.Media._ID);
		 
		 Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
		 Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));
		 

		 // add each song to mItems
		 do {
			 Log.i(TAG, "ID: " + mCursor.getString(idColumn) + " Title: " + mCursor.getString(titleColumn));
			 songs.add(new Song(
					 mCursor.getLong(idColumn),
					 mCursor.getString(artistColumn),
					 mCursor.getString(titleColumn),
					 mCursor.getString(albumColumn),
					 mCursor.getLong(durationColumn)));
		 } while (mCursor.moveToNext());

	        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
	}
	*/
	public ContentResolver getContentResolver() {
        return mResolver;
    }

}
