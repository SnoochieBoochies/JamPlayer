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
	private ViewPager viewPager;
	private PagerAdapter pagerAdapter;
	private ListView list_song;
	private ExpandableListView exp_list_google;
	private ExpandableListView exp_list_dropbox;
	private ExpandableListView exp_list_local;
	public static BaseAdapter displayAdapter;
	public static ExpandableListAdapter exp_list_adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabmain);
		if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            mTabHost.getTabWidget().getChildAt(0).getLayoutParams().width =(int) 30;
        }
		
		exp_list_adapter = new ExpandableListAdapter(this);
		exp_list_google= (ExpandableListView) findViewById(R.id.exp_list_google);
		exp_list_google.setAdapter(exp_list_adapter);
		exp_list_dropbox = (ExpandableListView) findViewById(R.id.exp_list_dropbox);
		exp_list_dropbox.setAdapter(exp_list_adapter);
		exp_list_local = (ExpandableListView) findViewById(R.id.exp_list_local);
		exp_list_local.setAdapter(exp_list_adapter);
		createTabs();
	}
	protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }

	private void createTabs() {
		mTabHost = getTabHost();
		mTabHost.addTab(mTabHost.newTabSpec("Google").setIndicator("Google").setContent(R.id.exp_list_google));
        mTabHost.addTab(mTabHost.newTabSpec("Soundcloud").setIndicator("Soundcloud").setContent(R.id.list_soundcloud));
        mTabHost.addTab(mTabHost.newTabSpec("Dropbox").setIndicator("Dropbox").setContent(R.id.exp_list_dropbox));
        mTabHost.addTab(mTabHost.newTabSpec(MediaUtils.Data_Local).setIndicator(MediaUtils.Data_Local).setContent(R.id.exp_list_local));
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
		int pos = this.mTabHost.getCurrentTab();
		this.viewPager.setCurrentItem(pos);

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
