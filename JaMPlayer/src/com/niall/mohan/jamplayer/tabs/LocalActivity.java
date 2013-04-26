package com.niall.mohan.jamplayer.tabs;

import com.google.android.gms.internal.ar;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Toast;

public class LocalActivity extends ExpandableListActivity implements  OnItemClickListener {
	public ExpandableListView list_view;
	private static String TAG = "LocalActivity";
	private ArtistAlbumListAdapter adapter;
	private String currentArtist;
	private String currentArtistId;
	private String currentAlbum;
	private String currentAlbumId;
	private String currentSong;
	private Cursor artistCursor;
	private static int lastListPos = -1;
	private static int lastListPosFine = -1;
	public MusicTable db;
	//setup. Get last selected artist/album combo.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		startService(new Intent(this,JamService.class));
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		if(savedInstanceState != null) {
			currentArtist = savedInstanceState.getString("selectedartist");
			currentArtistId = savedInstanceState.getString("selectedartistid");
			currentAlbum = savedInstanceState.getString("currentalbum");
			currentAlbumId = savedInstanceState.getString("selectedalbumid");
			currentSong = savedInstanceState.getString("currentsong");

		}
		setContentView(R.layout.local_tab_layout);
		//list_view = (ExpandableListView) findViewById(R.id.local_list_artist);

		db = new MusicTable(this);
		db.open();
		fillData();
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("selectedartist", currentArtist);
		outState.putString("selectedartistid", currentArtistId);
		outState.putString("selectedalbum", currentAlbum);
		outState.putString("selectedalbumid", currentAlbumId);
		outState.putString("selectedsong", currentSong);
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
	

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.i(TAG, "onItemClick()");
	}
	int mGroupIdColumnIndex;
	void fillData() {
		Log.i(TAG, "fillData()");
		artistCursor = db.getArtistsByService("local");
		startManagingCursor(artistCursor);
		//Log.i(TAG, artistCursor.getColumnName(2));
		//artistCursor.moveToFirst();
		//mGroupIdColumnIndex = artistCursor.getColumnIndexOrThrow(MusicTable.ARTIST);
		//artistCursor = db.query("artist", 2, "local");
		adapter = new ArtistAlbumListAdapter(artistCursor, this,
				android.R.layout.simple_expandable_list_item_1,android.R.layout.simple_expandable_list_item_1, 
				new String [] {MusicTable.ARTIST}, new int [] {android.R.id.text1}, new String [] {MusicTable.ALBUM}, new int [] {android.R.id.text1});
		setListAdapter(adapter);
	}
	public class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter {

		public ArtistAlbumListAdapter(Cursor cursor, Context context,
                int groupLayout, int childLayout, String[] groupFrom,
                int[] groupTo, String[] childrenFrom, int[] childrenTo) {
			super(context, cursor, groupLayout, groupFrom, groupTo,
                        childLayout, childrenFrom, childrenTo);
		}



		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			Cursor albumCursor = db.getArtistsAlbumsByService(groupCursor.getString(groupCursor.getColumnIndex("artist")));// = db.getArtistsByService("local");
			LocalActivity.this.startManagingCursor(albumCursor);
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
	
	
}