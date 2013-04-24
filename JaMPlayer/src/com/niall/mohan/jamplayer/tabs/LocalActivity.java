package com.niall.mohan.jamplayer.tabs;

import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MediaAdapter;
import com.niall.mohan.jamplayer.MusicTable;
import com.niall.mohan.jamplayer.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

public class LocalActivity extends Fragment implements  OnItemClickListener {
	/** (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
            return null;
        }
		View v = inflater.inflate(R.id.local_list_song, container, false);
		return (RelativeLayout)inflater.inflate(R.layout.local_tab_layout, container, false);
	}
	public static String TAG = "LocalFragment";
	private ListView list_song;
	private ExpandableListView exp_list_artists;
	public static BaseAdapter displayAdapter;
	public static ExpandableListAdapter exp_list_adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//displayAdapter = new MediaAdapter(this.getActivity());
		//list_song = (ListView) this.getActivity().findViewById(R.id.local_list_song);
		//list_song.setAdapter(displayAdapter);
		//list_song.setOnItemClickListener(this);
		
		exp_list_adapter = new ExpandableListAdapter(this.getActivity());
		exp_list_artists = (ExpandableListView) this.getActivity().findViewById(R.id.local_list_album);
		//exp_list_artists.setAdapter(exp_list_adapter);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.i(TAG, "item clicked");
		this.getActivity().startService(new Intent(JamService.ACTION_PLAY));
	}
	
	
}