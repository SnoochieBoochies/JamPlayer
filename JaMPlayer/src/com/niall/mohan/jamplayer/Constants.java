package com.niall.mohan.jamplayer;

import java.net.URI;

import com.dropbox.client2.session.Session.AccessType;

public final class Constants {
	public static final String YOUR_APP_CONSUMER_KEY = "9ba8dd1f82ad58e8470b3e5a69cc828c";
	public static final String YOUR_APP_CONSUMER_SECRET = "9269708fca324437b41fb738fb78a5f7";
	public static URI REDIRECT_URI = URI.create("http://developers.soundcloud.com/callback.html");
	public static final String LAST_FM_API_KEY = "d750eda9db3bbb8873246ca0a1725726";
	public static final String LAST_FM_SECRET = "15af00d4e63c6d6e6b3d194e284743c2";
	public static final String BROADCAST_SEEKBAR = "com.niall.mohan.jamplayer.sendseekbar";
	final static public String GRACENOTE_KEY = "13046016-57E031D9977B0F9F9DECABBC977DE50B";
	final static public String APP_KEY = "7d5b3w41ptwxz3t";
	final static public String APP_SECRET = "1nkra6j4iyhnvnp";
	final static public AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	final static public String ACCOUNT_PREFS_NAME = "prefs";
	final static public String ACCESS_KEY_NAME = "ACCESS_KEY";
	final static public String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	/*The actions we allow to happen through intentFilters*/
	public static final String ACTION_TOGGLE_PLAYBACK ="com.niall.mohan.jamplayer.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.niall.mohan.jamplayer.action.PLAY";
	public static final String ACTION_PAUSE = "com.niall.mohan.jamplayer.action.PAUSE";
	public static final String ACTION_STOP = "com.niall.mohan.jamplayer.action.STOP";
	public static final String ACTION_SEEK = "com.niall.mohan.jamplayer.action.SEEK";
	public static final String ACTION_SKIP = "com.niall.mohan.jamplayer.action.SKIP";
	public static final String ACTION_REWIND = "com.niall.mohan.jamplayerr.action.REWIND";
	public static final String ACTION_URL = "com.niall.mohan.jamplayer.action.URL";
	public static final String ACTION_NONE = "com.niall.mohan.jamplayer.action.NONE";
	public static final String ACTION_NOW_PLAYING = "com.niall.mohan.jamplayer.action.NOW_PLAYING";
	public static final String CHECK_PAUSED = "com.niall.mohan.jamplayer.action.CHECK_PAUSED";
}
