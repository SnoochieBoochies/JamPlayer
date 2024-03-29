/*This class represents the activity that is contained within the SoundCloud tab in the UI.
* This class is different than the others due to the pulling of metadata information at runtime.
* It also displays a refresh button to refresh the list of songs.
* the fillData() method is the main method that fills the list of songs using an ArrayAdapter, 
* (compared to other classes using custom adapters for artists/albums). It does this through an inner Async task
* class
* the nowPlaying BroadcastReceiver is present in every tab class as it is sent from the JamService
* to place the banner at the bottom of the screen to return to the now-playing activity.
*/
package com.niall.mohan.jamplayer.tabs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ListActivity;
import android.app.ProgressDialog;
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
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;
import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

public class SoundcloudActivity extends ListActivity implements  OnClickListener {
	private static String TAG = "SoundcloudActivity";
	ImageButton mPlayPauseButton;
	TextView subListName; 
	ArrayAdapter<JamSongs> adapter;
	ProgressDialog progress;
	long defaultPos = -1;
	int seekMax;
	long lastSeekTime;
	SharedPreferences prefs;
	ApiWrapper wrapper;
	ImageButton nowPlayingArtBtn;
	Button nowPlayingTitleBtn;
	View border;
	private boolean isPaused;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.song_list);
		subListName = (TextView) findViewById(R.id.album_name); //called this as we reuse the xml file. less bloat
		subListName.setText("Following Tracks");
		prefs = PreferenceManager.getDefaultSharedPreferences(SoundcloudActivity.this);
		String access = prefs.getString("SCloud_Access_Key", "null");
		Token toke = new Token(access, null, Token.SCOPE_NON_EXPIRING);
		Log.i(TAG, toke.access);
		if(!toke.access.equals("null")) {
			LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(Constants.ACTION_NOW_PLAYING));
			LocalBroadcastManager.getInstance(this).registerReceiver(paused, new IntentFilter(Constants.CHECK_PAUSED));
			wrapper = new ApiWrapper(Constants.YOUR_APP_CONSUMER_KEY, Constants.YOUR_APP_CONSUMER_SECRET, 
					Constants.REDIRECT_URI, toke);
		}
		else  {
			Toast.makeText(this, "Please Login to Soundcloud first.", 2000).show();
			return;
		}

		fillData();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}
	@Override
	protected void onPause() {
		super.onPause();
	}

	private void fillData() {
		sCloudSongs= new ArrayList<JamSongs>();
		SoundCloudSongTask sCloudTask = new SoundCloudSongTask(wrapper);
		sCloudTask.execute(sCloudSongs);
		
		adapter = new ArrayAdapter<JamSongs>(getApplicationContext(), R.layout.list_child_item, R.id.child_text, sCloudSongs);
		setListAdapter(adapter);
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "onListItemClick()");
		ArrayAdapter c = ((ArrayAdapter) l.getAdapter());
		Log.i(TAG, String.valueOf(c.getItem(position)));
		Intent play = new Intent(getApplicationContext(), PlayingActivity.class);
		AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		play.putExtra("songTitle", String.valueOf(c.getItem(position)));
		play.putExtra("position", position);
		play.putParcelableArrayListExtra("albumsongs", sCloudSongs);
		if(am.isMusicActive()|| isPaused == true) {
			Log.i(TAG, "doing skip");
			play.putExtra("action", "skip");
			play.putExtra("position", position);
			play.putParcelableArrayListExtra("albumsongs", sCloudSongs);
			startActivity(play);
		} else{
			startActivity(play);
		}
		super.onListItemClick(l, v, position, id);
	}
	private BroadcastReceiver nowPlaying = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceiver scloud");
			Log.i(TAG, intent.getStringExtra("title"));
			nowPlayingArtBtn = (ImageButton) findViewById(R.id.art_thumb);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			Bitmap bm = intent.getParcelableExtra("art");
			nowPlayingArtBtn.setImageBitmap(Bitmap.createScaledBitmap(bm, 120, 80, false));
			nowPlayingArtBtn.setOnClickListener(SoundcloudActivity.this);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn = (Button) findViewById(R.id.art_text);
			nowPlayingTitleBtn.setText(intent.getStringExtra("title"));
			nowPlayingTitleBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn.setOnClickListener(SoundcloudActivity.this);
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
	public void onClick(View v) {
		if(v == nowPlayingArtBtn || v == nowPlayingTitleBtn) {
			Intent intent = new Intent(getApplicationContext(), PlayingActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.putExtra("takeFromPrefs", true);
			intent.putExtra("position", 0);
			intent.putExtra("songTitle", nowPlayingTitleBtn.getText().toString());
			intent.putParcelableArrayListExtra("albumsongs", sCloudSongs);
			startActivity(intent);
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
			LocalBroadcastManager.getInstance(SoundcloudActivity.this).registerReceiver(nowPlaying, new IntentFilter(Constants.ACTION_NOW_PLAYING));
			super.onPostExecute(result);
		}
		/* This particular method handles the retrieval of songs and places them into JamSongs objects, from
	        * responses from the Soundcloud API.
	        */
		@Override
		protected ArrayList<JamSongs> doInBackground(ArrayList<JamSongs>... params) {
			ArrayList<JamSongs> list = params[0];
			try {
				Request resource = Request.to("/me/followings/tracks");
				HttpResponse resp = wrapper.get(resource);
				if(wrapper.get(resource).getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
					String json = reader.readLine();
					JSONTokener tokener = new JSONTokener(json);
					JSONArray finalResult = new JSONArray(tokener);
					for(int i = 0; i < finalResult.length();i++) {
						JSONObject f = finalResult.getJSONObject(i);
						String url = (f.getString("stream_url").contains("https://")) 
								? f.getString("stream_url").replace("https://", "http://") :f.getString("stream_url").replace("http://", "");
						url = url+"?client_id="+Constants.YOUR_APP_CONSUMER_KEY;
						list.add(i, new JamSongs(f.getString("title"),url,"soundcloud",f.getString("label_name"),f.getString("duration"),
								f.getString("label_name"), -1, "", f.getString("artwork_url"),0));
					}
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