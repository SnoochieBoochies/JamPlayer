package com.niall.mohan.jamplayer;

import java.io.File;

import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class WriteToCache {
	private static final String name = "JaM Music";
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	public File getAlbumStorageDir() {
	    // Get the directory for the app's public music directory.
		File file;
		if(isExternalStorageWritable()) {
			file = new File(Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_MUSIC), name);
			Log.i("getAlbumStoageDir", file.getAbsolutePath());
			 if (!file.mkdirs()) {
			        Log.e("getAlbumStorageDir", "Directory not created");
			 }
			return file;
		} else {
			return null;
		}
	} 


}
