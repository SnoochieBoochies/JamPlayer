package com.niall.mohan.jamplayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.TabsActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;


public class MediaUtils {

	public static String MediaUtilsTag = "MediaUtils";

	public static final String Data_Googles = "Google";
	public static final String Data_SoundCloud = "Soundcloud";
	public static final String Data_Dropbox = "Dropbox";
	public static final String Data_Local = "Local";

	public String Data_TYPE = Data_Local;


	private static final Uri sArtworkUri = Uri
			.parse("content://media/external/audio/albumart");
	private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
	private static Bitmap mCachedBit = null;


	// all medias which will be binded with MediaAdapter
	private static ArrayList<JamSongs> medias = new ArrayList<JamSongs>();

	private static MediaUtils mediaUtils;

	public static MediaUtils getInstance() {
		if (mediaUtils == null) {
			mediaUtils = new MediaUtils();
		}
		return mediaUtils;
	}




	public static void scanSdCard(Context context, int cmd) {
		Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
				.parse("file://"
						+ Environment.getExternalStorageDirectory()
								.getAbsolutePath()));
		intent.putExtra("cmd", cmd);
		context.sendBroadcast(intent);
	}


	public static void unduplicate(List<String> list) {

		Log.i(MediaUtilsTag, "unduplicate " + list.size());

		for (int i = 0; i < list.size(); i++) {
			for (int y = i + 1; y < list.size(); y++) {
				if (list.get(i).equals(list.get(y))) {
					list.remove(y);
					y--;
				}
			}
		}
	}
	
}
