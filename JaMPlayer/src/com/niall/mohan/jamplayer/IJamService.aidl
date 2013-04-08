package com.niall.mohan.jamplayer;

interface IJamService {
    boolean isPlaying();
    void play(int position);
    void pause();
    void prev();
    void next();
    long duration();
    long position();
    long seek(long pos);
    String getAlbumName();
    long getAlbumId();
    String getArtistName();
    long getArtistId();
    String getPath();
    void setShuffleMode();
    int getShuffleMode();
    void setRepeatMode();
    int getRepeatMode();
    
}