package com.niall.mohan.jamplayer.tabs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;
import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;
import com.soundcloud.api.Stream;
import com.soundcloud.api.Token;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SoundcloudActivity extends ListActivity implements  OnClickListener, OnSeekBarChangeListener {
	private static String TAG = "SoundcloudActivity";
	ImageButton mPlayPauseButton;
	TextView subListName; 
	ArrayAdapter<JamSongs> adapter;
	ProgressDialog progress;
	SeekBar seekbar;
	TextView currentTime;
	TextView totalTime;
	Intent serviceIntent;
	Intent in;
	long defaultPos = -1;
	int seekMax;
	long lastSeekTime;
	SharedPreferences prefs;
	ApiWrapper wrapper;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.song_list);
		mPlayPauseButton = (ImageButton) findViewById(R.id.control_play_btn);
		mPlayPauseButton.setOnClickListener(this);
		subListName = (TextView) findViewById(R.id.album_name); //called this as we reuse the xml file. less bloat
		subListName.setText("Following Tracks");
		prefs = PreferenceManager.getDefaultSharedPreferences(SoundcloudActivity.this);
		String access = prefs.getString("SCloud_Access_Key", "null");
		Token toke = new Token(access, null, Token.SCOPE_NON_EXPIRING);
		Log.i(TAG, toke.access);
		if(!toke.access.equals("null"))
			wrapper = new ApiWrapper(Constants.YOUR_APP_CONSUMER_KEY, Constants.YOUR_APP_CONSUMER_SECRET, 
					Constants.REDIRECT_URI, toke);
		else Toast.makeText(this, "Please Login to Soundcloud first.", 2000).show();
		seekbar = (SeekBar) findViewById(R.id.progress);
		seekbar.setOnSeekBarChangeListener(this);
		serviceIntent = new Intent(this, JamService.class);
		registerReceiver(receiver, new IntentFilter(JamService.ACTION_SKIP));
		// --- set up seekbar intent for broadcasting new position to
		// service ---
		in = new Intent(Constants.BROADCAST_SEEKBAR);
		currentTime = (TextView) findViewById(R.id.currenttime);
		totalTime = (TextView) findViewById(R.id.totaltime);
		fillData();
	}
	@Override
	protected void onResume() {
		super.onResume();
		//fillData();
	}
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive()");
			int counter = intent.getIntExtra("counter", 0);
			int mediamax = intent.getIntExtra("mediamax", 0);
			// int seekProgress = Integer.parseInt(counter);
			Log.i(TAG, String.valueOf(counter) + ":" + String.valueOf(mediamax));
			seekMax = mediamax;
			currentTime.setText(intent.getStringExtra("currentTime"));
			totalTime.setText(intent.getStringExtra("endTime"));
			seekbar.setMax(seekMax);
			seekbar.setProgress(counter);
		}
	};
	private void fillData() {
		sCloudSongs= new ArrayList<JamSongs>();
		SoundCloudSongTask sCloudTask = new SoundCloudSongTask(wrapper);
		sCloudTask.execute(sCloudSongs);
		
		adapter = new ArrayAdapter<JamSongs>(this, R.layout.list_child_item, sCloudSongs);
		setListAdapter(adapter);
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick()");
		Log.i(TAG, String.valueOf(position));
		//Intent stopIntent = new Intent(JamService.ACTION_STOP);
		//startService(stopIntent);
		//AudioManager am = (AudioManager) SoundcloudActivity.this.getSystemService(Context.AUDIO_SERVICE);
		//if(am.isMusicActive())
		//	startService(new Intent(JamService.ACTION_PAUSE));
		Intent intent = new Intent(JamService.ACTION_PLAY);
		intent.putExtra("position", position);
		intent.putParcelableArrayListExtra("albumsongs", sCloudSongs);
		startService(intent);
		doPlayPauseButton();
		super.onListItemClick(l, v, position, id);
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
		}
	}
	private void doPlayPauseButton() {
		mPlayPauseButton.setImageDrawable(getBaseContext().getResources()
				.getDrawable(R.drawable.playback_toggle));
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
	public static ArrayList<JamSongs> sCloudSongs = new ArrayList<JamSongs>();
	public class SoundCloudSongTask extends AsyncTask<ArrayList<JamSongs>, Void, ArrayList<JamSongs>> {
		SoundcloudActivity activity;
		ApiWrapper wrapper;
		
		public SoundCloudSongTask(ApiWrapper wrapper) {
			this.wrapper = wrapper;
		}
		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.show(getParent(), "", "Loading Songs...",
					true);
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(ArrayList<JamSongs> result) {
			sCloudSongs = result;
			progress.dismiss();
			Log.i(TAG, "onPostExecute");
			Log.i(TAG,String.valueOf(sCloudSongs.size()));
			adapter = new ArrayAdapter<JamSongs>(SoundcloudActivity.this, R.layout.list_child_item, sCloudSongs);
			setListAdapter(adapter);
			super.onPostExecute(result);
		}
		@Override
		protected ArrayList<JamSongs> doInBackground(ArrayList<JamSongs>... params) {
			boolean success = false;
			ArrayList<JamSongs> list = params[0];
			try {
				//Request resource = Request.to(Endpoints.MY_FOLLOWINGS);.with("client_id","9ba8dd1f82ad58e8470b3e5a69cc828c");
				Request resource = Request.to("/me/followings/tracks");
				//Stream stream = wrapper.resolveStreamUrl(resource.toUrl(), true);
				//Log.i(TAG, resource.toUrl());
				//Log.i(TAG, stream.streamUrl);
				HttpResponse resp = wrapper.get(resource);
				if(wrapper.get(resource).getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					//Log.i(TAG, "LEL");
					//Stream [] stream;// = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
					//stream[0] = Http.getString(resp);
					//Stream stream = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
					//Log.i(TAG,stream.toString());
					/*This needs to be packaged up and sent to the playing activity i guess*/
					BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
					String json = reader.readLine();
					JSONTokener tokener = new JSONTokener(json);
					JSONArray finalResult = new JSONArray(tokener);
					//Log.i(TAG,finalResult.getString(0));
					for(int i = 0; i < finalResult.length();i++) {
						JSONObject f = finalResult.getJSONObject(i);
						String temp;
						String url = (f.getString("stream_url").contains("https://")) 
								? f.getString("stream_url").replace("https://", "http://") :f.getString("stream_url").replace("http://", "");
						//Log.i(TAG, url);
						url = url+"?client_id="+Constants.YOUR_APP_CONSUMER_KEY;
						list.add(i, new JamSongs(f.getString("title"),url,"soundcloud",f.getString("label_name"),f.getString("duration"),
								f.getString("label_name"), -1, "", f.getString("artwork_url")));
						//Log.i(TAG, list.get(i).getTitle());
						//Log.i(TAG,d[i]);
					}
					success = true;
				}
			} catch (IOException io) {
				io.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return list;
		}
		
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				this.startActivity(intent);
				return true;
			case R.id.refresh:
				fillData();
		}
		return false;
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sub_menu_two, menu);
		return true;
	}
}