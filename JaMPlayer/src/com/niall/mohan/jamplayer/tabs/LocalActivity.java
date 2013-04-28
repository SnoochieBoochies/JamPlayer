package com.niall.mohan.jamplayer.tabs;

import com.google.android.gms.internal.ar;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.Toast;

public class LocalActivity extends ExpandableListActivity {
	private static String TAG = "LocalActivity";
	private ArtistAlbumListAdapter adapter;
	private String currentArtist;
	private String currentArtistId;
	private String currentAlbum;
	private String currentAlbumId;
	private Cursor artistCursor;
	public MusicTable db;
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

		}
		setContentView(R.layout.local_tab_layout);
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
		Intent intent = new Intent(this, SongList.class);
		intent.putExtra("albumId", currentAlbumId);
		//unknown album
		if(currentAlbum == null || currentAlbum.equals(MediaStore.UNKNOWN_STRING)) {
			artistCursor.moveToPosition(groupPosition);
			currentArtistId = artistCursor.getString(artistCursor.getColumnIndex(MusicTable.ARTIST));
			intent.putExtra("artist", currentArtistId);
		}
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
			Cursor albumCursor = db.getArtistsAlbumsByService(groupCursor.getString(groupCursor.getColumnIndex("artist")));// = db.getArtistsByService("local");
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
	
}