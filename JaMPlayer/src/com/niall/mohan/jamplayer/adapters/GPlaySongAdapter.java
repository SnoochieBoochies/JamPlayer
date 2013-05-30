package com.niall.mohan.jamplayer.adapters;

import android.util.Log;

import com.android.gm.api.model.Song;

/*Adapter class of JamSongs for GMusic API "Songs" object*/
public class GPlaySongAdapter extends JamSongs {
	Song song;
	public GPlaySongAdapter(Song song) {
		this.song = song;
	}
	public JamSongs setMediaInfo(Song s) {
		JamSongs mediaInfo = new JamSongs();
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
		mediaInfo.setAlbumId(0);
		return mediaInfo;
	}
}
