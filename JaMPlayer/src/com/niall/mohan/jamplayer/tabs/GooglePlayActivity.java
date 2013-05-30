package com.niall.mohan.jamplayer.tabs;

import java.util.ArrayList;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
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

import com.niall.mohan.jamplayer.Constants;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.adapters.JamSongs;

/*This is the google Play activity tab class. Does the same as the Dropbox+Local activity classes, only this class
 * retrieves the songs related to the "google" service.*/
public class GooglePlayActivity extends ExpandableListActivity implements OnClickListener {
	private static String TAG = "GooglePlayActivity";
	private ArtistAlbumListAdapter adapter;
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
	ImageButton nowPlayingArtBtn;
	Button nowPlayingTitleBtn;
	View border;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//startService(new Intent(this,JamService.class));
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if(savedInstanceState != null) {
			currentArtist = savedInstanceState.getString("selectedartist");
			currentArtistId = savedInstanceState.getString("selectedartistid");
			currentAlbum = savedInstanceState.getString("currentalbum");
			currentAlbumId = savedInstanceState.getString("selectedalbumid");

		}
		setContentView(R.layout.tab_content_layout);
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(Constants.ACTION_NOW_PLAYING));
		db = new MusicTable(this);
		db.open();
		fillData();
	}
	private BroadcastReceiver nowPlaying = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "Google receive");
			Log.i(TAG, intent.getStringExtra("title"));
			nowPlayingArtBtn = (ImageButton) findViewById(R.id.art_thumb);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			Bitmap bm = intent.getParcelableExtra("art");
			nowPlayingArtBtn.setImageBitmap(Bitmap.createScaledBitmap(bm, 120, 80, false));
			nowPlayingArtBtn.setOnClickListener(GooglePlayActivity.this);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn = (Button) findViewById(R.id.art_text);
			nowPlayingTitleBtn.setText(intent.getStringExtra("title"));
			nowPlayingTitleBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn.setOnClickListener(GooglePlayActivity.this);
			border = (View) findViewById(R.id.border);
			border.setVisibility(View.VISIBLE);
			albumSongs = intent.getParcelableArrayListExtra("albumsongs");
		}
	};
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState()");
		outState.putString("selectedartist", currentArtist);
		outState.putString("selectedartistid", currentArtistId);
		outState.putString("selectedalbum", currentAlbum);
		outState.putString("selectedalbumid", currentAlbumId);
		outState.putString("selectedservice", currentService);
		super.onSaveInstanceState(outState);
	}
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		fillData();
		super.onResume();
	}
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	@Override
	protected void onStop() {
		super.onStop();
		adapter.getCursor().close();
		artistCursor.close();
		db.close();
	}
	/*Fills the expandable list with artists + albums from the DB*/
	private void fillData() {
		Log.i(TAG, "fillData()");
		artistCursor = db.getArtistsByService("google");
		startManagingCursor(artistCursor);
		adapter = new ArtistAlbumListAdapter(artistCursor, this,
				android.R.layout.simple_expandable_list_item_1,android.R.layout.simple_expandable_list_item_1, 
				new String [] {MusicTable.ARTIST}, new int [] {android.R.id.text1}, new String [] {MusicTable.ALBUM}, new int [] {android.R.id.text1});
		setListAdapter(adapter);
		db.close();
	}
	/*This method fires the SongList class on the click of an album.
	 * The SongList class represents the songs in an album*/
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,int groupPosition, int childPosition, long id) {
		Log.i(TAG, "onChildClick()");
		currentAlbumId = Long.valueOf(id).toString();
		Cursor malbumCur = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
		currentAlbum = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.ALBUM));
		currentService = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.SERVICE_TYPE));
		intent = new Intent(this, SongList.class);
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
	/*Class that populates the expandable list.*/
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
	
	@Override
	public void onClick(View v) {
		if(v == nowPlayingArtBtn || v == nowPlayingTitleBtn) {
			Intent intent = new Intent(getApplicationContext(), PlayingActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.putExtra("takeFromPrefs", true);
			intent.putExtra("position", 0);
			intent.putExtra("songTitle", nowPlayingTitleBtn.getText().toString());
			intent.putParcelableArrayListExtra("albumsongs", albumSongs);
			startActivity(intent);
		}
	}
}