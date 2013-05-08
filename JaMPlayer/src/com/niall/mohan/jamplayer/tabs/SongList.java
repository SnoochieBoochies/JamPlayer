package com.niall.mohan.jamplayer.tabs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONException;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.model.Song;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.adapters.GPlaySongAdapter;
import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SongList extends ListActivity implements OnClickListener,
		OnSeekBarChangeListener {
	private static String TAG = "SongList";
	ImageButton mPlayPauseButton;
	TextView albumName;
	TextView currentTime;
	TextView totalTime;
	MusicTable db;
	String artist;
	String album;
	String service;
	ProgressDialog progress;
	SeekBar seekbar;
	Handler handler;
	Cursor songCursor;
	SimpleCursorAdapter adapter;
	ArrayList<JamSongs> albumSongs;
	SharedPreferences prefs;
	long lastSeekTime;
	long defaultPos = -1;
	boolean servBound = false;
	JamService mService;
	Intent serviceIntent;
	int seekMax;
	boolean mBroadcastIsRegistered;
	Intent in;
	boolean doRetreive;
	public static final String BROADCAST_SEEKBAR = "com.niall.mohan.jamplayer.sendseekbar";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		artist = intent.getStringExtra("artist");
		album = intent.getStringExtra("album");
		service = intent.getStringExtra("service");
		Log.i(TAG, artist + "/" + album);
		setContentView(R.layout.song_list);
		mPlayPauseButton = (ImageButton) findViewById(R.id.control_play_btn);
		mPlayPauseButton.setOnClickListener(this);
		albumName = (TextView) findViewById(R.id.album_name);
		albumName.setOnClickListener(this);
		seekbar = (SeekBar) findViewById(R.id.progress);
		seekbar.setOnSeekBarChangeListener(this);
		serviceIntent = new Intent(this, JamService.class);

		// --- set up seekbar intent for broadcasting new position to
		// service ---
		in = new Intent(BROADCAST_SEEKBAR);
		currentTime = (TextView) findViewById(R.id.currenttime);
		totalTime = (TextView) findViewById(R.id.totaltime);
		handler = new Handler();
		db = new MusicTable(this);
		db.open();
		fillData();
		if (service.equals("google")) {
			fillUrlData();
		}
		doRetreive = true;
		albumName.setText(album);
		registerReceiver(receiver, new IntentFilter(JamService.ACTION_SKIP));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Intent intent = new Intent(this, JamService.class);
		// bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// if(servBound) {
		// unbindService(mConnection);
		// servBound = false;
		// }

	}

	@Override
	protected void onResume() {
		super.onResume();
		doRetreive = false;
		fillData();
		// fillUrlData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive()");
			updateUI(serviceIntent);
		}
	};

	private void updateUI(Intent serviceIntent) {
		int counter = serviceIntent.getIntExtra("counter", 0);
		int mediamax = serviceIntent.getIntExtra("mediamax", 0);
		// int seekProgress = Integer.parseInt(counter);
		Log.i(TAG, String.valueOf(counter) + ":" + String.valueOf(mediamax));
		seekMax = mediamax;
		seekbar.setMax(seekMax);
		seekbar.setProgress(counter);
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
							.getString(8)));
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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick()");
		Cursor c = ((SimpleCursorAdapter) l.getAdapter()).getCursor();
		c.moveToPosition(position);
		Intent intent = new Intent(JamService.ACTION_PLAY);
		intent.putExtra("position", position);
		intent.putParcelableArrayListExtra("albumsongs", albumSongs);
		startService(intent);
		
		mBroadcastIsRegistered = true;
		doPlayPauseButton();
		super.onListItemClick(l, v, position, id);

	}

	private void doPlayPauseButton() {
		mPlayPauseButton.setImageDrawable(getBaseContext().getResources()
				.getDrawable(R.drawable.playback_toggle));
	}

	@Override
	public void onClick(View v) {
		if (v == mPlayPauseButton) {
			if (v.isSelected()) {
				v.setSelected(false);
				startService(new Intent(JamService.ACTION_PLAY));
				doPlayPauseButton();
				// ...Handle toggle off
			} else {
				v.setSelected(true);
				doPlayPauseButton();
				startService(new Intent(JamService.ACTION_PAUSE));
				// ...Handled toggle on
			}
		} else if (v == albumName) {
			Intent intent = new Intent(JamService.ACTION_PLAY);
			intent.putExtra("position", 0);
			intent.putParcelableArrayListExtra("albumsongs", albumSongs);
			startService(intent);
			doPlayPauseButton();
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		defaultPos = -1;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		lastSeekTime = 0;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			int seekPos = seekbar.getProgress();
			Log.i(TAG, "progress: " + String.valueOf(seekPos));
			in.putExtra("seekpos", seekPos);
			sendBroadcast(in);
		}

	}

	/*
	 * private ServiceConnection mConnection = new ServiceConnection() {
	 * 
	 * @Override public void onServiceDisconnected(ComponentName name) {
	 * servBound = false; }
	 * 
	 * @Override public void onServiceConnected(ComponentName name, IBinder
	 * service) { JamBinder binder = (JamBinder) service; mService =
	 * binder.getService(); servBound = true; } };
	 */
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
				SongAdapter temp = new SongAdapter(list.get(i));
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
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

		private class SongAdapter extends Song {
			JamSongs song;

			public SongAdapter(JamSongs song) {
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

}
