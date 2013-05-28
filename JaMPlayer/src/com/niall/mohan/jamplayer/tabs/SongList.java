package com.niall.mohan.jamplayer.tabs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONException;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.model.Song;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;
import com.niall.mohan.jamplayer.adapters.GPlaySongAdapter;
import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView.BufferType;
import android.widget.TextView;
import android.widget.Toast;

public class SongList extends ListActivity implements OnClickListener {
	private static String TAG = "SongList";
	TextView albumName;
	MusicTable db;
	String artist;
	String album;
	String service;
	ProgressDialog progress;
	Cursor songCursor;
	SimpleCursorAdapter adapter;
	public ArrayList<JamSongs> albumSongs;
	SharedPreferences prefs;
	boolean servBound = false;
	JamService mService;
	boolean doRetreive;
	public DropboxAPI<AndroidAuthSession> mApi;
	ImageButton nowPlayingArtBtn;
	Button nowPlayingTitleBtn;
	View border;
	private boolean isPaused;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		artist = intent.getStringExtra("artist");
		album = intent.getStringExtra("album");
		service = intent.getStringExtra("service");
		Log.i(TAG, artist + "/" + album);
		setContentView(R.layout.song_list);
		albumName = (TextView) findViewById(R.id.album_name);
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(JamService.ACTION_NOW_PLAYING));
		LocalBroadcastManager.getInstance(this).registerReceiver(paused, new IntentFilter(JamService.CHECK_PAUSED));
		albumName.setOnClickListener(this);
		db = new MusicTable(this);
		db.open();
		fillData();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor writer = prefs.edit();
		writer.remove("paused");
		writer.commit();
		if (service.equals("google")) {
			fillUrlData();
		} else if(service.equals("dropbox")) {
			buildDbSess();
			fillDbUrlData();
		}
		doRetreive = true;
		albumName.setText(album);
		
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
			nowPlayingArtBtn.setOnClickListener(SongList.this);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn = (Button) findViewById(R.id.art_text);
			nowPlayingTitleBtn.setText(intent.getStringExtra("title"));
			nowPlayingTitleBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn.setOnClickListener(SongList.this);
			border = (View) findViewById(R.id.border);
			border.setVisibility(View.VISIBLE);
		}
	};
	private BroadcastReceiver paused = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			isPaused = intent.getBooleanExtra("paused", false);
			Log.i(TAG, String.valueOf(isPaused));
			Editor writer = prefs.edit();
			writer.putBoolean("paused", isPaused);
			writer.commit();
		}
	};
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		doRetreive = false;
		fillData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}

	@SuppressWarnings("deprecation")
	private void fillData() {
		albumSongs = new ArrayList<JamSongs>();
		songCursor = db.getAlbumSongs(service, artist, album);
		startManagingCursor(songCursor);
		final int count = songCursor.getCount();
		for (int i = 0; i < count; i++) {
			albumSongs.add(
					i,
					new JamSongs(songCursor.getString(1), songCursor
							.getString(4), songCursor.getString(6), songCursor
							.getString(2), songCursor.getString(5), songCursor
							.getString(3), songCursor.getInt(7), songCursor
							.getString(8),songCursor.getString(9), songCursor.getLong(10)));
			Log.i(TAG, albumSongs.get(i).getPath());
			songCursor.moveToNext();

		}

		adapter = new SimpleCursorAdapter(this, R.layout.list_child_item,
				songCursor, new String[] { MusicTable.TITLE },
				new int[] { R.id.child_text });
		setListAdapter(adapter);
	}

	private void fillUrlData() {
		RetreiveGoogleUrl urlTask = new RetreiveGoogleUrl(SongList.this) {
			@Override
			protected void onPostExecute(ArrayList<JamSongs> result) {
				albumSongs = result;

				super.onPostExecute(result);
				progress.dismiss();
			}
		};
		urlTask.execute(albumSongs);
		adapter = new SimpleCursorAdapter(this, R.layout.list_child_item,
				songCursor, new String[] { MusicTable.TITLE },
				new int[] { R.id.child_text });
		setListAdapter(adapter);
		db.close();
		doRetreive = false;
	}
	private void fillDbUrlData() {
		RetreiveDropboxUrls urlTask = new RetreiveDropboxUrls() {
			@Override
			protected void onPostExecute(ArrayList<JamSongs> result) {
				albumSongs = result;
				super.onPostExecute(result);
				progress.dismiss();
			}
		};
		urlTask.execute(albumSongs);
		adapter = new SimpleCursorAdapter(this, R.layout.list_child_item,
				songCursor, new String[] { MusicTable.TITLE },
				new int[] { R.id.child_text });
		setListAdapter(adapter);
		db.close();
		doRetreive = false;
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick()");
		Cursor c = ((SimpleCursorAdapter) l.getAdapter()).getCursor();
		c.moveToPosition(position);
		Log.i(TAG, c.getString(c.getColumnIndex("title")));
		final Intent play = new Intent(getApplicationContext(), PlayingActivity.class);
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		play.putExtra("songTitle", c.getString(c.getColumnIndex("title")));
		play.putExtra("position", position);
		play.putParcelableArrayListExtra("albumsongs", albumSongs);
		isPaused = prefs.getBoolean("paused", false);
		System.out.println(isPaused);
		if(am.isMusicActive() || isPaused == true) {
			Log.i(TAG, "doing skip");
			play.putExtra("action", "skip");
			play.putExtra("position", position);
			play.putParcelableArrayListExtra("albumsongs", albumSongs);
			play.putExtra("paused", isPaused);
			startActivity(play);
		} else{
			startActivity(play);
		}
		//doPlayPauseButton();
		super.onListItemClick(l, v, position, id);

	}

	@Override
	public void onClick(View v) {
		if (v == albumName) {
			Intent play = new Intent(getApplicationContext(), PlayingActivity.class);
			play.putExtra("position", 0);
			play.putParcelableArrayListExtra("albumsongs", albumSongs);
			startActivity(play);
			//doPlayPauseButton();
		} else if(v == nowPlayingArtBtn || v == nowPlayingTitleBtn) {
			Intent intent = new Intent(getApplicationContext(), PlayingActivity.class);
			//intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		}
	}

	public class RetreiveGoogleUrl extends
			AsyncTask<ArrayList<JamSongs>, Void, ArrayList<JamSongs>> {
		private static final String TAG2 = "RetreiveGoogleUrl";
		protected SongList mActivity;

		public RetreiveGoogleUrl(SongList mActivity) {
			this.mActivity = mActivity;
		}

		@Override
		protected ArrayList<JamSongs> doInBackground(
				ArrayList<JamSongs>... params) {
			Log.i(TAG2, "doInBackground()");
			boolean success = false;
			prefs = PreferenceManager
					.getDefaultSharedPreferences(SongList.this);
			String em = prefs.getString("Google_email", "");
			String toke = prefs.getString("Google_token", "");
			GoogleMusicApi.createInstance(mActivity);
			success = GoogleMusicApi.login(mActivity, toke);
			if (!success)
				GoogleAuthUtil.invalidateToken(mActivity, toke);
			Log.i(TAG, "EMAIL FIRST" + em);
			ArrayList<JamSongs> list = params[0];
			for (int i = 0; i < list.size(); i++) {
				GoogleAdapter temp = new GoogleAdapter(list.get(i));
				Song s = temp.setMediaInfo(list.get(i)); // convert to google song and make url
				Log.i(TAG2, s.getId());
				try {
					URI songURL;
					songURL = GoogleMusicApi.getSongStream(s);
					// Log.i(TAG2, songURL.toString());
					s.setUrl(String.valueOf(songURL));			
					list.get(i).setPath(String.valueOf(songURL));
					Log.i(TAG2, list.get(i).getPath());
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				} catch (Exception e) {
					try {
						success = false;
						prefs = PreferenceManager
								.getDefaultSharedPreferences(SongList.this);
						String email = prefs.getString("Google_email", "");
						Log.i(TAG, "EMAIL SECOND" + email);
						String token = GoogleAuthUtil.getToken(mActivity,
								email, "sj");
						Log.i(TAG, "TOKE MOTHERFUCKER: \n" + token);
						if (!TextUtils.isEmpty(token)) {
							GoogleMusicApi.createInstance(mActivity);
							success = GoogleMusicApi.login(mActivity, token);
							prefs = PreferenceManager
									.getDefaultSharedPreferences(SongList.this);
							Editor writer = prefs.edit();
							writer.putString("Google_token", token);
							writer.putString("Google_email", email);
							writer.commit();
							if (!success)
								GoogleAuthUtil
										.invalidateToken(mActivity, token);
						}
					} catch (GooglePlayServicesAvailabilityException playEx) {
						// GooglePlayServices.apk is either old, disabled, or
						// not present.
						// mActivity.showDialog(playEx.getConnectionStatusCode());
						GooglePlayServicesUtil.getErrorDialog(
								playEx.getConnectionStatusCode(), mActivity,
								1001).show();
					} catch (UserRecoverableAuthException e1) {
						mActivity.startActivityForResult(e1.getIntent(), 1001);
						e.printStackTrace();
					} catch (IOException e2) {
						e.printStackTrace();
					} catch (GoogleAuthException e3) {
						e.printStackTrace();
					}
				}
			}
			return list;
		}

		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(mActivity, "", "Loading Album...",
					true);
			super.onPreExecute();
		}

		private class GoogleAdapter extends Song {
			JamSongs song;

			public GoogleAdapter(JamSongs song) {
				this.song = song;
			}

			public Song setMediaInfo(JamSongs s) {
				Song mediaInfo = new Song();
				mediaInfo.setTitle(s.getTitle());
				mediaInfo.setAlbum(s.getAlbum());
				mediaInfo.setArtist(s.getArtist());
				mediaInfo.setDurationMillis(Long.valueOf(s.getDuration()));
				mediaInfo.setTrack(s.getTrackNum());
				mediaInfo.setId(s.getId());
				return mediaInfo;
			}
		}
	}
	private void buildDbSess() {
		prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
		String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
		Log.i(TAG, key);
		Log.i(TAG, secret);
		AppKeyPair appKeyPair = new AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET);
		AccessTokenPair accessToken = new AccessTokenPair(key, secret);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE, accessToken);
		mApi = new DropboxAPI<AndroidAuthSession>(session);
	}
	public class RetreiveDropboxUrls extends AsyncTask<ArrayList<JamSongs>, Void, ArrayList<JamSongs>> {
		private static final String TAG4 = "RetreiveDropboxUrls";
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progress = ProgressDialog.show(SongList.this, "", "Loading Album...",
					true);
		}
		@Override
		protected ArrayList<JamSongs> doInBackground(ArrayList<JamSongs>... params) {
			Log.i(TAG4, "doInBackground()");
			ArrayList<JamSongs> songs = params[0];
			try {
				JamSongs tempSong;
	            for(int i = 0; i < songs.size(); i++) {
	            	DropboxLink url = mApi.media(songs.get(i).getPath(), false);
	            	//Log.i(TAG, url.url);
	            	songs.get(i).setPath(url.url);	
	            }
			} catch(DropboxException e) {
				e.printStackTrace();
			}
			return songs;
		}
		
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				this.startActivity(intent);
				return true;
		}
		return false;
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sub_menu_one, menu);
		return true;
	}
}
