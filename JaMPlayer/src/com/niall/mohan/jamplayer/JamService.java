package com.niall.mohan.jamplayer;

import java.io.IOException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.SongList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class JamService extends Service implements OnCompletionListener, OnPreparedListener,OnBufferingUpdateListener,OnErrorListener, MusicFocusable,
PrepareMusicRetrieverTask.MusicRetrieverPreparedListener{
	
	/*--------------------------------------*/
	public static ArrayList<JamSongs> albumSongs;

	private static final String TAG = "JamService";
	public static final String ACTION_TOGGLE_PLAYBACK ="com.niall.mohan.jamplayer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.niall.mohan.jamplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.niall.mohan.jamplayer.action.PAUSE";
    public static final String ACTION_STOP = "com.niall.mohan.jamplayer.action.STOP";
    public static final String ACTION_SKIP = "com.niall.mohan.jamplayer.action.SKIP";
    public static final String ACTION_REWIND = "com.niall.mohan.jamplayerr.action.REWIND";
    public static final String ACTION_URL = "com.niall.mohan.jamplayer.action.URL";
    public static final String ACTION_NONE = "com.niall.mohan.jamplayer.action.NONE";
    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;
    MediaPlayer mPlayer = null;
    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    AudioFocusHelper mAudioFocusHelper = null;
 // indicates the state our service:
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
 // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false; 
 // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    String mWhatToPlayAfterRetrieve = null;
    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };
    // why did we pause? (only relevant if mState == State.Paused)
    PauseReason mPauseReason = PauseReason.UserRequest;
    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    // title of the song we are currently playing
    String mSongTitle = "";
    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;  
 // Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
    WifiLock mWifiLock;
    // The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;
    // Our instance of our MusicRetriever, which handles scanning for media and
    // providing titles and URIs as we need.
    MusicRetriever mRetriever;
    RemoteControlClientCompat mRemoteControlClientCompat;
 // Dummy album art we will pass to the remote control (if the APIs are available).
    Bitmap mDummyAlbumArt;
    ComponentName mMediaButtonReceiverComponent;
    AudioManager mAudioManager;
    NotificationManager mNotificationManager;
    Notification mNotification = null;
    int currentListPosition = -1;
    public boolean isPlaying = false; //boolean to send over to activity to change buttons
    long duration;

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }
        else
            mPlayer.reset();
    }
    @Override
    public void onCreate() {   	
        Log.i(TAG, "debug: Creating service");
        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // Create the retriever and start an asynchronous task that will prepare it.
        mRetriever = new MusicRetriever(getContentResolver(),this);
        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();
        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus
        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);
        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
        
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	String action = intent.getAction();
    	currentListPosition = intent.getIntExtra("position", -1);
    	albumSongs = intent.getParcelableArrayListExtra("albumsongs");
		if(action.equals(ACTION_PLAY)) processPlayRequest(currentListPosition);
		else if(action.equals(ACTION_STOP)) processStopRequest();
		else if(action.equals(ACTION_PAUSE)) processPauseRequest();
		else if(action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
		else if(action.equals(ACTION_NONE)) return START_NOT_STICKY;
    	Log.i(TAG, String.valueOf(currentListPosition));

    	//processPlayRequest(currentListPosition);
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
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready

    		Log.i(TAG,"pos "+ String.valueOf(position));
            Log.i(TAG, albumSongs.get(position).getPath());
            mWhatToPlayAfterRetrieve = albumSongs.get(position).getPath();
            mStartPlayingAfterRetrieve = true;
            return;
        }
        tryToGetAudioFocus();
        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(albumSongs.get(position).getPath());
            Log.i("LELEL", "LELEL");
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
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
            // do not give up audio focus
        }
    }
    private void pause() {
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
            // do not give up audio focus
        }
    }
    void processStopRequest() {
        processStopRequest(true);
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
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) mPlayer.start();
    }

    void tryToGetAudioFocus() {
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
    void playNextSong(String manualUrl) {
        mState = State.Stopped;
        Log.i(TAG, manualUrl);
        relaxResources(false); // release everything except MediaPlayer

        try {
            JamSongs playingItem = null;
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(manualUrl);
                mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");

                //playingItem = new MediaInfo(_title, manualUrl, _album, _duration, _artist, _display_name);
            }
            else {
                mIsStreaming = false; // playing a locally available song

                playingItem = albumSongs.get(currentListPosition);
                if (playingItem == null) {
                    Toast.makeText(this,
                            "No available music to play. Place some music on your external storage "
                            + "device (e.g. your SD card) and try again.",
                            Toast.LENGTH_LONG).show();
                    
                    return;
                }

                // set the source of the media player a a content URI
                createMediaPlayerIfNeeded();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                Log.i("playItem: ", playingItem.getPath());
                mPlayer.setDataSource(getApplicationContext(), Uri.parse(playingItem.getPath()));
            }

            //mSongTitle = playingItem.getTitle();
            mSongTitle = albumSongs.get(currentListPosition).getArtist()+" - "+albumSongs.get(currentListPosition).getTitle();
            

            mState = State.Preparing;
            setUpAsForeground(mSongTitle + " (loading)");

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
    	int newPos = currentListPosition+1;
    	if(newPos < albumSongs.size())
    		playNextSong(albumSongs.get(newPos).getPath());
    	else {
    		player.stop();
    	}
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
    }
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
			
	}
	public boolean hasConnectivity() {
	    ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo info = connectivityManager.getActiveNetworkInfo();

	    int netType = info.getType();
	    int netSubtype = info.getSubtype();

	    if (netType == ConnectivityManager.TYPE_WIFI)
	    {
	        return info.isConnected();
	    }
	    else if (netType == ConnectivityManager.TYPE_MOBILE && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS)
	    {
	        return info.isConnected();
	    }

	    return false;
	}
    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), SongList.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
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
                new Intent(getApplicationContext(), SongList.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                 Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_headset;
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

    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;

        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(mWhatToPlayAfterRetrieve.toString());
        }
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

}