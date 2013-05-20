package com.niall.mohan.jamplayer.tabs;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.api.client.http.HttpResponse;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;
import com.niall.mohan.jamplayer.WriteToCache;
import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

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
	long lastSeekTime;
	long defaultPos = -1;
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
		//ArtLoaderTask task = new ArtLoaderTask();
		//task.execute(receive);
		//loader.setVisibility(View.VISIBLE);

		final WriteToCache cache = new WriteToCache();
		prefs = PreferenceManager.getDefaultSharedPreferences(PlayingActivity.this);
		final String tempArtUri = prefs.getString("artwork", "null");
		Log.i(TAG, "URI "+tempArtUri);
		final long tempId = prefs.getLong("id", 0);
		AudioManager am = (AudioManager) PlayingActivity.this.getSystemService(Context.AUDIO_SERVICE);
		if(am.isMusicActive()) {
			songName.setText(prefs.getString("songTitle", ""));
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
		} else {
			//Intent receive = getIntent();
			position = receive.getIntExtra("position", 0);
			albumSongs = receive.getParcelableArrayListExtra("albumsongs");
			songName.setText(receive.getStringExtra("songTitle"));
			action = receive.getStringExtra("action");
			artUri = albumSongs.get(position).getArtwork();
			albId = albumSongs.get(position).getAlbumId();
			Thread artWorkTask = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						//loader.setVisibility(View.VISIBLE);
						art = cache.getArtwork(artUri, albId, getApplicationContext());
						
						albumArt.setImageBitmap(art);
						//loader.setVisibility(View.GONE);
					} catch (IOException e) {
						e.printStackTrace();
					}		
				}
			});
			artWorkTask.run();
}
		//loader.setVisibility(View.GONE);
		db = new MusicTable(this);
		db.open();
		doPlayPauseButton();
		Intent intent = new Intent(JamService.ACTION_PLAY);
		intent.putExtra("position", position);
		intent.putParcelableArrayListExtra("albumsongs", albumSongs);
		intent.putExtra("art", art);
		if(action =="skip")
			startService(new Intent(JamService.ACTION_SKIP));
		else {
			startService(intent);
		}
		
		
	}
	
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
		lastSeekTime = 0;
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		defaultPos = -1;
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
