package com.niall.mohan.jamplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{
	final String SUGGESTED_URL = "https://www.dropbox.com/s/1qdxajykco6kipf/Ayreon%20-%201995%20-%20The%20Final%20Experiment%20%5BCD2%5D.flac";
    //List<Song> songs = new ArrayList<Song>();
    MusicRetriever mRetriever;
    ImageButton mPlayPauseButton;
    Button mEjectButton;
    ListView lv;
    Cursor cr;
    boolean change;
    private SeekBar songProgressBar;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    private long mLastSeekEventTime;
    private long mDuration;
    
    private ExpandableListView mExpandableList;
    //this will be for the change to the service so we can call service stuff in the activity
    private ServiceConnection mConnection = new ServiceConnection()
    {
		public void onServiceConnected(ComponentName className, IBinder service) {
		}

		public void onServiceDisconnected(ComponentName className) {
		}
    };

    /**
     * Called when the activity is first created. Here, we simply set the event listeners and
     * start the background service ({@link MusicService}) that will handle the actual media
     * playback.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPlayPauseButton = (ImageButton) findViewById(R.id.control_play_btn);
        mPlayPauseButton.setOnClickListener(playPauseHandler);
        //songProgressBar = (SeekBar) findViewById(R.id.seekBar1);
        songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
        //songProgressBar.setOnSeekBarChangeListener(seekBarListener);
        //-----------------------------------------------------------    
        
        //mRetriever = new MusicRetriever(this);
        //mRetriever.open();
        //songs = mRetriever.getItems();
        //List<Song> list = new ArrayList<Song>();
        //for(int i =0;i<songs.size();i++) {
        //	list.add(songs.get(i));
        //}
        //lv = (ListView) findViewById(R.id.listView);
        //ArrayAdapter<Song> adapter = new ArrayAdapter<Song>(this, android.R.layout.simple_list_item_1, list);
        //lv.setAdapter(adapter);	 
        //Log.i("SONGS SIZE", String.valueOf(mRetriever.songs.size()));
        
        //mExpandableList = (ExpandableListView)findViewById(R.id.listView);
        //songs = mRetriever.getItems();
        List<String> artists = new ArrayList<String>();
        List<String> albums = new ArrayList<String>();
        List<String> song = new ArrayList<String>();
        /*
        ArrayList<String> arrayParents = new ArrayList<String>();
        ArrayList<String> arrayChildren = new ArrayList<String>();
        for(int i =0;i<songs.size();i++) {
        	//Parent parent = new Parent();
        	//parent.setTitle(songs.get(i).getArtist());
        	arrayChildren.add(songs.get(i).getAlbum());
        	//parent.setArrayChildren(arrayChildren);
        	arrayParents.add(songs.get(i).getArtist());

        }
        mExpandableList.setAdapter(new expandableAdapter(MainActivity.this,arrayParents));
        */
        //Set up for list view so it looks nice(artist name + song etc)
        /*
        lv = (ListView) findViewById(R.id.listView);
        songs = mRetriever.getItems();
        List<String> list = new ArrayList<String>();
        for(int i =0;i<songs.size();i++) {
        	list.add(i, songs.get(i).getTitle());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter); 
        lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
				Log.i("on click",String.valueOf(songs.get(position).getArtist()));
				Intent intent = new Intent(JamService.ACTION_TOGGLE_PLAYBACK);
				intent.putExtra("position", position);
				startService(intent);
				//mPlayPauseButton.setImageResource(R.drawable.ic_appwidget_music_pause);
			}
		});
        Intent intent = new Intent(MainActivity.this, MusicIntentReceiver.class);
    	change = intent.getBooleanExtra("isplaying", false);
    	Log.i("HUEH",String.valueOf(intent.hasExtra("isplaying")));
    	*/

    }
    
    View.OnClickListener playPauseHandler = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.isSelected()){
                v.setSelected(false);
            	startService(new Intent(JamService.ACTION_PLAY));
            	doPlayPauseButton();
                //...Handle toggle off
            } else {
                v.setSelected(true);
                doPlayPauseButton();
                startService(new Intent(JamService.ACTION_PAUSE));
                //...Handled toggle on
            }
        }
    };
    OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener() {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mLastSeekEventTime = 0;
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
			if(!fromUser) return;
			long now = SystemClock.elapsedRealtime();
			if((now - mLastSeekEventTime) > 250) {
				mLastSeekEventTime = now;
				//finish this.
			}
		}
	};
    private void doPlayPauseButton() {
    	mPlayPauseButton.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.playback_toggle));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            	mPlayPauseButton.setImageResource(R.drawable.ic_appwidget_music_pause);
            	startService(new Intent(JamService.ACTION_TOGGLE_PLAYBACK));
            	Log.i("PLAY PAUSE", "TRUE");
            	return true;
            case KeyEvent.KEYCODE_HEADSETHOOK:
                startService(new Intent(JamService.ACTION_TOGGLE_PLAYBACK));
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
	@Override
	public void onClick(View v) { 
		showUrlDialog();
	}
	 /** 
     * Shows an alert dialog where the user can input a URL. After showing the dialog, if the user
     * confirms, sends the appropriate intent to the {@link MusicService} to cause that URL to be
     * played.
     */
    void showUrlDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Manual Input");
        alertBuilder.setMessage("Enter a URL (must be http://)");
        final EditText input = new EditText(this);
        alertBuilder.setView(input);

        input.setText(SUGGESTED_URL);

        alertBuilder.setPositiveButton("Play!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {
                // Send an intent with the URL of the song to play. This is expected by
                // MusicService.
                Intent i = new Intent(JamService.ACTION_URL);
                Uri uri = Uri.parse(input.getText().toString());
                i.setData(uri);
                startService(i);
            }
        });
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int whichButton) {}
        });

        alertBuilder.show();
    }


    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				this.startActivity(intent);
				return true;
		}
		return false;
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}
