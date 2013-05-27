package com.niall.mohan.jamplayer.tabs;

import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ProgressBar;
import android.widget.SimpleCursorTreeAdapter;

import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicRetriever;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.PrepareMusicRetrieverTask;
import com.niall.mohan.jamplayer.R;

public class LocalActivity extends ExpandableListActivity implements OnClickListener {
	private static String TAG = "LocalActivity";
	private ArtistAlbumListAdapter adapter;
	private String currentArtist;
	private String currentArtistId;
	private String currentAlbum;
	private String currentAlbumId;
	private String currentService;
	private Cursor artistCursor;
	public MusicTable db;
	MusicRetriever mRetriever;
	ImageButton nowPlayingArtBtn;
	Button nowPlayingTitleBtn;
	View border;
	ProgressBar loader;
	//setup. Get last selected artist/album combo.
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
			currentService = savedInstanceState.getString("selectedservice");
		}
		setContentView(R.layout.tab_content_layout);
		loader = (ProgressBar) findViewById(R.id.local_loader);
        /*mRetriever = new MusicRetriever(getContentResolver(),this);
        (new PrepareMusicRetrieverTask(mRetriever) {
            @Override
            protected void onPreExecute() {
               	super.onPreExecute();
               	loader.setVisibility(View.VISIBLE);
            }
        }).execute();
        */
        loader.setVisibility(View.GONE);
		LocalBroadcastManager.getInstance(this).registerReceiver(nowPlaying, new IntentFilter(JamService.ACTION_NOW_PLAYING));
		db = new MusicTable(this);
		db.open();
		fillData();
	}
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
	protected void onResume() {
		fillData();
		super.onResume();
	}
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	private void fillData() {
		Log.i(TAG, "fillData()");
		artistCursor = db.getArtistsByService("local");
		startManagingCursor(artistCursor);
		adapter = new ArtistAlbumListAdapter(artistCursor, this,
				android.R.layout.simple_expandable_list_item_1,android.R.layout.simple_expandable_list_item_1, 
				new String [] {MusicTable.ARTIST}, new int [] {android.R.id.text1}, new String [] {MusicTable.ALBUM}, new int [] {android.R.id.text1});
		setListAdapter(adapter);
		db.close();
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		Log.i(TAG, "onChildClick()");
		currentAlbumId = Long.valueOf(id).toString();
		Cursor malbumCur = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
		//String album = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.ALBUM));
		currentAlbum = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.ALBUM));
		currentService = malbumCur.getString(malbumCur.getColumnIndex(MusicTable.SERVICE_TYPE));
		Intent intent = new Intent(this, SongList.class);
		intent.putExtra("albumId", currentAlbumId);
		//unknown album
		if(currentAlbum == null || currentAlbum.equals(MediaStore.UNKNOWN_STRING)) {
			artistCursor.moveToPosition(groupPosition);
			currentArtistId = artistCursor.getString(artistCursor.getColumnIndex(MusicTable.ARTIST));
			intent.putExtra("artist", currentArtistId);
		}
		intent.putExtra("service", currentService);
		intent.putExtra("album", currentAlbum);
		currentArtist = artistCursor.getString(1);
		intent.putExtra("artist", currentArtist);
		startActivity(intent);
		return true;
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
	private BroadcastReceiver nowPlaying = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Log.i(TAG, "onReceiver google");
			Log.i(TAG, intent.getStringExtra("title"));
			nowPlayingArtBtn = (ImageButton) findViewById(R.id.art_thumb);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			Bitmap bm = intent.getParcelableExtra("art");
			nowPlayingArtBtn.setImageBitmap(Bitmap.createScaledBitmap(bm, 120, 80, false));
			nowPlayingArtBtn.setOnClickListener(LocalActivity.this);
			nowPlayingArtBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn = (Button) findViewById(R.id.art_text);
			nowPlayingTitleBtn.setText(intent.getStringExtra("title"));
			nowPlayingTitleBtn.setVisibility(View.VISIBLE);
			nowPlayingTitleBtn.setOnClickListener(LocalActivity.this);
			border = (View) findViewById(R.id.border);
			border.setVisibility(View.VISIBLE);
		}
	};
	@Override
	public void onClick(View v) {
		if(v == nowPlayingArtBtn || v == nowPlayingTitleBtn) {
			Intent intent = new Intent(getApplicationContext(), PlayingActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		}
	}

	
}