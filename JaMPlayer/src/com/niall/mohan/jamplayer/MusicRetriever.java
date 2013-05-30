package com.niall.mohan.jamplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import com.niall.mohan.jamplayer.adapters.JamSongs;

/* Does the retrieval of music off the SD card and inserts into the DB.*/
public class MusicRetriever {
	final String TAG = "MusicRetriever";
	ContentResolver mResolver; //this is used to get metadata info...
	MusicTable db;

	public MusicRetriever(Context context) {
	}

	
	public MusicRetriever(ContentResolver mResolver, Context context) {
		db = new MusicTable(context);
		this.mResolver = mResolver;
		pushToDb();
	}
	public void pushToDb() {
		ContentResolver resolver = getContentResolver();
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				MusicTable.MEDIA_PROJECTION, null, null, null);
		cursor.moveToFirst();
		for (int i = 0; i < cursor.getCount(); i++) {
			JamSongs mediaInfo = new JamSongs(cursor.getString(0), cursor.getString(1),"local",cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getInt(5), "",
					cursor.getString(1),cursor.getLong(6));

			db.insert(mediaInfo);
			cursor.moveToNext();
		}
		cursor.close();
		Log.i(TAG, "Pushed to db");
	}
	public ContentResolver getContentResolver() {
        return mResolver;
    }

}
