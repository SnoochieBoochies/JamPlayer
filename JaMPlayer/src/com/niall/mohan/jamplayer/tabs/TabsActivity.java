package com.niall.mohan.jamplayer.tabs;

import com.niall.mohan.jamplayer.R;
import com.niall.mohan.jamplayer.SettingsActivity;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

//Using TabActivity as my test phone is running 2.3 and I don't have time to mess with ActionbarSherlock...
@SuppressWarnings("deprecation")
public class TabsActivity extends TabActivity implements OnTabChangeListener,ViewPager.OnPageChangeListener {
	private static String TAG = "TabsActivity";
	private TabHost mTabHost;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//startService(new Intent(JamService.ACTION_NONE));
		//startService(new Intent(this,JamService.class));
		setContentView(R.layout.tabmain);
		if (savedInstanceState != null) {
            //mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            //mTabHost.getTabWidget().getChildAt(0).getLayoutParams().width =(int) 30;
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
	public void onTabChanged(String tabId) {
		Log.i(TAG, "tab changed " + tabId);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int arg0) {
		// TODO Auto-generated method stub
		this.mTabHost.setCurrentTab(arg0);
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
