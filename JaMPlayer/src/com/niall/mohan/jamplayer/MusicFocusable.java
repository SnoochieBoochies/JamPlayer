package com.niall.mohan.jamplayer;

/* This class exists for backwards compatibility for the AudioFocus.OnChangeListener. Only available from SDK 8 onwards.
 */
public interface MusicFocusable {
    /* Signals that audio focus was gained. */
    public void onGainedAudioFocus();

    /*Signals that audio focus was lost.*/
    public void onLostAudioFocus(boolean canDuck);
}
