package com.niall.mohan.jamplayer.tabs;

import com.google.android.gms.internal.al;
import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNOperationStatusChanged;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;
import com.gracenote.mmid.MobileSDK.GNStatus;
import com.niall.mohan.jamplayer.JamService;
import com.niall.mohan.jamplayer.MediaUtils;
import com.niall.mohan.jamplayer.R;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

//Using TabActivity as my test phone is running 2.3 and I don't have time to mess with ActionbarSherlock...
@SuppressWarnings("deprecation")
public class TabsActivity extends TabActivity implements OnTabChangeListener,ViewPager.OnPageChangeListener {
	private static String TAG = "TabsActivity";
	private TabHost mTabHost;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//startService(new Intent(this,JamService.class));
		setContentView(R.layout.tabmain);
		if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            mTabHost.getTabWidget().getChildAt(0).getLayoutParams().width =(int) 30;
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


}
