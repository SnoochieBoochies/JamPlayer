package com.niall.mohan.jamplayer.adapters;

import android.os.Parcel;
import android.os.Parcelable;

public class JamSongs  implements Parcelable{
	public String title; // media title
	public String path; // media path
	public String album; // media album
	public String playlist; // media path
	public String duration; // media duration
	public String artist; // media artist
	public String display_name;
	public String service;
	public int trackNum;
	
	public JamSongs(String title, String path, String service, String album,String duration, String artist, int trackNum) {
		this.title = title;
		this.path = path;
		this.service = service;
		this.album = album;
		this.artist = artist;
		this.duration = duration;
		this.trackNum = trackNum;
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
	/*implements parceable to be able to send an arraylist of jamsongs to the service, so we can play an album etc.*/
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(path);
		dest.writeString(service);
		dest.writeString(album);
		dest.writeString(duration);
		dest.writeString(artist);
		dest.writeInt(trackNum);
	}
	public static final Parcelable.Creator<JamSongs> CREATOR = new Creator<JamSongs>() {
		@Override
		public JamSongs[] newArray(int size) {
			return new JamSongs[size];
		}
		
		@Override
		public JamSongs createFromParcel(Parcel source) {
			return new JamSongs(source);
		}
	};
	private JamSongs(Parcel source) {
		title = source.readString();
		path = source.readString();
		service = source.readString();
		album = source.readString();
		duration = source.readString();
		artist = source.readString();
		trackNum = source.readInt();
	}
}