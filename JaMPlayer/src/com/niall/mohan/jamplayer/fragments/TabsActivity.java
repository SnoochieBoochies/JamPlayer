package com.niall.mohan.jamplayer.fragments;

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
import com.niall.mohan.jamplayer.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
public class TabsActivity extends FragmentActivity implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
	private TabHost tabhost;
	private ViewPager viewPager;
	private PagerAdapter pagerAdapter;
	private List<String> tabsInfo = new ArrayList<String>();
	//private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabInfo>();
	private static final int NUM_PAGES = 3;

	class TabFactory implements TabContentFactory {

		private final Context mContext;

	    /**
	     * @param context
	     */
	    public TabFactory(Context context) {
	        mContext = context;
	    }

	    /** (non-Javadoc)
	     * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
	     */
	    public View createTabContent(String tag) {
	        View v = new View(mContext);
	        v.setMinimumWidth(0);
	        v.setMinimumHeight(0);
	        return v;
	    }

	}
	GNConfig config;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Inflate the layout
		setContentView(R.layout.tabs_viewpager_layout);
		// Initialise the TabHost
		this.initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null) {
            tabhost.setCurrentTabByTag(savedInstanceState.getString("tab")); //set the tab as per the saved state
            tabhost.getTabWidget().getChildAt(0).getLayoutParams().width =(int) 30;
        }
		// Intialise ViewPager
		this.intialiseViewPager();
		
		//config = GNConfig.init("13046016-57E031D9977B0F9F9DECABBC977DE50B", this.getApplicationContext());
		//TextSearchTask t = new TextSearchTask();
		//t.textSearch("Ayreon", "", "");
	}
	
	protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", tabhost.getCurrentTabTag()); //save the tab selected
        super.onSaveInstanceState(outState);
    }
	private void intialiseViewPager() {

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, GooglePlayFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, SoundcloudFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, DropboxFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, LocalFragment.class.getName()));
		this.pagerAdapter  = new PagerAdapter(super.getSupportFragmentManager(), fragments);
		//
		this.viewPager = (ViewPager)super.findViewById(R.id.viewpager);
		this.viewPager.setAdapter(this.pagerAdapter);
		this.viewPager.setOnPageChangeListener(this);
    }
	private void initialiseTabHost(Bundle args) {
		tabhost = (TabHost)findViewById(android.R.id.tabhost);
        tabhost.setup();
        TabsActivity.AddTab(this, this.tabhost, this.tabhost.newTabSpec("Google").setIndicator("Google"));
        this.tabsInfo.add("Google");
        TabsActivity.AddTab(this, this.tabhost, this.tabhost.newTabSpec("Soundcloud").setIndicator("Soundcloud"));
        this.tabsInfo.add("Soundcloud");
        TabsActivity.AddTab(this, this.tabhost, this.tabhost.newTabSpec("Dropbox").setIndicator("Dropbox"));
        this.tabsInfo.add("Dropbox");
        TabsActivity.AddTab(this, this.tabhost, this.tabhost.newTabSpec("Local").setIndicator("Local"));
        this.tabsInfo.add("Local");  
        // Default to first tab
        //this.onTabChanged("Tab1");
        //
        tabhost.setOnTabChangedListener(this);
	}
	private static void AddTab(TabsActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec) {
		// Attach a Tab view factory to the spec
		tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
	}
	public void onTabChanged(String tag) {
		//TabInfo newTab = this.mapTabInfo.get(tag);
		int pos = this.tabhost.getCurrentTab();
		this.viewPager.setCurrentItem(pos);
		
    }
	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		this.tabhost.setCurrentTab(position);
	}
	@Override
	public void onPageScrollStateChanged(int arg0) {

	}
	@Override
	public void onPageScrolled(int position, float arg1, int arg2) {
	}
}
