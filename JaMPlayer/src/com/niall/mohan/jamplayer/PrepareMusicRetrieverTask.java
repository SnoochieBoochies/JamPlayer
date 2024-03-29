package com.niall.mohan.jamplayer;

import android.os.AsyncTask;
import android.widget.ProgressBar;

/*This class is used to prepare the media before the mediaplayer is ready. That's why it's
 * and Async task.
 */

public class PrepareMusicRetrieverTask extends AsyncTask<Void, Void, Void> {
    MusicRetriever mRetriever;
    MusicRetrieverPreparedListener mListener;
    MusicTable db;
    ProgressBar loader;
    //for the service
    public PrepareMusicRetrieverTask(MusicRetriever retriever,
            MusicRetrieverPreparedListener listener) {
        mRetriever = retriever;
        mListener = listener;
    }
    public PrepareMusicRetrieverTask(MusicRetriever retriever) {
    	mRetriever = retriever;
    }
    public PrepareMusicRetrieverTask() {}

    @Override
    protected Void doInBackground(Void... arg0) {
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        mListener.onMusicRetrieverPrepared();
    }

    public interface MusicRetrieverPreparedListener {
        public void onMusicRetrieverPrepared();
    }
}
