package com.niall.mohan.jamplayer.adapters;

import android.os.Parcel;
import android.os.Parcelable;

/*The representation of a song in this app.
 * It implements parcelable so I can send lists of 
 * JamSongs over Intents to other activities, ie: a list of songs in an album
 * from GooglePlayActivity to SongList activity.
*/
public class JamSongs  implements Parcelable{
	private String title; // media title
	private String path; // media path
	private String album; // media album
	private String playlist; // media path
	private String duration; // media duration
	private String artist; // media artist
	private String display_name;
	private String service;
	private int trackNum;
	private String id;
	private String artworkUri;
	private long albumId; 
	public JamSongs(String title, String path, String service, String album,String duration, String artist, int trackNum, String id, String artworkUri, long albumId) {
		this.title = title;
		this.path = path;
		this.service = service;
		this.album = album;
		this.artist = artist;
		this.duration = duration;
		this.trackNum = trackNum;
		this.id = id;
		this.artworkUri = artworkUri;
		this.albumId = albumId;
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
	public int getTrackNum() {
		return trackNum;
	}
	public String getId() {
		return id;
	}
	public String getArtwork() {
		return artworkUri;
	}
	public long getAlbumId() {
		return albumId;
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
	public void setTrackNum(int trackNum) {
		this.trackNum = trackNum;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setArtwork(String artworkUri) {
		this.artworkUri = artworkUri;
	}
	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}
	@Override
	public String toString() {
		if(service.equals("google") || service.equals("dropbox") || service.equals("local"))
			return this.title;
		else
			return this.title+" - "+this.album;
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
		dest.writeString(id);
		dest.writeString(artworkUri);
		dest.writeLong(albumId);
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
		id = source.readString();
		artworkUri = source.readString();
		albumId = source.readLong();
	}
}