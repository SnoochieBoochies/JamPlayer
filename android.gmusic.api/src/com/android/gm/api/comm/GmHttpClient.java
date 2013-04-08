package com.android.gm.api.comm;

import org.apache.http.HttpEntity;

import android.content.Context;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

public class GmHttpClient extends SyncHttpClient {

	public GmHttpClient() {
		super();
	}
	String res;
    public String post(Context context, String url, HttpEntity entity, String contentType) {
    	String result="";
    	post(context, url, entity, contentType, new AsyncHttpResponseHandler() {
    		     @Override
    		     public void onSuccess(String response) {
    		         //Log.i("GMHTTPCLIENT",response);
    		         res = response;
    		     }
    		   
    	});
    	result = res;
    	return result;
    }

	@Override
	public String onRequestFailed(Throwable error, String content) {
		// TODO Auto-generated method stub
		return null;
	}
}
