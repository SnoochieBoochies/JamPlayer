package com.niall.mohan.jamplayer.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

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
import com.niall.mohan.jamplayer.adapters.JamBaseAdapter;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

//Using TabActivity as my test phone is running 2.3 and I don't have time to mess with ActionbarSherlock...
@SuppressWarnings("deprecation")
public class TabsActivity extends TabActivity implements OnTabChangeListener, OnItemClickListener,ViewPager.OnPageChangeListener {
	private static String TAG = "TabsActivity";
	private TabHost mTabHost;
	private ListView list_song;
	public static BaseAdapter displayAdapter;
	public static ExpandableListAdapter exp_list_adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this,JamService.class));
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
		mTabHost.addTab(mTabHost.newTabSpec("Google").setIndicator("Google").setContent(new Intent(this,GooglePlayActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Soundcloud").setIndicator("Soundcloud").setContent(new Intent(this,SoundcloudActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Dropbox").setIndicator("Dropbox").setContent(new Intent(this,DropboxActivity.class)));
        //mTabHost.addTab(mTabHost.newTabSpec(MediaUtils.Data_Local).setIndicator(MediaUtils.Data_Local).setContent(R.id.exp_list_local));
        mTabHost.addTab(mTabHost.newTabSpec("Local").setIndicator("LOCAL").setContent(new Intent(this,LocalActivity.class)));
        mTabHost.setOnTabChangedListener(this);
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.i(TAG, "item click");
		startService(new Intent(JamService.ACTION_PLAY));
		
	}

	@Override
	public void onTabChanged(String tabId) {
		Log.i(TAG, "tab changed " + tabId);
		MediaUtils utils = MediaUtils.getInstance();
		//utils.bind_data_adapter(this, tabId);
		//if(tabId.equals("Local"))
			//startActivity(new Intent(getApplicationContext(), LocalActivity.class));

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
