package com.niall.mohan.jamplayer;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.PlayingActivity;

import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

public class JamService extends Service implements OnCompletionListener,OnBufferingUpdateListener, OnPreparedListener,OnErrorListener, OnSeekCompleteListener,
MusicFocusable, PrepareMusicRetrieverTask.MusicRetrieverPreparedListener {
	
	/*--------------------------------------*/
	public static ArrayList<JamSongs> albumSongs;
	private int seekPos;
	private int maxPos;
	Intent seekIntent;
	Intent nowPlayingIntent;
	private final Handler handler = new Handler();
	private String lastFmKey;
	private String lastFmUser;
	SharedPreferences prefs;
	Session session; 
	private static final String TAG = "JamService";
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
    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float VOLUME = 0.1f;
    MediaPlayer mPlayer = null;
    AudioFocusHelper mAudioFocusHelper = null;
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };
    State mState = State.Retrieving;
    boolean mStartPlayingAfterRetrieve = false; 
    JamSongs songToPlayAfterRetrieve = null;

    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    String mSongTitle = "";
    boolean mIsStreaming = false;  
    WifiLock mWifiLock;
    final int NOTIFICATION_ID = 1; //go to constants
    MusicRetriever mRetriever;
    Bitmap art;
    ComponentName mMediaButtonReceiverComponent;
    AudioManager mAudioManager;
    NotificationManager mNotificationManager;
    Notification mNotification = null;
    static int currentListPosition = -1;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
    	Log.i(TAG, "createMediaPlayerIfNeeded() ");
        if (mPlayer == null) {
        	Log.i(TAG, "createMediaPlayerIfNeeded() new");
            mPlayer = new MediaPlayer();
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnSeekCompleteListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
        }
        else {
        	Log.i(TAG, "createMediaPlayerIfNeeded() reset");
            mPlayer.reset();
        }
    }
    @Override
    public void onCreate() {   	
        Log.i(TAG, "debug: Creating service");
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus
        mRetriever = new MusicRetriever(getContentResolver(),this);
        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();
        seekIntent = new Intent(ACTION_SEEK);
        prefs = PreferenceManager.getDefaultSharedPreferences(JamService.this);
        lastFmKey = prefs.getString("LastFm_Access", "null");
        lastFmUser = prefs.getString("LastFm_User", "null");
        if(lastFmKey == "null")
        	Toast.makeText(getApplicationContext(), "Not logged in for scrobbling to Last.fm", 3000).show();
        Log.i(TAG, lastFmKey);
        session = Session.createSession(Constants.LAST_FM_API_KEY, Constants.LAST_FM_SECRET, lastFmKey);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	String action = intent.getAction();
    	currentListPosition = intent.getIntExtra("position", -1);
    	albumSongs = intent.getParcelableArrayListExtra("albumsongs");
    	//Log.i(TAG, String.valueOf(albumSongs.size()));
    	art = intent.getParcelableExtra("art");
    	if(intent.equals(null)) Log.i(TAG, "null intent");
		if(action.equals(ACTION_PLAY)) {processPlayRequest(currentListPosition); Log.i(TAG, "play request");}
		else if(action.equals(ACTION_STOP)) processStopRequest();
		else if(action.equals(ACTION_SKIP)) processSkipRequest(currentListPosition);
		else if(action.equals(ACTION_PAUSE)) processPauseRequest();
		else if(action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();

		else if(action.equals(ACTION_NONE)) onCreate();
    	Log.i(TAG, String.valueOf(currentListPosition));
		registerReceiver(seekReceiver, new IntentFilter(Constants.BROADCAST_SEEKBAR));
    	return START_NOT_STICKY;
    }
    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest(currentListPosition);
        } else {
            processPauseRequest();
        }
    }
    private void processPlayRequest(int position) {
    	if (mState == State.Retrieving) {
    		Log.i(TAG,"pos "+ String.valueOf(position));
            Log.i(TAG, albumSongs.get(position).getPath());
            songToPlayAfterRetrieve = albumSongs.get(position);
            mStartPlayingAfterRetrieve = true;  
            return;
        }
        tryToGetAudioFocus();
        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(albumSongs.get(position).getPath(),position);
            Log.i("LELEL", "LELEL");
        } else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        } else if (mState == State.Playing) {
            configAndStartMediaPlayer();
        }
    }
    @Override
    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;
        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(songToPlayAfterRetrieve.getPath(),currentListPosition);
        }
    }
    void processPauseRequest() {
        if (mState == State.Retrieving) {
            // If we are still retrieving media, clear the flag that indicates we should start
            // playing when we're ready
            mStartPlayingAfterRetrieve = false;
            return;
        }
        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            Intent in = new Intent(CHECK_PAUSED);
            in.putExtra("paused", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(in);
            // do not give up audio focus
        }
    }
    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }
    void processSkipRequest(int position) {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            //processPlayRequest(position);
            playNextSong(albumSongs.get(position).getPath(),position);
        }
    }
    
    private BroadcastReceiver seekReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive() service");
			updateSeekPos(intent);
		}
	};

	// Update seek position from Activity
	public void updateSeekPos(Intent intent) {
		int seekPos = intent.getIntExtra("seekpos", 0);
		Log.i(TAG, "recieved seek "+String.valueOf(seekPos));
		if (mPlayer.isPlaying()) {
			Log.i(TAG, "isPlaying...");
			handler.removeCallbacks(sendUpdatesToUI);
			mPlayer.seekTo(seekPos);
			setupHandler();
		}

	}
	private void setupHandler() {
		handler.removeCallbacks(sendUpdatesToUI);
		handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
		//handler.post(sendUpdatesToUI);
	}
	private Runnable sendUpdatesToUI = new Runnable() {
		public void run() {
			LogMediaPosition();
			handler.postDelayed(this, 1000); // 2 seconds
		}
	};

	private void LogMediaPosition() {
		//createMediaPlayerIfNeeded();
		//configAndStartMediaPlayer();
		if(mPlayer.isPlaying()) {
			//createMediaPlayerIfNeeded();
			//configAndStartMediaPlayer();
			seekPos = mPlayer.getCurrentPosition();
			maxPos = mPlayer.getDuration();
			String currentTime = makeTimers(seekPos);
			String endTime = makeTimers(maxPos);
			seekIntent.putExtra("counter", seekPos);
			seekIntent.putExtra("mediamax", maxPos);
			seekIntent.putExtra("currentTime", currentTime);
			seekIntent.putExtra("endTime", endTime);
			sendBroadcast(seekIntent);
		}
	}
	private String makeTimers(int current) {
		String endTime = "";
	    String secondsTime = "";
		// Convert total duration into time
	    int hours = (int)( current / (1000*60*60));
	    int minutes = (int)(current % (1000*60*60)) / (1000*60);
	    int seconds = (int) ((current % (1000*60*60)) % (1000*60) / 1000);
	    // Add hours if there
	    if(hours > 0){
	    	endTime = hours + ":";
	    }
	    // Prepending 0 to seconds if it is one digit
	    if(seconds < 10){
	    	secondsTime= "0" + seconds;
	    }else{
	    	secondsTime= "" + seconds;
	    }
	    endTime = endTime + minutes + ":" + secondsTime;
	    return endTime;    
	}

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
       if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
    	Log.i(TAG, "configAndStartMediaPlayer()");
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(VOLUME, VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) mPlayer.start();
    }

    void tryToGetAudioFocus() {
    	Log.i(TAG, "tryToGetAudioFocus()");
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(String manualUrl, int position) {
    	Log.i(TAG, "playNextSong");
        mState = State.Stopped;
        //Log.i(TAG, manualUrl);
        relaxResources(false); // release everything except MediaPlayer
       
        Intent in = new Intent(ACTION_NOW_PLAYING);
        in.putExtra("title", albumSongs.get(position).getTitle());
        in.putExtra("art", art);
        
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
        try {
            JamSongs playingItem = null;
        	createMediaPlayerIfNeeded();
        	playingItem = albumSongs.get(currentListPosition);
        	mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        	Log.i("playItem: ", playingItem.getPath());
        	mPlayer.setDataSource(getApplicationContext(), Uri.parse(playingItem.getPath()));

            //mSongTitle = playingItem.getTitle();
            mSongTitle = albumSongs.get(currentListPosition).getArtist()+" - "+albumSongs.get(currentListPosition).getTitle();
            if(!lastFmKey.equals(null)) {
	            Caller.getInstance().setCache(null); //API workaround. disables caching.
	            Log.i(TAG, albumSongs.get(currentListPosition).getArtist());
	            ScrobbleResult result = Track.updateNowPlaying(albumSongs.get(currentListPosition).getArtist(), albumSongs.get(currentListPosition).getTitle(), session);
	            int now = (int) (System.currentTimeMillis() / 1000);
	            ScrobbleResult result2 = Track.scrobble(albumSongs.get(currentListPosition).getArtist(), albumSongs.get(currentListPosition).getTitle(), now, session);
	            Log.i(TAG,("ok: " + (result2.isSuccessful() && !result2.isIgnored())));
            }


            mState = State.Preparing;
            setUpAsForeground(mSongTitle + " (loading)");
            // Until the media player is prepared, we *cannot* call start() on it! 	
            mPlayer.prepareAsync();
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /** Called when media player is done playing current song. */
    @Override
    public void onCompletion(MediaPlayer player) {
    	Log.i(TAG,"onCompletion()");
        // The media player finished playing the current song, so we go ahead and start the next.
    	int newPos = currentListPosition+1;
    	Log.i(TAG, String.valueOf(newPos));
    	//Log.i(TAG, albumSongs.get(newPos).getPath());
    	if(newPos < albumSongs.size())
    		playNextSong(albumSongs.get(currentListPosition++).getPath(),newPos);
    	else {
    		player.stop();
    	}
    }

    /** Called when media player is done preparing. */
    @Override
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
    	Log.i(TAG, "onPrepared()");
        mState = State.Playing;
        updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
        setupHandler();
    }

    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), PlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                        Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        mNotification.setLatestEventInfo(getApplicationContext(), "JaMPlayer", text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), PlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                 Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_launcher;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "JaMPlayer",
                text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
            "no duck"), Toast.LENGTH_SHORT).show();
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if(!mPlayer.isPlaying()) {
			mPlayer.start();
		}
	}
	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
		Log.i(TAG,"onBufferingUpdate");
	}
}