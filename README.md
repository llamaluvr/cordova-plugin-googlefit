# Cordova plugin for [Google Fit](https://developers.google.com/fit/) (SDK)



## Requirements

* Java JDK 1.7 / 7 or greater
* [Android SDK](http://developer.android.com)
* Installed Android Platform and Google Play Services (see next step) within the Android SDK
* A [Google Developer Console](https://console.developers.google.com/) project
  with enabled "Fitness API" and a client certificate.


For more information please read the [Google Fit Getting Started on Android](https://developers.google.com/fit/android/get-started) documention.


## Install plugin

	cordova plugin add https://github.com/dam1/cordova-plugin-googlefit.git



## Status / API

**Connect:**

Success callback was always called with 'OK'.

Error callback was called with an error string.

	navigator.googlefit.connect(function() {
		console.info('Connected successfully!');
	}, function() {
		console.warn('Connection failed:', error);
	});

**Read weight:**

TODO: Options could be used to get the weight for a specific date (from, to). Always just the latest result was returned.

Success callback contains an weight object. DOES NOT MATCH HEALTHKIT API YET.

Error callback was called with an error string.

	navigator.googlefit.readWeight({}, function(weight) {
		console.info(weight);
	}, function(error) {
		console.warn('Read weight failed:', error);
	});

**Read height:**

TODO: Options could be used to get the height for a specific date (from, to). Always just the latest result was returned.

Success callback contains an height object. DOES NOT MATCH HEALTHKIT API YET.

Error callback was called with an error string.

	navigator.googlefit.readHeight({}, function(height) {
		console.info(heiht);
	}, function(error) {
		console.warn('Read height failed:', error);
	});


See also [www/googlefit.js](https://github.com/ilovept/cordova-plugin-googlefit/blob/master/www/googlefit.js)



## Application client ID and certificate

1. Create a project within the [Google Developer Console](https://console.developers.google.com/)
2. Enable the "APIs → Auth" → "APIs" → "Fitness API"
3. Create a Client ID and submit your signing identity (fingerprint):

Create a SHA1 fingerprint of the certificate you use to sign your APK. For developers this
is, in most cases, automatically generated and stored in `~/.android/debug.keystore`.

	keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -list -v

Create a new "APs &amp; Auth" → "Credentials" → "OAuth - "Create new Client ID" and select
"Installed Application" → "Android". (Probably you must create a consent screen first..)



## Debug the plugin

This plugin will call your success or failure callbacks for any javascript function.
For debugging it uses the [Android Log](http://developer.android.com/tools/debugging/debugging-log.html)
command with the tag `GoogleFit` so you can filter all relevant debug information with
[logcat](http://developer.android.com/tools/help/logcat.html):

	adb logcat -s GoogleFit:d

Or, if you want debug Cordova too:

	adb logcat -s GoogleFit:d CordovaActivity:d CordovaApp:d CordovaWebViewImpl:d CordovaInterfaceImpl:d CordovaBridge:d PluginManager:d Config:d



