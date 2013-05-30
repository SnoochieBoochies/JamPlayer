package com.niall.mohan.jamplayer.tabs;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;

//Using TabActivity as my test phone is running 2.3 and I don't have time to mess with ActionbarSherlock...
/*This class acts as the host for the tabs. It represents the tab buttons at the top of the screen
 * and adds the bindings of each activity to those tabs.*/
@SuppressWarnings("deprecation")
public class TabsActivity extends TabActivity implements OnTabChangeListener {
	private static String TAG = "TabsActivity";
	public TabHost mTabHost;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabmain);
		Log.i(TAG, "onCreate()");
		if (savedInstanceState != null) {
        }
		createTabs();
	}
	protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

	private void createTabs() {
		mTabHost = getTabHost();
		getTabHost().getTabWidget().setDividerDrawable(R.drawable.empty_divider);
		mTabHost.addTab(mTabHost.newTabSpec("Google").setIndicator("Google").setContent(new Intent(this,GooglePlayActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Soundcloud").setIndicator("Soundcloud").setContent(new Intent(this,SoundcloudActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Dropbox").setIndicator("Dropbox").setContent(new Intent(this,DropboxActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Local").setIndicator("Local").setContent(new Intent(this,LocalActivity.class)));
        mTabHost.setOnTabChangedListener(this);
	}
	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onTabChanged(String tabId) {
		Log.i(TAG, "tab changed " + tabId);
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
		getMenuInflater().inflate(R.menu.sub_menu_one, menu);
		return true;
	}

}
