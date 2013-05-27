package com.niall.mohan.jamplayer.tabs;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;
import com.niall.mohan.jamplayer.WriteToCache;
import com.niall.mohan.jamplayer.adapters.JamSongs;

public class PlayingActivity extends Activity implements OnClickListener, OnSeekBarChangeListener{
	private static String TAG = "PlayingActivity";
	ImageButton mPlayPauseButton;
	TextView songName;
	TextView currentTime;
	TextView totalTime;
	SeekBar seekbar;
	Handler handler;
	ImageView albumArt;
	SharedPreferences prefs;
	boolean servBound = false;
	JamService mService;
	Intent serviceIntent;
	int seekMax;
	boolean mBroadcastIsRegistered;
	Intent in;
	private int position;
	private ArrayList<JamSongs> albumSongs;
	MusicTable db;
	Bitmap art;
	String action;
	static String artUri;
	static long albId;
	private boolean isPaused;
	WriteToCache cache = new WriteToCache();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playing_item);
		albumArt = (ImageView) findViewById(R.id.album_art);
		mPlayPauseButton = (ImageButton) findViewById(R.id.control_play_btn);
		mPlayPauseButton.setOnClickListener(this);
		songName = (TextView) findViewById(R.id.song_name);
		songName.setOnClickListener(this);
		seekbar = (SeekBar) findViewById(R.id.progress);
		seekbar.setOnSeekBarChangeListener(this);
		serviceIntent = new Intent(this, JamService.class);
		registerReceiver(seekReceiver, new IntentFilter(JamService.ACTION_SEEK));
		in = new Intent(Constants.BROADCAST_SEEKBAR);
		currentTime = (TextView) findViewById(R.id.currenttime);
		totalTime = (TextView) findViewById(R.id.totaltime);
		final Intent receive = getIntent();
		albumSongs = new ArrayList<JamSongs>();
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(JamService.ACTION_NOW_PLAYING));
		LocalBroadcastManager.getInstance(this).registerReceiver(paused, new IntentFilter(JamService.CHECK_PAUSED));
		final WriteToCache cache = new WriteToCache();
		prefs = PreferenceManager.getDefaultSharedPreferences(PlayingActivity.this);
		String artUriForPrefs = prefs.getString("artwork", "null");
		Log.i(TAG, "URI "+artUriForPrefs);
		long albIdForPrefs = prefs.getLong("id", 0);
		String act = prefs.getString("skip", "null");
		int positionPrefs = prefs.getInt("position", 0);
		Log.i(TAG, String.valueOf(positionPrefs));
		AudioManager am = (AudioManager) PlayingActivity.this.getSystemService(Context.AUDIO_SERVICE);
		action = receive.getStringExtra("action");
		position = receive.getIntExtra("position", -1);
		albumSongs = receive.getParcelableArrayListExtra("albumsongs");
		isPaused = receive.getBooleanExtra("paused", false);
		if(am.isMusicActive() || isPaused) {
			try {
				action = receive.getStringExtra("action");
				position = receive.getIntExtra("position", 0);
				albumSongs = receive.getParcelableArrayListExtra("albumsongs");
				ArrayList<JamSongs> temp = cache.getArrayList();
				if(temp.equals(albumSongs)) {
					songName.setText(receive.getStringExtra("songTitle"));
					artUri = albumSongs.get(position).getArtwork();
					albId = albumSongs.get(position).getAlbumId();
					art = cache.getArtwork(artUri, albId, getApplicationContext());
					albumArt.setImageBitmap(art);
					if(action.equals("skip")) {
						Intent skip = new Intent(JamService.ACTION_SKIP);
						skip.putExtra("position", position);
						skip.putParcelableArrayListExtra("albumsongs", albumSongs);
						skip.putExtra("art", art);
						skip.putExtra("skip", action);
						Log.i(TAG, "do skip");
						startService(skip);
					} else {//else i'm going back into the activity from pressing one of the buttons.
						songName.setText(prefs.getString("songTitle", ""));
						final String tempArtUri = prefs.getString("artwork", "null");
						final long tempId = prefs.getLong("id", 0);
						Thread mis = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									art = cache.getArtwork(tempArtUri, tempId, getApplicationContext());
									albumArt.setImageBitmap(art);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						});
						mis.run();
					}
					cache.writeArrayList(albumSongs);
				} else {
					songName.setText(temp.get(0).getTitle());
					artUri = temp.get(0).getArtwork();
					albId = temp.get(0).getAlbumId();
					art = cache.getArtwork(artUri, albId, getApplicationContext());
					albumArt.setImageBitmap(art);
				}
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		} else {
			//Intent receive = getIntent();
			position = receive.getIntExtra("position", 0);
			albumSongs = receive.getParcelableArrayListExtra("albumsongs");
			songName.setText(receive.getStringExtra("songTitle"));
			action = receive.getStringExtra("action");
			artUri = albumSongs.get(position).getArtwork();
			albId = albumSongs.get(position).getAlbumId();

			try {
				art = cache.getArtwork(artUri, albId, getApplicationContext());
			} catch (IOException e) {
				e.printStackTrace();
			}
			albumArt.setImageBitmap(art);
			
		}
		Editor writer = prefs.edit();
		writer.putString("artwork", artUri);
		writer.putLong("id", albId);
		writer.putString("skip", action);	
		writer.commit();
		//loader.setVisibility(View.GONE);
		db = new MusicTable(this);
		db.open();
		doPlayPauseButton();
		Intent intent = new Intent(JamService.ACTION_PLAY);
		intent.putExtra("position", position);
		intent.putParcelableArrayListExtra("albumsongs", albumSongs);
		intent.putExtra("art", art);
		startService(intent);
		
		
		
	}
	private BroadcastReceiver nowPlaying = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.i(TAG, "onReceiver google");
			Log.i(TAG, intent.getStringExtra("title"));
			songName.setText(intent.getStringExtra("title"));
		}
	};
	private BroadcastReceiver paused = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			isPaused = intent.getBooleanExtra("paused", false);
			Log.i(TAG, String.valueOf(isPaused));
		}
	};
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Log.i(TAG, artUri);
		Editor writer = prefs.edit();
		writer.putString("artwork", artUri);
		writer.putLong("id", albId);
		writer.putString("songTitle", songName.getText().toString());
		writer.commit();
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "Saved state");
		super.onSaveInstanceState(outState);
		outState.putParcelable("art", art);
		outState.putInt("position", position);
		outState.putParcelableArrayList("albumsongs", albumSongs);
		outState.putString("songTitle", songName.getText().toString());
		outState.putString("action", action);
	}
	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		unregisterReceiver(seekReceiver);
	}
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(seekReceiver, new IntentFilter(JamService.ACTION_SEEK));
	}

	private void doPlayPauseButton() {
		mPlayPauseButton.setImageDrawable(getBaseContext().getResources()
				.getDrawable(R.drawable.playback_toggle));
	}
	private BroadcastReceiver seekReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive()");
			int counter = intent.getIntExtra("counter", 0);
			int mediamax = intent.getIntExtra("mediamax", 0);
			seekMax = mediamax;
			currentTime.setText(intent.getStringExtra("currentTime"));
			totalTime.setText(intent.getStringExtra("endTime"));
			seekbar.setMax(seekMax);
			seekbar.setProgress(counter);
		}
	};
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean fromUser) {
		if (fromUser) {
			int seekPos = seekbar.getProgress();
			//Log.i(TAG, "progress: " + String.valueOf(seekPos));
			in.putExtra("seekpos", seekPos);
			sendBroadcast(in);
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onClick(View arg0) {
		if (arg0 == mPlayPauseButton) {
			if (arg0.isSelected()) {
				arg0.setSelected(false);
				startService(new Intent(JamService.ACTION_PLAY));
				doPlayPauseButton();
				// ...Handle toggle off
			} else {
				arg0.setSelected(true);
				doPlayPauseButton();
				startService(new Intent(JamService.ACTION_PAUSE));
				// ...Handled toggle on
			}
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
