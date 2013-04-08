package com.niall.mohan.jamplayer.adapters;

public class JamSongs  {
	public String title; // media title
	public String path; // media path
	public String album; // media album
	public String playlist; // media path
	public String duration; // media duration
	public String artist; // media artist
	public String display_name;
	public int cloud;
	
	public JamSongs(String title, String path, int cloud, String album,String duration, String artist,
			String display_name) {
		this.title = title;
		this.path = path;
		this.cloud = cloud;
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
	public int getCloud() {
		return cloud;
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
	public void setCloud(int cloud) {
		this.cloud = cloud;
	}
}