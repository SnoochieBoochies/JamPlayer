package com.niall.mohan.jamplayer.adapters;

public class JamSongs  {
	public String title; // media title
	public String path; // media path
	public String album; // media album
	public String playlist; // media path
	public String duration; // media duration
	public String artist; // media artist
	public String display_name;
	public String service;
	
	public JamSongs(String title, String path, String service, String album,String duration, String artist,
			String display_name) {
		this.title = title;
		this.path = path;
		this.service = service;
		this.album = album;
		this.artist = artist;
		this.duration = duration;
		this.display_name = display_name;
	}
	public JamSongs() {};

	/*The variables could be easily retrieved if public but it's better design to do it this way*/
	public String getTitle() {
		return title;
	}
	public String getPath() {
		return path;
	}
	public String getArtist() {
		return artist;
	}
	public String getAlbum() {
		return album;
	}
	public String getDuration() {
		return duration;
	}
	public String getService() {
		return service;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public void setService(String service) {
		this.service = service;
	}
}