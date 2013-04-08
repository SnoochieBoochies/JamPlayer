package com.niall.mohan.jamplayer;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RelativeLayout;

import com.dropbox.client2.DropboxAPI;
import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNFingerprintResult;
import com.gracenote.mmid.MobileSDK.GNFingerprintResultReady;
import com.gracenote.mmid.MobileSDK.GNOperationStatusChanged;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;
import com.gracenote.mmid.MobileSDK.GNStatus;
import com.niall.mohan.jamplayer.adapters.JamSongs;

/**
 * Implement Text Search with Artist/Album/Track combination.
 */
public class TextSearchTask implements GNSearchResultReady,
		GNOperationStatusChanged {
	GNConfig config;
	String songUrl;
	MusicTable tb = new MusicTable();
	public void textSearch(String artist, String album, String track, String url) {
		songUrl = url; //our url per search as settings activity is in a for each loop for each file that we want.
		GNOperations.searchByText(this, config, artist, album, track);
	}

	/**
	 * Intermediate status update when webservices is contacted
	 */

	public void GNStatusChanged(GNStatus status) {
		updateStatus(status.getMessage(), true);
	}

	@Override
	public void GNResultReady(GNSearchResult result) {
		if (result.isFailure()) {
			// An error occurred so display the error to the user.
			String msg = String.format("[%d] %s", result.getErrCode(),
					result.getErrMessage());
			updateStatus(msg, false); // Display error while leaving the
			// prior status update
		} else {
			if (result.isFingerprintSearchNoMatchStatus()) {
				// Handle special case of webservices lookup with no match
				Log.i("TABS","no result");
			} else {
				// Text search can return 0 to N responses
				GNSearchResponse resp = result.getBestResponse();
				JamSongs song = new JamSongs(resp.getTrackTitle(), songUrl, 1, resp.getAlbumTitle(), null, resp.getArtist(), resp.getTrackTitle());
				SettingsActivity.dropboxSongs.add(song);
				Log.i("SONGS",String.valueOf(SettingsActivity.dropboxSongs.size()));
				//Log.i("BEST RESULT",resp.getArtist()+" "+resp.getAlbumTitle()+" "+resp.getTrackTitle()+"PATH = "+songUrl);
				//push dropbox stuff to db here.
				//probably need to pass the results/file paths to each other, so we can connect files with our new metadata.
			}

			updateStatus("Success", true);
		}
	}
	private void updateStatus(String status, boolean clearStatus) {
		if (clearStatus) {
			Log.i("TAB", status);
		} else {
			Log.i("TAB", status + status);
		}
	}
	private void pushToDb(JamSongs song) {
		tb.insert(song);
	}

}

