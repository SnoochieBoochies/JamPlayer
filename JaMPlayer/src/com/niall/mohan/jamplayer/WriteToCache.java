package com.niall.mohan.jamplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import com.niall.mohan.jamplayer.adapters.JamSongs;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class WriteToCache {
	private static final String TAG = "JaM Music";
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	public File getAlbumStorageDir() {
	    // Get the directory for the app's public music directory.
		File file;
		if(isExternalStorageWritable()) {
			file = new File(Environment.getExternalStoragePublicDirectory(
		            Environment.DIRECTORY_MUSIC), TAG);
			Log.i("getAlbumStoageDir", file.getAbsolutePath());
			 if (!file.mkdirs()) {
			        Log.e("getAlbumStorageDir", "Directory not created");
			 }
			return file;
		} else {
			return null;
		}
	} 
	private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
	public Bitmap getArtwork(String path, long albumId, Context context) throws IOException {
		Bitmap art = null;
		WriteToCache cac = new WriteToCache();
		Log.i(TAG, String.valueOf(albumId));
		Log.i(TAG, path);
		File file = new File(cac.getAlbumStorageDir()+"/artwork.jpg");
		InputStream is = null;
		if(file.exists()) 
			file.delete();
		if(path.contains("http")) {
			try {
				URL url = new URL(path);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.connect();
				is = con.getInputStream();
				return BitmapFactory.decodeStream(is);
			} catch(IOException e) {
				e.printStackTrace();
			} finally {
				is.close();
			}
		} else if(!path.contains("http")) { //local file.
			ContentResolver cr = context.getContentResolver();
			Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
			if(uri != null) {	
				ParcelFileDescriptor fd = null;
	            try {
	                fd = cr.openFileDescriptor(uri, "r");
	                int sampleSize = 1;
	                BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
	                art = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor());
	                return art;
	            } catch(FileNotFoundException e) {
	            	//e.printStackTrace();
	            	return BitmapFactory.decodeResource(context.getResources(), R.drawable.dummy_album_art);
	            } finally {
	            	try {
	            		if(fd != null) {
	            			fd.close();
	            		}
	            	} catch(IOException e) {
	            		e.printStackTrace();
	            		return BitmapFactory.decodeResource(context.getResources(), R.drawable.dummy_album_art);
	            	}
	            }
			}
		} else {
			return BitmapFactory.decodeResource(context.getResources(), R.drawable.dummy_album_art);
		}
		return art;
	}
	public void writeArrayList(ArrayList<JamSongs> songs) {
		 try {
			 PrintWriter indices = new PrintWriter(new File(getAlbumStorageDir()
                    + "/arraylist"+ ".tmp"));
			 for(int i = 0; i < songs.size(); i++) {
				indices.println(songs.get(i).getTitle());
				indices.println(songs.get(i).getPath());
				indices.println(songs.get(i).getService());
				indices.println(songs.get(i).getAlbum());
				indices.println(songs.get(i).getDuration());
				indices.println(songs.get(i).getArtist());
				indices.println(String.valueOf(songs.get(i).getTrackNum()));
				indices.println(songs.get(i).getId());
				indices.println(songs.get(i).getArtwork());
				indices.println(String.valueOf(songs.get(i).getAlbumId()));
			 }
			 indices.close();

            // RENAME NEW FILE TO THAT OF THE PREVIOUS FILE

            // File myFile = new File("/sdcard/mysdfile.txt");
            File oldFile = new File(getAlbumStorageDir() + "/arraylist");

            File newFile = new File(getAlbumStorageDir() + "/arraylist"+ ".tmp");

            oldFile.delete();

            newFile.renameTo(oldFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

	}
	public ArrayList<JamSongs> getArrayList() {
		ArrayList<JamSongs> songs = new ArrayList<JamSongs>();
		String full = "";
		try {
			File readFile = new File(getAlbumStorageDir()+"/arraylist");
			FileInputStream is = new FileInputStream(readFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			Scanner in = new Scanner(new File(getAlbumStorageDir()+"/arraylist"));
			String rowData = "";
			String buffer = "";
			while(in.hasNext()) {
				songs.add(new JamSongs(in.nextLine(), in.nextLine(), in.nextLine(), in.nextLine(), in.nextLine(), in.nextLine(), Integer.valueOf(in.nextLine()), in.nextLine(),
						in.nextLine(), Long.valueOf(in.nextLine())));
			}
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return songs;
	}
}
