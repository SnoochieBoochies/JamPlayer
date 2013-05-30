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
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
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

/*This is the class that is created after selecting a song to play.
 *It holds the album art, player buttons and title of the currently playing song.
 */
public class PlayingActivity extends Activity implements OnClickListener, OnSeekBarChangeListener{
	private static String TAG = "PlayingActivity";
	ImageButton mPlayPauseButton;
	TextView songName;
	TextView currentTime;
	TextView totalTime;
	SeekBar seekbar;
	Handler handler;
	ImageView albumArt;
	ProgressBar loader;
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
	private boolean takeFromPrefs;
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
		registerReceiver(seekReceiver, new IntentFilter(Constants.ACTION_SEEK));
		in = new Intent(Constants.BROADCAST_SEEKBAR);
		currentTime = (TextView) findViewById(R.id.currenttime);
		totalTime = (TextView) findViewById(R.id.totaltime);
		loader = (ProgressBar) findViewById(R.id.playing_loader);
		loader.setVisibility(View.VISIBLE);
		final Intent receive = getIntent();
		/*-----------------------handling different states of entering the activity------------------------*/
		albumSongs = new ArrayList<JamSongs>();
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(Constants.ACTION_NOW_PLAYING));
		LocalBroadcastManager.getInstance(this).registerReceiver(paused, new IntentFilter(Constants.CHECK_PAUSED));
		LocalBroadcastManager.getInstance(this).registerReceiver(stopReceiver, new IntentFilter(Constants.ACTION_STOP));
		prefs = PreferenceManager.getDefaultSharedPreferences(PlayingActivity.this);
		String artUriForPrefs = prefs.getString("artwork", "null");
		Log.i(TAG, "URI "+artUriForPrefs);
		AudioManager am = (AudioManager) PlayingActivity.this.getSystemService(Context.AUDIO_SERVICE);		
		handleBroadcasts(receive,am);
		/*---------------------------------------------------------------*/
		db = new MusicTable(this);
		db.open();
		doPlayPauseButton();
		Intent intent = new Intent(Constants.ACTION_PLAY);
		intent.putExtra("position", position);
		intent.putParcelableArrayListExtra("albumsongs", albumSongs);
		intent.putExtra("art", art);
		startService(intent);
	}
	/*this method handles whether the user entered the activity by
	 * playing a song initially, skipping to a song, and doing these if music is paused
	 * */
	private void handleBroadcasts(Intent receive, AudioManager am) {
		LoaderTask load = new LoaderTask();
		action = receive.getStringExtra("action");
		position = receive.getIntExtra("position", 0);
		//albumSongs = receive.getParcelableArrayListExtra("albumsongs");
		boolean tempPaused = prefs.getBoolean("paused", false);
		Log.i(TAG,"stored pause"+String.valueOf(tempPaused));
		takeFromPrefs = receive.getBooleanExtra("takeFromPrefs", false); //sent from the banner press to say take from prefs instead of intent.
		if(am.isMusicActive() || isPaused || tempPaused == true) {
			load.execute(receive);
		} else {//done the first time the activity is created
			loader.setVisibility(View.VISIBLE);
			Log.i(TAG, "else regular");
			position = receive.getIntExtra("position", 0);
			albumSongs = receive.getParcelableArrayListExtra("albumsongs");
			songName.setText(receive.getStringExtra("songTitle"));
			action = receive.getStringExtra("action");
			artUri = albumSongs.get(position).getArtwork();
			albId = albumSongs.get(position).getAlbumId();
			Editor writer = prefs.edit();
			writer.putString("artwork", artUri);
			writer.putLong("id", albId);
			writer.putString("skip", action);
			writer.putInt("position", position);
			writer.commit();
			try {
				art = cache.getArtwork(artUri, albId, getApplicationContext());
			} catch (IOException e) {
				e.printStackTrace();
			}
			albumArt.setVisibility(View.VISIBLE);
			albumArt.setImageBitmap(art);
			loader.setVisibility(View.GONE);
		}
	}
	/*----------Receivers----------------------------------*/
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
			Log.i(TAG, "Pause"+String.valueOf(isPaused));
		}
	};
	private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "stopReceiver");
			mPlayPauseButton.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.ic_appwidget_music_play));		
		}
	};
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
	/*------------------------------------------------------*/
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
		Editor writer = prefs.edit();
		writer.putString("artwork", artUri);
		writer.putLong("id", albId);
		writer.putString("skip", action);
		writer.putInt("position", position);
		writer.putBoolean("paused", isPaused);
		writer.commit();
		unregisterReceiver(seekReceiver);
	}
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(seekReceiver, new IntentFilter(Constants.ACTION_SEEK));
	}

	/*Toggles the play/pause button*/
	private void doPlayPauseButton() {
		mPlayPauseButton.setImageDrawable(getBaseContext().getResources()
				.getDrawable(R.drawable.playback_toggle));
	}
	
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
				startService(new Intent(Constants.ACTION_PLAY));
				doPlayPauseButton();
				// ...Handle toggle off
			} else {
				arg0.setSelected(true);
				doPlayPauseButton();
				startService(new Intent(Constants.ACTION_PAUSE));
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
	/*This inner ASync task gets called in handleBroadcasts if the user is returning to the
	 * activity from somewhere else. ie: skipping, or pressing the banner.*/
	private class LoaderTask extends AsyncTask<Intent, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(Void result) {
			loader.setVisibility(View.GONE);
			albumArt.setImageBitmap(art);
			albumArt.setVisibility(View.VISIBLE);
			super.onPostExecute(result);
		}
		@Override
		protected Void doInBackground(Intent... params) {
			Intent receive = params[0];
			try {
				action = receive.getStringExtra("action");
				albumSongs = receive.getParcelableArrayListExtra("albumsongs");
				position = receive.getIntExtra("position", 0);
				System.out.println(takeFromPrefs);
				if(!takeFromPrefs) {
					Log.i(TAG, "albumSongs not empty");
					songName.setText(receive.getStringExtra("songTitle"));
					//Log.i(TAG, receive.getStringExtra("songTitle"));
					artUri = albumSongs.get(position).getArtwork();
					//Log.i(TAG, artUri);
					albId = albumSongs.get(position).getAlbumId();
					art = cache.getArtwork(artUri, albId, getApplicationContext());
				}  else {//else i'm going back into the activity from pressing one of the buttons.
					Log.i(TAG, "else after click");
					songName.setText(receive.getStringExtra("songTitle"));
					Log.i(TAG, receive.getStringExtra("songTitle"));
					albumSongs = receive.getParcelableArrayListExtra("albumsongs");
					position = receive.getIntExtra("position", 0);
					artUri = albumSongs.get(position).getArtwork();
					Log.i(TAG, artUri);
					albId = albumSongs.get(position).getAlbumId();
					art = cache.getArtwork(artUri, albId, getApplicationContext());
					action = "";
				}
				if(action.equals("skip")) {
					Intent skip = new Intent(Constants.ACTION_SKIP);
					skip.putExtra("position", position);
					skip.putParcelableArrayListExtra("albumsongs", albumSongs);
					skip.putExtra("art", art);
					skip.putExtra("skip", action);
					Log.i(TAG, "do skip");
					startService(skip);
				}
				Editor writer = prefs.edit();
				writer.remove("paused");
				writer.commit();
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return null;
		}
		
	}
}
