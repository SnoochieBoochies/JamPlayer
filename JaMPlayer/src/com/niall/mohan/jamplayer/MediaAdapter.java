package com.niall.mohan.jamplayer;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MediaAdapter extends BaseAdapter {

	ArrayList<String> songs;
	Context context;
	LayoutInflater inflater;
	public MediaAdapter(Context context) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.context = context;
		songs = new ArrayList<String>();
	}

	public MediaAdapter(Context context, ArrayList<String> songs) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.context = context;
		this.songs = songs;
	}

	@Override
	public int getCount() {

		if (songs != null) {
			return songs.size();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {

		return songs.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.song, parent, false);
		}

		final String data = songs.get(position);
		final TextView tv_song = (TextView) convertView;
		tv_song.setText(data);
		return convertView;
	}

}