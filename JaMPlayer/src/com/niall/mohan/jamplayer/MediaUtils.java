package com.niall.mohan.jamplayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.ExpandableListAdapter;
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
	private static IJamService sService;

	private static final Uri sArtworkUri = Uri
			.parse("content://media/external/audio/albumart");
	private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
	private static Bitmap mCachedBit = null;

	// sConnectionMap contains all binded MediaPlaybackService
	private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

	// all medias which will be binded with MediaAdapter
	private static ArrayList<JamSongs> medias = new ArrayList<JamSongs>();

	private static MediaUtils mediaUtils;

	public static MediaUtils getInstance() {
		if (mediaUtils == null) {
			mediaUtils = new MediaUtils();
		}
		return mediaUtils;
	}

	public static class ServiceToken {
		ContextWrapper mWrappedContext;

		ServiceToken(ContextWrapper context) {
			mWrappedContext = context;
		}
	}

	public static ServiceToken bindToService(Activity context,
			ServiceConnection callback) {
		Activity realActivity = context.getParent();
		if (realActivity == null) {
			realActivity = context;
		}
		ContextWrapper cw = new ContextWrapper(realActivity);
		ServiceBinder sb = new ServiceBinder(callback);
		if (cw.bindService((new Intent()).setClass(cw,
				JamService.class), sb, Context.BIND_AUTO_CREATE)) {
			sConnectionMap.put(cw, sb);
			Log.i("test", "sConnectionMap size is " + sConnectionMap.size());
			return new ServiceToken(cw);
		}
		Log.e("Music", "Failed to bind to service");
		return null;
	}

	public static void unbindFromService(ServiceToken token) {
		if (token == null) {
			Log.e("MusicUtils", "Trying to unbind with null token");
			return;
		}
		ContextWrapper cw = token.mWrappedContext;
		ServiceBinder sb = sConnectionMap.remove(cw);
		if (sb == null) {
			Log.e("MusicUtils", "Trying to unbind for unknown Context");
			return;
		}
		cw.unbindService(sb);
		if (sConnectionMap.isEmpty()) {
			// presumably there is nobody interested in the service at this
			// point,
			// so don't hang on to the ServiceConnection
			sService = null;
		}
		Log.i("test", "sConnectionMap size is " + sConnectionMap.size());
	}

	private static class ServiceBinder implements ServiceConnection {

		ServiceConnection mCallback;

		ServiceBinder(ServiceConnection callback) {
			mCallback = callback;
		}

		public void onServiceConnected(ComponentName className,
				android.os.IBinder service) {
			sService = IJamService.Stub.asInterface(service);
			if (mCallback != null) {
				mCallback.onServiceConnected(className, service);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			if (mCallback != null) {
				mCallback.onServiceDisconnected(className);
			}
			sService = null;
		}
	}

	public void play(int position) {

		try {
			sService.play(position);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	public void next() {
		try {
			sService.next();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void prev() {
		try {
			sService.prev();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public long duration() {
		if (sService != null) {
			try {
				return sService.duration();
			} catch (RemoteException ex) {
			}
		}
		return -1;
	}

	public static void scanSdCard(Context context, int cmd) {
		Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
				.parse("file://"
						+ Environment.getExternalStorageDirectory()
								.getAbsolutePath()));
		intent.putExtra("cmd", cmd);
		context.sendBroadcast(intent);
	}

	public void bind_data_adapter(Context context, String tabId) {
		if (context == null || tabId == null) {
			return;
		}
		List<String> father = null;
		List<JamSongs> child = null;
		ContentResolver resolver = context.getContentResolver();
		//if (Data_Googles.equals(tabId)) {
		//	father = find_father_data(resolver, new String[] { tabId });
	//		Log.i(MediaUtilsTag, "father:" + father.size());
		//	ExpandableListAdapter expandAdapter = (ExpandableListAdapter) TabsActivity.exp_list_adapter;
			//expandAdapter.onDatasetChanged(father, child);
		//	return;
		//} else if (Data_SoundCloud.equals(tabId)) {
		//	MusicTable mediaSqlite = new MusicTable(context);
		//	father = mediaSqlite
		//			.query(new String[] { MusicTable.PLAYLIST });
		//} else 
		if (Data_Local.equals(tabId)) {
			father = find_father_data(resolver,
					new String[] { MusicTable.ARTIST });
		} else {
			father = find_father_data(resolver, new String[] { tabId });
		}
		MediaAdapter mediaAdapter = (MediaAdapter) TabsActivity.displayAdapter;
		//mediaAdapter.onDatasetChanged(father, null);
	}

	public List<JamSongs> find_child_data(ContentResolver resolver,
			String where, String[] values) {
		ArrayList<JamSongs> childs = new ArrayList<JamSongs>();
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				MusicTable.MEDIA_PROJECTION, where + "=?", values, null);
		if (cursor == null)
			return null;
		cursor.moveToFirst();
		final int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			JamSongs child = new JamSongs(cursor.getString(0), cursor.getString(1),"local",cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getString(5));
			childs.add(child);
			cursor.moveToNext();
		}
		return childs;
	}

	public List<String> find_father_data(ContentResolver resolver, String[] type) {
		ArrayList<String> fathers = new ArrayList<String>();
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, type, null, null,
				null);
		if (cursor == null)
			return null;
		cursor.moveToFirst();
		final int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			String father = cursor.getString(0);
			fathers.add(father);
			cursor.moveToNext();
		}
		unduplicate(fathers);
		return fathers;
	}


	public void setShuffleMode() {

		if (sService == null) {
			Log.i(MediaUtilsTag, "sService is dead");
			return;
		}

		try {
			sService.setShuffleMode();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void setRepeatMode() {
		if (sService == null) {
			Log.i(MediaUtilsTag, "sService is dead");
			return;
		}
		try {
			sService.setRepeatMode();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public int getRepeatMode() {
		if (sService == null) {
			Log.i(MediaUtilsTag, "sService is dead");
			return -1;
		}
		int repeat_mode = 0;
		try {
			repeat_mode = sService.getRepeatMode();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return repeat_mode;
	}

	public int getShuffleMode() {
		if (sService == null) {
			Log.i(MediaUtilsTag, "sService is dead");
			return -1;
		}
		int shuffle_mode = 0;
		try {
			shuffle_mode = sService.getShuffleMode();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return shuffle_mode;
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
