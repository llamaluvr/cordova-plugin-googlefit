package de.ilpt.plugins.googlefit;

import android.util.Log;

public class AndroidLogger implements Loggable {

	private String tag;

	public AndroidLogger(String tag) {
		this.tag = tag;
	}

	public void log(String message) {
		Log.i(this.tag, message);
	}
}