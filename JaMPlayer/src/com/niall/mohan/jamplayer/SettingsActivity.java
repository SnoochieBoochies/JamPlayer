package com.niall.mohan.jamplayer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.gm.api.GoogleMusicApi;
import com.android.gm.api.model.Song;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.gracenote.mmid.MobileSDK.GNConfig;
import com.niall.mohan.jamplayer.adapters.GPlaySongAdapter;
import com.niall.mohan.jamplayer.adapters.JamSongs;
import com.niall.mohan.jamplayer.tabs.GooglePlayActivity;
import com.niall.mohan.jamplayer.tabs.SongList;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Token;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
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
    private boolean mLoggedIn;
	public DropboxAPI<AndroidAuthSession> dApi;
    SharedPreferences preferences;
	GoogleMusicApi gApi;
	MusicTable tb;
	Token sCloudtoken;
	private static final int GPLAY = 1;
	private static final int SCLOUD = 2;
	private static final int LAST_FM = 3;
	final URI REDIRECT_URI = URI.create("http://developers.soundcloud.com/callback.html");
	final ApiWrapper sCloudWrapper = new ApiWrapper(Constants.YOUR_APP_CONSUMER_KEY, Constants.YOUR_APP_CONSUMER_SECRET, REDIRECT_URI, null);
	String actualToken="";
	String scope;
	GoogleMusicLoginTask login;
	boolean gPlaySuccess;
	@Override
	protected Dialog onCreateDialog(int id) {
		final AccountManager am = AccountManager.get(SettingsActivity.this);
		WebView webView;// = new WebView(SettingsActivity.this);
		webView = (WebView) findViewById(R.id.webview);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
	        				//insert the current songs into the db. later during execution when a user clicks an artist/album, we'll batch fetch those.
	        				//urls aren't constructed at this point. at the songlist stage there will be an async task to check for the valid
	        				//token(which forms part of the url). If not, get a new one and form new urls.
	        				final Thread thread = new Thread(new Runnable() {
								@Override
								public void run() {
									for(Song s: gPlayList) {
        	        					GPlaySongAdapter adapter = new GPlaySongAdapter(s);
        	        					JamSongs jamToDb = adapter.setMediaInfo(s);
        	        					//Log.i("IENRGINERG",s.getTitle());
        	        					pushToDb(jamToDb);
        	        				}
        	        				progress.setVisibility(View.GONE);
								}
							});
	        	        	//It's ok to call it here as it's part of the async task.
	        	        	login = new GoogleMusicLoginTask(SettingsActivity.this) {
	        	        		@Override
	        	        		protected void onPostExecute(Boolean result) {
	        	        			super.onPostExecute(result);
	        	        			if(result) {
	        	        				progress.setVisibility(View.GONE);
	        	        				connectGplay.setText("Unlink from Google");
	        	        				thread.run();
	        	        			}
	        	        		}
	        	        	};
	        	        	login.execute(GoogleAccounts[which].name);	
	        	        	progress.setVisibility(View.VISIBLE);
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
	    	actualToken = preferences.getString("SCloud_Access", null);
	    	scope = preferences.getString("SCloud_Scope", null);
	    	
	    	if(SCloudAccounts.length > 0) {
	    		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
	    		builder2.setTitle("Select a SCloud account");
	    		for (int i = 0; i < size2; i++) { //just a formality. i doubt many people have more than one SC account.
	    			names2[i] = SCloudAccounts[i].name;
	    		}
	    		builder2.setItems(names2, new DialogInterface.OnClickListener() {
	    			@SuppressWarnings("deprecation")
					public void onClick(DialogInterface dialog, final int which) {
	    				// Stuff to do when the account is selected by the user
	    				am.getAuthToken(SCloudAccounts[which], "access_token", true, new AccountManagerCallback<Bundle>() {
	    					@Override
	    					public void run(AccountManagerFuture<Bundle> arg0) {
	    						try {
									String access = arg0.getResult().getString(AccountManager.KEY_AUTHTOKEN);
									Log.i(TAG, access);
									Editor writer = preferences.edit();
									writer.putString("SCloud_Access_Key", access);
									writer.commit();
									connectSCloud.setText("Unlink from Soundcloud");
								} catch (OperationCanceledException e) {
									e.printStackTrace();
								} catch (AuthenticatorException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}						
	    					}
	    				}, null);
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
								Editor writer = preferences.edit();
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
					Session session = Authenticator.getMobileSession(un, pw, Constants.LAST_FM_API_KEY, Constants.LAST_FM_SECRET);
					if(session.getKey() != null) {
						Editor writer = preferences.edit();
						writer.putString("LastFm_Access", session.getKey());
						writer.commit();
						writer.putString("LastFm_User", session.getUsername());
						writer.commit();
						connectLastFm.setText("Unlink from Last.fm");
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
		if(savedInstanceState != null) {
			connectGplay.setText(savedInstanceState.getString("gplay"));
			connectDropb.setText(savedInstanceState.getString("dropbox"));
			connectLastFm.setText(savedInstanceState.getString("lastfm"));
			connectSCloud.setText(savedInstanceState.getString("scloud"));
			
		}
		progress = (ProgressBar) findViewById(R.id.progress);
		AndroidAuthSession session = buildSession();
        dApi = new DropboxAPI<AndroidAuthSession>(session);
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
					dApi.getSession().startAuthentication(SettingsActivity.this);
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
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState()");
		super.onSaveInstanceState(outState);
		outState.putString("gplay", connectGplay.getText().toString());
		outState.putString("dropbox", connectDropb.getText().toString());
		outState.putString("lastfm", connectLastFm.getText().toString());
		outState.putString("scloud", connectSCloud.getText().toString());
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor writer = preferences.edit();
		writer.putString("gplay", connectGplay.getText().toString());
		writer.putString("dropbox", connectDropb.getText().toString());
		writer.putString("lastfm", connectLastFm.getText().toString());
		writer.putString("scloud", connectSCloud.getText().toString());
		writer.commit();
	}
	/*------Catch all method to push songs into the DB from dropbox--------------*/
	void pushToDb(JamSongs song) {
		tb.insert(song);
	}

	/*----------------DROPBOX----------------------*/
	private void logOut() {
        // Remove credentials from the session
        dApi.getSession().unlink();
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
        AppKeyPair appKeyPair = new AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET);
        AndroidAuthSession session;
        String[] stored = getKeys();
        if (stored != null) {
        	//RequestTokenPair token = new RequestTokenPair(stored[0], stored[1]);
            AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE, accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, Constants.ACCESS_TYPE);
        }

        return session;
    }


    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(Constants.ACCESS_KEY_NAME, null);
        String secret = prefs.getString(Constants.ACCESS_SECRET_NAME, null);
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
        SharedPreferences prefs = getSharedPreferences(Constants.ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(Constants.ACCESS_KEY_NAME, key);
        edit.putString(Constants.ACCESS_SECRET_NAME, secret);
        edit.commit();
    }
 
    /*------------------END OF DROPBOX-----------------------------*/
	@Override
    protected void onResume() {
        super.onResume();
        //store our tokens in here after all or any of the authorisations are completed.
        AndroidAuthSession session = dApi.getSession();
        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Dropbox authentication completes properly.
        if (session.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();
                // Store it locally in our app for later use
                AccessTokenPair tokens = session.getAccessTokenPair();
                Log.i(TAG, tokens.key+" "+tokens.secret);
                storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
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
    	protected GooglePlayActivity act2;
        protected String mScope;
        protected String mEmail;
        protected int mRequestCode;
        GoogleMusicLoginTask(SettingsActivity activity) {
        	this.mActivity = activity;
        }
        public GoogleMusicLoginTask(SongList activity) {
			this.act = activity;
		}
        public GoogleMusicLoginTask(GooglePlayActivity activity) {
        	this.act2 = activity;
		}
        @Override
        protected Boolean doInBackground(String... params) {
        	boolean success = false;
            try {
            	String token = GoogleAuthUtil.getToken(mActivity,params[0], "sj");
            	//GoogleAuthUtil.invalidateToken(mActivity, token);
  	        
  	          Log.i(TAG, "TOKE MOTHERFUCKER: \n"+ token);
  	          if (!TextUtils.isEmpty(token)) {
                GoogleMusicApi.createInstance(mActivity);
                success = GoogleMusicApi.login(mActivity, token);
                Log.i(TAG, String.valueOf(success));
                gPlayList = GoogleMusicApi.getAllSongs(mActivity);	
                Log.i(TAG, String.valueOf(gPlayList.size()));
                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor writer = preferences.edit();
				writer.putString("Google_token", token);
				writer.putString("Google_email", params[0]);
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
				e.printStackTrace();
			} catch (URISyntaxException e) {
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
}
