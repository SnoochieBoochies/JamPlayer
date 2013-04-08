package com.niall.mohan.jamplayer.adapters;

import android.util.Log;

import com.android.gm.api.model.Song;

public class GPlaySongAdapter extends JamSongs {
	Song song;
	public GPlaySongAdapter(Song song) {
		this.song = song;
	}
	public JamSongs setMediaInfo(Song s) {
		JamSongs mediaInfo = new JamSongs();//new JamSongs(s.getTitle(),s.getUrl(),1,s.getAlbum(),String.valueOf(s.getDurationMillis()),s.getArtist(),s.getTitle());
		mediaInfo.title = s.getTitle();
		mediaInfo.path = s.getUrl();
		mediaInfo.cloud = 1;
		mediaInfo.album = s.getAlbum();
		mediaInfo.duration = String.valueOf(s.getDurationMillis());
		mediaInfo.artist = s.getArtist();
		mediaInfo.display_name = s.getTitle();
		return mediaInfo;
	}
}
