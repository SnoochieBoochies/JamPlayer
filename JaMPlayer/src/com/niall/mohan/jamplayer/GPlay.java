package com.niall.mohan.jamplayer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

import android.util.Log;

import com.android.gm.api.GoogleMusicApi;


public class GPlay {
	String password = "powerg12";
	String username = "dcujamplayer@gmail.com";
	GoogleMusicApi api;
	/*
	public GPlay() {
	}
	public void auth() {
		api = new GoogleSkyJamAPI();
		try
		{
			api.login(username, password);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void getSongs() {
		auth();
		try {
			Collection<Track> tracks = api.getAllTracks();
			for(Track t : tracks) {
				TrackToMediaInfoAdapter adapter;
				adapter = new TrackToMediaInfoAdapter(t);
				adapter.setMediaInfo(t);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class TrackToMediaInfoAdapter extends MediaInfo {
		Track tune;
		public TrackToMediaInfoAdapter(Track tune) {
			this.tune = tune;
		}
		public MediaInfo setMediaInfo(Track t) {
			MediaInfo mediaInfo = new MediaInfo();
			try {
				mediaInfo._title = tune.getTitle();
				mediaInfo._path = api.getTrackURL(tune).toString();
				mediaInfo._cloud = 1;
				mediaInfo._album = tune.getAlbum();
				mediaInfo._duration = String.valueOf(tune.getDurationMillis());
				mediaInfo._artist = tune.getArtist();
				mediaInfo._display_name = tune.getTitle();
				Log.i("GPLAY MEDIAINFO",mediaInfo._title);
				return mediaInfo;
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	*/
}
