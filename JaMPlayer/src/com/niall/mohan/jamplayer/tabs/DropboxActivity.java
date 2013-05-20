package com.niall.mohan.jamplayer.tabs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNDescriptor;
import com.gracenote.mmid.MobileSDK.GNOperationStatusChanged;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;
import com.gracenote.mmid.MobileSDK.GNStatus;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.WriteToCache;
import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Toast;

public class DropboxActivity extends ExpandableListActivity implements OnClickListener  {
	private static String TAG = "DropboxActivity";
	public static ArtistAlbumListAdapter adapter;
	private String currentArtist;
	private String currentArtistId;
	private String currentAlbum;
	private String currentAlbumId;
	private String currentService;
	private Cursor artistCursor;
	public MusicTable db;
	public static Intent intent;
	SharedPreferences prefs;
	ArrayList<JamSongs> albumSongs;
	ProgressDialog dropboxProgress;
	WriteToCache newFolder;
	public DropboxAPI<AndroidAuthSession> mApi;
	GNConfig config;
	boolean firstTime = true;
	String key;
	ImageButton nowPlayingArtBtn;
	Button nowPlayingTitleBtn;
	View border;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if(savedInstanceState != null) {
			currentArtist = savedInstanceState.getString("selectedartist");
			currentArtistId = savedInstanceState.getString("selectedartistid");
			currentAlbum = savedInstanceState.getString("currentalbum");
			currentAlbumId = savedInstanceState.getString("selectedalbumid");

		}
		setContentView(R.layout.tab_content_layout);
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(JamService.ACTION_NOW_PLAYING));
		prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
		key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
		String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
		if(key == null) {
			Toast.makeText(getApplicationContext(), "Connect with Dropbox first.", Toast.LENGTH_SHORT).show();
		} else {
			Log.i(TAG, key);
			Log.i(TAG, secret);
			AppKeyPair appKeyPair = new AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET);
			AccessTokenPair accessToken = new AccessTokenPair(key, secret);
	        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE, accessToken);
			mApi = new DropboxAPI<AndroidAuthSession>(session);
			db = new MusicTable(this);
			db.open();
			albumSongs = new ArrayList<JamSongs>();
			fillData();
			newFolder = new WriteToCache();
			config = GNConfig.init(Constants.GRACENOTE_KEY, getApplicationContext());
			config.setProperty("content.coverArt", "1");
		}
		
	}
	private void fillData() {
		Log.i(TAG, "fillData()");
		boolean first = prefs.getBoolean("firstTime", true);
		if(first) {
			DropboxSongsTask task = new DropboxSongsTask() {
				@Override
				protected void onPostExecute(Boolean result) {
		            dropboxProgress.dismiss();
					super.onPostExecute(result);
				}
			};
			task.execute();
			artistCursor = db.getArtistsByService("dropbox");
			startManagingCursor(artistCursor);
			adapter = new ArtistAlbumListAdapter(artistCursor, this,
					android.R.layout.simple_expandable_list_item_1,android.R.layout.simple_expandable_list_item_1, 
					new String [] {MusicTable.ARTIST}, new int [] {android.R.id.text1}, new String [] {MusicTable.ALBUM}, new int [] {android.R.id.text1});
			setListAdapter(adapter);
			firstTime = false;
			Editor writer = prefs.edit();
			writer.putBoolean("firstTime", firstTime);
			writer.commit();
		} else {
			artistCursor = db.getArtistsByService("dropbox");
			startManagingCursor(artistCursor);
			adapter = new ArtistAlbumListAdapter(artistCursor, this,
					android.R.layout.simple_expandable_list_item_1,android.R.layout.simple_expandable_list_item_1, 
					new String [] {MusicTable.ARTIST}, new int [] {android.R.id.text1}, new String [] {MusicTable.ALBUM}, new int [] {android.R.id.text1});
			setListAdapter(adapter);
		}
		db.close();

			
	}
	private BroadcastReceiver nowPlaying = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.i(TAG, "onReceiver google");
			Log.i(TAG, intent.getStringExtra("title"));
			nowPlayingArtBtn = (ImageButton) findViewById(R.id.art_thumb);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			Bitmap bm = intent.getParcelableExtra("art");
			nowPlayingArtBtn.setImageBitmap(Bitmap.createScaledBitmap(bm, 120, 80, false));
			nowPlayingArtBtn.setOnClickListener(DropboxActivity.this);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn = (Button) findViewById(R.id.art_text);
			nowPlayingTitleBtn.setText(intent.getStringExtra("title"));
			nowPlayingTitleBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn.setOnClickListener(DropboxActivity.this);
			border = (View) findViewById(R.id.border);
			border.setVisibility(View.VISIBLE);
		}
	};
	@Override
	protected void onResume() {
		super.onResume();
		if(key != null)
			fillData();
	}
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,int groupPosition, int childPosition, long id) {
		Log.i(TAG, "onChildClick()");
		currentAlbumId = Long.valueOf(id).toString();
		Cursor malbumCur = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
		currentAlbum = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.ALBUM));
		currentService = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.SERVICE_TYPE));
		intent = new Intent(this, SongList.class);
		intent.putExtra("albumId", currentAlbumId);
		//unknown album
		if(currentAlbum == null || currentAlbum.equals(MediaStore.UNKNOWN_STRING)) {
			artistCursor.moveToPosition(groupPosition);
			currentArtistId = artistCursor.getString(artistCursor.getColumnIndex(MusicTable.ARTIST));
			intent.putExtra("artist", currentArtistId);
		}
		intent.putExtra("album", currentAlbum);
		currentArtist = artistCursor.getString(1);
		intent.putExtra("artist", currentArtist);
		intent.putExtra("service", currentService);
		startActivity(intent);
		return true;
	}
	public class DropboxSongsTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected void onPreExecute() {
			dropboxProgress = ProgressDialog.show(DropboxActivity.this, "", "Constructing Dropbox Songs. \n" +
            		"Please wait...",true);
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean success = false;
			try {
				final ArrayList<Entry> files = new ArrayList<DropboxAPI.Entry>();
	            String [] formats = {".mp3",".flac",".ogg",".wav"}; //supported formats of the MEdiaPlayer
	            for(int i = 0; i < formats.length; i++)
	            	files.addAll((ArrayList<Entry>) mApi.search("/", formats[i], 100, false));

	            File f = newFolder.getAlbumStorageDir();
	            File [] fa = f.listFiles();
	            Log.i(TAG,fa[0].getAbsolutePath()+fa[1].getAbsolutePath());
	            final RecognizePCMStreamTask task = new RecognizePCMStreamTask();
	            task.mconfig = config;
	            Thread graceNoteThread = new Thread(new Runnable() {
					@Override
					public void run() {
			            try {
				            for(Entry e : files) {
				            	DropboxLink url = mApi.media(e.path, false);
				            	//check the file type as only specific formats can be recognised by gracenote's servers.
				            	if(e.mimeType.contains("mpeg") || e.mimeType.contains("wav") || e.mimeType.contains("wave") || e.mimeType.contains("aac")) {
				            		Log.i(TAG, "SYNC");
									//synchronized (albumSongs) {
										task.recognisePcmStream(url.url, e.path);
									//};
				            	} 
				            }
						}catch (DropboxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					
				});
	            graceNoteThread.run();
	            success = true;
			} catch (DropboxException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}  
			return success;
		}
		
	}
	private class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter {

		public ArtistAlbumListAdapter(Cursor cursor, Context context,
                int groupLayout, int childLayout, String[] groupFrom,
                int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
                        childLayout, childrenFrom, childrenTo);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			Cursor albumCursor = db.getArtistsAlbumsByService(groupCursor.getString(groupCursor.getColumnIndex("service")),groupCursor.getString(groupCursor.getColumnIndex("artist")));// = db.getArtistsByService("local");
			startManagingCursor(albumCursor);
			//albumCursor.moveToFirst();
			return albumCursor;
		}
		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			Log.i(TAG,"childview");

			return super.getChildView(groupPosition, childPosition, isLastChild,
					convertView, parent);
		}

	}
	public class RecognizePCMStreamTask implements GNSearchResultReady, GNOperationStatusChanged {
    	GNConfig mconfig;
    	String songPath;
    	public void recognisePcmStream(String url, String dropboxPath) throws IOException {
    		//GNSampleBuffer buffer = loadSamplePCM();
    		//GNOperations.recognizeMIDFileFromPcm(this, config, buffer);
    		Log.i(TAG, url);
    		File file = new File(newFolder.getAlbumStorageDir()+"/temp.mp3");
    	    if(file.exists()) {
    	    	if(file.delete())
    	    		Log.i(TAG, "file deleted!!");
    	    }
    	    
    		HttpURLConnection con;
    		URL url2 = new URL(url);
    		con = (HttpURLConnection) url2.openConnection();
    		con.setRequestProperty("Range", "bytes=0-960000");
    		InputStream is = con.getInputStream();
    		
    		FileOutputStream fileOut = new FileOutputStream(file);
    		byte[] buf = new byte[960000];
    		int n = 0;  
    	    while ((n=is.read(buf))>=0) {  
    	       fileOut.write(buf, 0, n);  
    	    }
    	    fileOut.close();
    	    fileOut.flush();
    	    if(file.exists()) {
    	    	Log.i(TAG, "temp.mp3 exists!");
    	    	songPath = dropboxPath;
    	    	GNOperations.recognizeMIDFileFromFile(this, config, newFolder.getAlbumStorageDir()+"/temp.mp3");
    	    }
    	    con.disconnect();    	    	    
			file.deleteOnExit();
    	}
		@Override
		public void GNStatusChanged(GNStatus status) {
			// TODO Auto-generated method stub
			Log.i(TAG, "GNStatusChanged() "+ status.getMessage());
			//updateStatus(status.getMessage(), true);
		}

		@Override
		public void GNResultReady(GNSearchResult result) {
			if (result.isFailure()) {
				// An error occurred so display the error to the user.
				String msg = String.format("[%d] %s", result.getErrCode(),
				result.getErrMessage());
				updateStatus(msg, false); // Display error while leaving the
				// prior status update
			} else {
				if (result.isFingerprintSearchNoMatchStatus()) {
					// Handle special case of webservices lookup with no match
					Log.i("EWRGON","NONE");
				} else {
					GNSearchResponse bestResponse = result.getBestResponse();
					Log.i("RESPONsssssE",bestResponse.getArtist()+ " "+bestResponse.getAlbumTitle()+ " "+bestResponse.getTrackTitle()+ " "+bestResponse.getSongPosition()+ " ");
					JamSongs temp = new JamSongs(bestResponse.getTrackTitle(), songPath, "dropbox", bestResponse.getAlbumTitle(),
							String.valueOf(0), bestResponse.getArtist(), bestResponse.getTrackNumber(), bestResponse.getTrackId(),
							bestResponse.getCoverArt().getUrl(), 0);
					albumSongs.add(temp);
					db.insert(temp);
					Log.i(TAG, String.valueOf(albumSongs.size()));
				}
			updateStatus("Success", true);
			}
		}
		private void updateStatus(String status, boolean clearStatus) {
		     if (clearStatus) {
		    	 Log.i("TAB", status);
		     } else {
		    	 Log.i("TAB", status + status);
		     }
		}

    }
	@Override
	public void onClick(View v) {
		if(v == nowPlayingArtBtn || v == nowPlayingTitleBtn) {
			Intent intent = new Intent(getApplicationContext(), PlayingActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		}
	}
}