package com.niall.mohan.jamplayer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.SampleBuffer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.InputFilter.LengthFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.comm.GmHttpClient;
import com.android.gm.api.comm.SimpleForm;
import com.android.gm.api.model.Playlist;
import com.android.gm.api.model.QueryResults;
import com.android.gm.api.model.Song;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry.JsonExtractor;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.jsonextract.JsonExtractionException;
import com.dropbox.client2.jsonextract.JsonThing;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.TokenPair;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.gracenote.mmid.MobileSDK.GNConfig;
import com.gracenote.mmid.MobileSDK.GNOperationStatusChanged;
import com.gracenote.mmid.MobileSDK.GNOperations;
import com.gracenote.mmid.MobileSDK.GNSampleBuffer;
import com.gracenote.mmid.MobileSDK.GNSearchResponse;
import com.gracenote.mmid.MobileSDK.GNSearchResult;
import com.gracenote.mmid.MobileSDK.GNSearchResultReady;
import com.gracenote.mmid.MobileSDK.GNStatus;
import com.niall.mohan.jamplayer.adapters.GPlaySongAdapter;
import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.SongList;
import com.niall.mohan.jamplayer.tabs.SoundcloudActivity;
import com.niall.mohan.jamplayer.tabs.TabsActivity;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Chart;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
/*
 * Setup for the settings page. Settings page will have the cloud services login bits
 */
public class SettingsActivity extends AccountAuthenticatorActivity {
	WriteToCache newFolder;
	GNConfig config;
    private static final String TAG = "SettingsActivity";
	Button connectGplay;
	Button connectDropb;
	Button connectSCloud;
	Button connectLastFm;
	EditText last_fm_username;
	EditText last_fm_pswd;
	ProgressBar progress;
    private static final String YOUR_APP_CONSUMER_KEY = "9ba8dd1f82ad58e8470b3e5a69cc828c";
    private static final String YOUR_APP_CONSUMER_SECRET = "9269708fca324437b41fb738fb78a5f7";
    private static final String LAST_FM_API_KEY = "d750eda9db3bbb8873246ca0a1725726";
    private static final String LAST_FM_SECRET = "15af00d4e63c6d6e6b3d194e284743c2";
    final static private String APP_KEY = "7d5b3w41ptwxz3t";
    final static private String APP_SECRET = "1nkra6j4iyhnvnp";
    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    final static private String GRACENOTE_KEY = "13046016-57E031D9977B0F9F9DECABBC977DE50B";
    private boolean mLoggedIn;
	public DropboxAPI<AndroidAuthSession> mApi;
    SharedPreferences preferences;
	GoogleMusicApi api;
	MusicTable tb;
	Token sCloudtoken;
	private static final int GPLAY = 1;
	private static final int SCLOUD = 2;
	private static final int LAST_FM = 3;
	final URI REDIRECT_URI = URI.create("http://developers.soundcloud.com/callback.html");
	final ApiWrapper sCloudWrapper = new ApiWrapper(YOUR_APP_CONSUMER_KEY, YOUR_APP_CONSUMER_SECRET, REDIRECT_URI, null);
	String actualToken="";
	String scope;
	@Override
	protected Dialog onCreateDialog(int id) {
		final AccountManager am = AccountManager.get(SettingsActivity.this);
		WebView webView;// = new WebView(SettingsActivity.this);
		webView = (WebView) findViewById(R.id.webview);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	  switch (id) {
	    case GPLAY:
	      builder.setTitle("Select a Google account");
	      final Account[] GoogleAccounts = am.getAccountsByType("com.google");
	      final int size = GoogleAccounts.length;
	      String[] names = new String[size];
	      for (int i = 0; i < size; i++) {
	        names[i] = GoogleAccounts[i].name;
	      }
	      builder.setItems(names, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, final int which) {
	          // Stuff to do when the account is selected by the user
	        	am.getAuthToken(GoogleAccounts[which], "sj", null, SettingsActivity.this,new AccountManagerCallback<Bundle>() {
	        	    public void run(AccountManagerFuture<Bundle> future) {
	        	        try {
	        	        	String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
	        	        	Log.i("token stored: ",token);
	        	        	//It's ok to call it here as it's part of the async task.
	        	        	GoogleMusicLoginTask login = new GoogleMusicLoginTask(SettingsActivity.this) {
	        	        		@Override
	        	        		protected void onPostExecute(Boolean success) {
	        	        			Log.i(TAG,"songs size"+gPlayList.size());
	        	        			//now we have all of the songs in the list. push to the DB.
	        	        			if(success) {
	        	        				progress.setVisibility(View.VISIBLE);
	        	        				//insert the current songs into the db. later during execution when a user clicks an artist/album, we'll batch fetch those.
	        	        				//urls aren't constructed at this point. at the songlist stage there will be an async task to check for the valid
	        	        				//token(which forms part of the url). If not, get a new one and form new urls.
	        	        				for(Song s: gPlayList) {
	        	        					GPlaySongAdapter adapter = new GPlaySongAdapter(s);
	        	        					JamSongs jamToDb = adapter.setMediaInfo(s);
	        	        					//Log.i("IENRGINERG",s.getTitle());
	        	        					pushToDb(jamToDb);
	        	        				}
	        	        				progress.setVisibility(View.GONE);
	        	        				
	        	        			}
	        	        			
	        	        		}
	        	        	};
	        	        	login.execute(GoogleAccounts[which].name);
	        	        } catch (Exception e) {
	        	          e.printStackTrace();
	        	        }
	        	      }
	        	    }, null);
	        }
	      });
	      return builder.create();
    	
	    case SCLOUD:
	    	final Account[] SCloudAccounts = am.getAccountsByType("com.soundcloud.android.account");
	    	final Account sCloudAccount;
	    	final int size2 = SCloudAccounts.length;
	    	String[] names2 = new String[size2];
	    	actualToken = getPreferences(Context.MODE_PRIVATE).getString("SCloud_Access", null);
	    	scope = getPreferences(Context.MODE_PRIVATE).getString("SCloud_Scope", null);
	    	
	    	if(SCloudAccounts.length > 0) {
	    		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
	    		builder2.setTitle("Select a SCloud account");
	    		for (int i = 0; i < size2; i++) { //just a formality. i doubt many people have more than one SC account.
	    			names2[i] = SCloudAccounts[i].name;
	    		}
	    		builder2.setItems(names2, new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				// Stuff to do when the account is selected by the user
	    				String access;
						try {
							access = am.blockingGetAuthToken(SCloudAccounts[which], "access_token", true);
		    				Token toke = new Token(access,null, Token.SCOPE_NON_EXPIRING);
		    	    		Log.i(TAG,toke.toString());
		    	    		final ApiWrapper wrapper = new ApiWrapper(YOUR_APP_CONSUMER_KEY, YOUR_APP_CONSUMER_SECRET, REDIRECT_URI, toke);
		    	    		Log.i(TAG,wrapper.getToken().toString());
		    	    		SoundCloudSongTask sCloudTask = new SoundCloudSongTask(wrapper);
		    	    		sCloudTask.execute(wrapper.getToken().toString());
		    	    		
						} catch (OperationCanceledException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (AuthenticatorException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
	    			}
	    		});
	    		return builder2.create();
	    	} else if(actualToken == null) {
		    	webView.setWebViewClient(new WebViewClient() {
		    		@Override
			        public boolean shouldOverrideUrlLoading(final WebView view, String url) {
			            if (url.startsWith(REDIRECT_URI.toString())) {
			                Uri result = Uri.parse(url);
			                Log.i(TAG,result.toString());
			                String error = result.getQueryParameter("error");
			                String code = result.getQueryParameter("code");
			       		 	try {
								sCloudtoken = sCloudWrapper.authorizationCode(code);
								Log.i(TAG, "CODE: "+sCloudtoken.toString());
								Editor writer = getPreferences(Context.MODE_PRIVATE).edit();
								writer.putString("SCloud_Access", sCloudtoken.access);
								writer.commit();
								writer.putString("SCloud_Scope", sCloudtoken.scope);
								writer.commit();
							} catch (IOException e) { 	
								e.printStackTrace();
							}
			                Log.i(TAG,error+" "+code);
			                return true;
			            }
			            return false;
			        }
			    });
		    	webView.loadUrl(String.valueOf(sCloudWrapper.authorizationCodeUrl(Endpoints.CONNECT, Token.SCOPE_NON_EXPIRING)));
		    	setContentView(webView);
		    	webView.requestFocus(View.FOCUS_DOWN);
	    	} else {
	    		//i have an access token. do stuff.
	    		//Log.i(TAG, "HAVE ACCESS TOKEN: "+ actualToken);
	    		Token t = new Token(actualToken, null, scope);
	    		Log.i(TAG,t.toString());
	    		final ApiWrapper wrapper = new ApiWrapper(YOUR_APP_CONSUMER_KEY, YOUR_APP_CONSUMER_SECRET, REDIRECT_URI, t);
	    		Log.i(TAG,wrapper.getToken().toString());
	    		try {
	    			//Request resource = Request.to(Endpoints.MY_FOLLOWINGS);.with("client_id","9ba8dd1f82ad58e8470b3e5a69cc828c");
	    			Request resource = Request.to("/me/followings/tracks");
	    			//Stream stream = wrapper.resolveStreamUrl(resource.toUrl(), true);
	    			Log.i(TAG, resource.toUrl());
	    			//Log.i(TAG, stream.streamUrl);
					HttpResponse resp = wrapper.get(resource);
					if(wrapper.get(resource).getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						//Log.i(TAG, "LEL");
						//Stream [] stream;// = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
						//stream[0] = Http.getString(resp);
						//Stream stream = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
						//Log.i(TAG,stream.toString());
						/*This needs to be packaged up and sent to the playing activity i guess*/
						BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
						String json = reader.readLine();
						String [] d;
						JSONTokener tokener = new JSONTokener(json);
						JSONArray finalResult = new JSONArray(tokener);
						Log.i(TAG,finalResult.getString(0));
						d = new String[finalResult.length()];
						for(int i = 0; i <d.length;i++) {
							JSONObject f = finalResult.getJSONObject(i);
							d[i] = f.getString("stream_url");
						}
					} else if(wrapper.get(resource).getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}   	
	    case LAST_FM:
	    	Caller.getInstance().setCache(null); //API workaround. disables caching.	    	 
	    	builder.setTitle("Login to Last.fm");
	    	LayoutInflater inflater = this.getLayoutInflater();
	    	final View layout = inflater.inflate(R.layout.last_fm_signin, null);
	    	builder.setView(layout).setPositiveButton("Sign in", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					last_fm_username = (EditText)layout.findViewById(R.id.username);
					last_fm_pswd = (EditText)layout.findViewById(R.id.password);
					String un = last_fm_username.getText().toString();
					String pw = last_fm_pswd.getText().toString();
					Session session = Authenticator.getMobileSession(un, pw, LAST_FM_API_KEY, LAST_FM_SECRET);
					if(session.getKey() != null) {
						Editor writer = getPreferences(Context.MODE_PRIVATE).edit();
						writer.putString("LastFm_Access", session.getKey());
						writer.commit();
					} else {
						showToast("Login Failed, try again.");
					}
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                  dialog.cancel();
	               }
	           });      
	    return builder.create();
	  }
	  return null;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		tb= new MusicTable(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_layout);
		connectGplay = (Button)findViewById(R.id.connect_gplay_btn);
		connectDropb = (Button)findViewById(R.id.connect_dropbox_btn);
		connectSCloud = (Button)findViewById(R.id.connect_scloud_btn);
		connectLastFm = (Button)findViewById(R.id.connect_last_fm_btn);
		progress = (ProgressBar) findViewById(R.id.progress);
		AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
        newFolder = new WriteToCache();
		connectGplay.setOnClickListener(new OnClickListener() {
			//need to check after auth() is the database already occupied and are there any changes.
			@Override
			public void onClick(View arg0) {
				showDialog(GPLAY);
			}});
		connectDropb.setOnClickListener(new OnClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onClick(View arg0) {
				if(mLoggedIn) {
					logOut();
				}else {
                    // Start the remote authentication
                    mApi.getSession().startAuthentication(SettingsActivity.this);
					String [] tokes = getKeys();
					//Log.i(TAG, tokes[0] + " + "+ tokes[1]);
					config = GNConfig.init(GRACENOTE_KEY, getApplicationContext());
                }
				
	            
			}
			
		});
		connectSCloud.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				synchronized (this) {
					showDialog(SCLOUD);
				}
			}
		});
		connectLastFm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(LAST_FM);
			}
		});
		
	}
	/*------Catch all method to push songs into the DB from dropbox--------------*/
	void pushToDb(JamSongs song) {
		tb.insert(song);
	}

	/*----------------DROPBOX----------------------*/
	private void logOut() {
        // Remove credentials from the session
        mApi.getSession().unlink();

        // Clear our stored keys
       // clearKeys();
        // Change UI state to display logged out version
        setLoggedIn(false);
    }
	private void setLoggedIn(boolean loggedIn) {
    	mLoggedIn = loggedIn;
    	if (loggedIn) {
    		connectDropb.setText("Unlink from Dropbox");

    	} 
    	
    }
	private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
        	//RequestTokenPair token = new RequestTokenPair(stored[0], stored[1]);
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }
    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }
    private void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }
 
    /*------------------END OF DROPBOX-----------------------------*/
    static ArrayList<JamSongs> dropboxSongs = new ArrayList<JamSongs>();
	@Override
    protected void onResume() {
        super.onResume();
        //store our tokens in here after all or any of the authorisations are completed.
        AndroidAuthSession session = mApi.getSession();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
        	DropboxSongsTask dbTask = new DropboxSongsTask();
        	dbTask.execute();
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                TokenPair tokens = session.getAccessTokenPair();
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);

            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
            
        }
    }
	public class DropboxSongsTask extends AsyncTask<String, Void, Boolean> {
		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(Boolean result) {
			progress.setVisibility(View.GONE);
			super.onPostExecute(result);
		}
		@Override
		protected Boolean doInBackground(String... params) {
			boolean success = false;
			try {
				ArrayList<Entry> files = new ArrayList<DropboxAPI.Entry>();
	            String [] formats = {".mp3",".flac",".ogg",".wav"}; //supported formats of the MEdiaPlayer
	            TextSearchTask graceNoteSearch;
	            for(int i = 0; i < formats.length; i++)
	            	files.addAll((ArrayList<Entry>) mApi.search("/", formats[i], 100, false));
				
	            for(Entry e : files) {
	            	DropboxLink url = mApi.media(e.path, false);
	            	Log.i(TAG,url.url);
	                graceNoteSearch = new TextSearchTask();
	                graceNoteSearch.config = config;
	                //graceNoteSearch.execute(url.url);
	                //GNOperations.searchByText(graceNoteSearch, config, e.fileName(), e.fileName(), e.fileName());
	                //graceNoteSearch.textSearch(e.fileName(), e.fileName(), e.fileName(), url.url);
	                Log.i(TAG, e.path);
	                

	            }
	            File f = newFolder.getAlbumStorageDir();
	            File [] fa = f.listFiles();
	            Log.i(TAG,fa[0].getAbsolutePath()+fa[1].getAbsolutePath());
	            //FileOutputStream os = new FileOutputStream(f.getAbsolutePath()+"/Intronaut - Valley of Smoke - Above.mp3");
	            //DropboxFileInfo info = mApi.getFile("/Intronaut - Valley of Smoke - Above.mp3", null, os, null);
	            //http://dl.dropbox.com/1/view/4gi2cufj2w3lzjm/Apps/JaM/Intronaut%20-%20Valley%20of%20Smoke%20-%20Above.mp3
	            //get songs from textsearch now that the mediaInfo object is constructed with all the info we need.
	            Log.i(TAG+"SONGS",String.valueOf(dropboxSongs.size()));
	            RecognizePCMStreamTask task = new RecognizePCMStreamTask();
	            task.mconfig = config;
	            try {
					task.recognizePCMStream();
				} catch (BitstreamException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (DecoderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	            success = true;
	            File a = SettingsActivity.this.getFilesDir();
	            String [] t =a.list();
	            Log.i(TAG, t[0]);
			} catch (DropboxException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
			return success;
		}
		
	}
	
	public class SoundCloudSongTask extends AsyncTask<String, Void, Boolean> {
		SettingsActivity activity;
		ApiWrapper wrapper;
		
		public SoundCloudSongTask(ApiWrapper wrapper) {
			this.wrapper = wrapper;
		}
		@Override
		protected void onPreExecute() {
			progress.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		@Override
		protected void onPostExecute(Boolean result) {
			progress.setVisibility(View.GONE);
			super.onPostExecute(result);
		}
		@Override
		protected Boolean doInBackground(String... params) {
			boolean success = false;
			try {
				//Request resource = Request.to(Endpoints.MY_FOLLOWINGS);.with("client_id","9ba8dd1f82ad58e8470b3e5a69cc828c");
				Request resource = Request.to("/me/followings/tracks");
				//Stream stream = wrapper.resolveStreamUrl(resource.toUrl(), true);
				//Log.i(TAG, resource.toUrl());
				//Log.i(TAG, stream.streamUrl);
				HttpResponse resp = wrapper.get(resource);
				if(wrapper.get(resource).getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					//Log.i(TAG, "LEL");
					//Stream [] stream;// = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
					//stream[0] = Http.getString(resp);
					//Stream stream = wrapper.resolveStreamUrl("http://api.soundcloud.com/tracks/23999498/stream?client_id=9ba8dd1f82ad58e8470b3e5a69cc828c", true);
					//Log.i(TAG,stream.toString());
					/*This needs to be packaged up and sent to the playing activity i guess*/
					BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
					String json = reader.readLine();
					JamSongs [] d;
					JSONTokener tokener = new JSONTokener(json);
					JSONArray finalResult = new JSONArray(tokener);
					Log.i(TAG,finalResult.getString(0));
					d = new JamSongs[finalResult.length()];
					for(int i = 0; i <d.length;i++) {
						JSONObject f = finalResult.getJSONObject(i);
						d[i] = new JamSongs(f.getString("title"),f.getString("stream_url"),"soundcloud",f.getString("label_name"),f.getString("duration"),
								f.getString("label_name"), -1, "");
						Log.i(TAG,d[i].title);
						//Log.i(TAG,d[i]);
					}
					success = true;
				}
			} catch (IOException io) {
				io.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return success;
		}
		
	}
	/**
     * Represents an asynchronous task used to authenticate a user against the
     * GooglePlay Service
     */
	List<Song> gPlayList;
    public class GoogleMusicLoginTask extends AsyncTask<String, Void, Boolean> {
    	protected SettingsActivity mActivity;
    	protected SongList act;
        protected String mScope;
        protected String mEmail;
        protected int mRequestCode;
        GoogleMusicLoginTask(SettingsActivity activity) {
        	this.mActivity = activity;
        }
        public GoogleMusicLoginTask(SongList activity) {
			this.act = activity;
		}
        @Override
        protected Boolean doInBackground(String... params) {
        	boolean success = false;
            try {
  	          String token = GoogleAuthUtil.getToken(mActivity,params[0], "sj");
  	          Log.i(TAG, "TOKE MOTHERFUCKER: \n"+ token);
  	          if (!TextUtils.isEmpty(token)) {
                GoogleMusicApi.createInstance(mActivity);
                success = GoogleMusicApi.login(mActivity, token);
                gPlayList = GoogleMusicApi.getAllSongs(mActivity);
                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor writer = preferences.edit();
				writer.putString("Google_Access", token);
				writer.commit();
                if (!success)
                	GoogleAuthUtil.invalidateToken(mActivity, token);
  	          }
            } catch (GooglePlayServicesAvailabilityException playEx) {
                // GooglePlayServices.apk is either old, disabled, or not present.
                //mActivity.showDialog(playEx.getConnectionStatusCode());
                GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(), mActivity, 1001);
            } catch (UserRecoverableAuthException e) {
                mActivity.startActivityForResult(e.getIntent(), 1001);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            } catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            return success;
  	         
        }

        @Override
        protected void onPreExecute() {
        	progress.setVisibility(View.VISIBLE);
        	super.onPreExecute();
        }
    }
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /*Made this an inner class just for ease of passing objects.*/
    private class TextSearchTask implements GNSearchResultReady, GNOperationStatusChanged {
    	GNConfig config;
    	String songUrl;
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
    			if (result.isTextSearchNoMatchStatus()) {
    				// Handle special case of webservices lookup with no match
    				Log.i("TABS","no result");
    			} else {
    				// Text search can return 0 to N responses
    				GNSearchResponse resp = result.getBestResponse();
    				JamSongs song = new JamSongs(resp.getTrackTitle(), songUrl, "dropbox", resp.getAlbumTitle(), null, resp.getArtist(), resp.getTrackNumber(), resp.getTrackId());
    				SettingsActivity.dropboxSongs.add(song);
    				pushToDb(song);
    				Log.i("SONGS",String.valueOf(SettingsActivity.dropboxSongs.size()));
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
    }
    public class RecognizePCMStreamTask implements GNSearchResultReady,
	GNOperationStatusChanged {
    	GNConfig mconfig;
    	private GNSampleBuffer loadSamplePCM() throws IOException, BitstreamException, DecoderException {
			int channel = 1;
			int sampleRate = 8000;
			int length = (int) (channel * sampleRate * 2 * 6.5);
			/*
			HttpURLConnection con;
    		URL url2 = new URL("http://redbrick.dcu.ie/~chalk/01%20...Baby%20One%20More%20Time.mp3");
    		con = (HttpURLConnection) url2.openConnection();
    		con.setRequestProperty("Range", "bytes=1040000");
    		con.connect();
    		InputStream is = con.getInputStream();
    		File file = new File(SettingsActivity.this.getFilesDir(),"pcm.mp3");
    		Log.i(TAG,file.getAbsolutePath());
    		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()));
    		byte [] pcmBuffer = new byte[1040000];
    		int i =0;
    		while((i = is.read(pcmBuffer,0,pcmBuffer.length)) != -1)
    			os.write(pcmBuffer, 0,i);
    		os.flush();
    		os.close();
    		byte [] pcmBuffer2 = new byte[1040000];
    		FileInputStream is2 = new FileInputStream(file);
    		*/

			FileInputStream is2 = new FileInputStream("mnt/sdcard/Music/JaM Music/01 ...Baby One More Time.mp3");
			Bitstream bits = new Bitstream(is2);
			Decoder decode = new Decoder();

			//SampleBuffer output = (SampleBuffer) decode.decodeFrame(frameHeader, bitstream);
			//byte[] array = MyShortToByte(output.getBuffer());
			//at.write(array,  0,  array.length);
			File pcm = new File(SettingsActivity.this.getFilesDir(),"pcm.pcm");
			FileOutputStream os = new FileOutputStream(pcm);
			boolean done = false;
			byte[] array = new byte[1040000];
			InputStream data = new FileInputStream("mnt/sdcard/Music/JaM Music/01 ...Baby One More Time.mp3");
			Bitstream bitstream = new Bitstream(data);
			Decoder decoder = new Decoder();
			array = decode(is2,0,16740);
			//while(!done) {
			//  javazoom.jl.decoder.Header frameHeader = bitstream.readFrame();
			//  if(frameHeader == null)
			//	  done = true;
			 // SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
			 // short [] buff = buffer.getBuffer();

			 

			  //bitstream.closeFrame();
		//	}
			os.close();
			//FileInputStream pcmIS = new FileInputStream(pcm);
    		//try {
			//	DataInputStream dataIn = new DataInputStream(pcmIS);
			//	dataIn.read(pcmBuffer2, 0, pcmBuffer2.length);
			//	dataIn.close();
			//} catch (IOException e) {
			//	pcmBuffer2 = null;
			//}
			
			return new GNSampleBuffer(array, channel, sampleRate);
		}
    	public byte[] decode(FileInputStream inputStream, int startMs, int maxMs) 
    			throws IOException, DecoderException {
    			ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
    			
    			float totalMs = 0;
    			boolean seeking = true;
    			
    			try {
    				Bitstream bitstream = new Bitstream(inputStream);
    				Decoder decoder = new Decoder();
    				
    				boolean done = false;
    				while (! done) {
    					javazoom.jl.decoder.Header frameHeader = bitstream.readFrame();
    					if (frameHeader == null) {
    						done = true;
    					} else {
    						totalMs += frameHeader.ms_per_frame();

    						if (totalMs >= startMs) {
    							seeking = false;
    						}
    						
    						if (! seeking) {
    							SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
    							
    							if (output.getSampleFrequency() != 44100
    									|| output.getChannelCount() != 2) {
    								throw new javazoom.jl.decoder.DecoderException("mono or non-44100 MP3 not supported", null);
    							}
    							
    							short[] pcm = output.getBuffer();
    							for (short s : pcm) {
    								outStream.write(s & 0xff);
    								outStream.write((s >> 8 ) & 0xff);
    							}
    						}
    						
    						if (totalMs >= (startMs + maxMs)) {
    							done = true;
    						}
    					}
    					bitstream.closeFrame();
    				}
    				
    				return outStream.toByteArray();
    			} catch (BitstreamException e) {
    				throw new IOException("Bitstream error: " + e);
    			} catch (DecoderException e) {
    				Log.w(TAG, "Decoder error", e);
    				throw new DecoderException(maxMs, e);
    			} 
    		}

		public void recognizePCMStream() throws IOException, BitstreamException, DecoderException {
			GNSampleBuffer samplePCMBuffer = loadSamplePCM();

			GNOperations.recognizeMIDStreamFromPcm(this, mconfig,
					samplePCMBuffer);
		}

/**
 * When the fingeprint/lookup operation is finished, this method will be
 * invoked in the UI thread to report results.
 */

		public void GNResultReady(GNSearchResult result) {
			if (result.isFailure()) {
		// 	An error occurred so display the error to the user.
				String msg = String.format("[%d] %s", result.getErrCode(),
				result.getErrMessage());
				updateStatus(msg, false); // Display error while leaving the
		// 	prior status update
			} else {
				if (result.isFingerprintSearchNoMatchStatus()) {
			// Handle special case of webservices lookup with no match
					Log.i("EWRGON","NONE");
				} else {
					//GNSearchResponse bestResponse = result.getBestResponse();
					GNSearchResponse [] resp = result.getResponses();
					for(GNSearchResponse s : resp) {
						Log.i("RESPONsssssE",s.getArtist());	
					}
				}
				updateStatus("Success", true);
	}

}

/**
 * Intermediate status update from the fingerprinter
 */

		public void GNStatusChanged(GNStatus status) {
			updateStatus(status.getMessage(), true);
		}
    	private void updateStatus(String status, boolean clearStatus) {
    		if (clearStatus) {
    			Log.i("TAB", status);
    		} else {
    			Log.i("TAB", status + status);
    		}
    	}
	}
}
