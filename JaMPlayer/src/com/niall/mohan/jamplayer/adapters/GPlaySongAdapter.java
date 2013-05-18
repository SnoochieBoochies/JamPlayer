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
		mediaInfo.setTitle(s.getTitle());
		mediaInfo.setPath(s.getUrl());
		mediaInfo.setService("google");
		mediaInfo.setAlbum(s.getAlbum());
		mediaInfo.setArtist(s.getArtist());
		mediaInfo.setDuration(String.valueOf(s.getDurationMillis()));
		mediaInfo.setTrackNum(s.getTrack());
		mediaInfo.setId(s.getId());
		if(s.getAlbumArtUrl() == null)
			mediaInfo.setArtwork("");
		else {
			mediaInfo.setArtwork("http:"+s.getAlbumArtUrl());
			Log.i("ART", mediaInfo.getArtwork());
		}
		return mediaInfo;
	}
}
