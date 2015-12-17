# Cordova plugin for [Google Fit](https://developers.google.com/fit/) (SDK)

## Purpose of the Fork

* Add functionality for saving workout sessions and weights (and maybe some other stuff) to the Google Fit plugin.

## Status of the Fork

* Added ability to save a simple workout (see below for API)

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

**Save simple workout:**

This saves a workout session to Google Fit that includes a name, description, unique id, activity type, and start time and end time. Does not save any activity segments. Pass an object that contains each of these values as shown below.

The activity property matches the string value of the [FitnessActivities constants](https://developers.google.com/android/reference/com/google/android/gms/fitness/FitnessActivities). Click on a constant under the aforementioned link to see its string value, which does not always (ever?) match the name of the constant.

	var workout = {
        	name: 'some title',
                description: 'some description',
                uniqueIdentifier: 'some unique id',
                activity: 'running',
                startTime: startTimeInMilliseconds,
                endTime: endTimeInMilliseconds
        };
	navigator.googlefit.saveWorkout(workout, function() {
		console.log('Workout saved to Google Fit');
	}, function(error) {
		console.warn('Save workout failed:', error);
	});

Calling saveWorkout() again with the same unique identifier will overwrite the workout. There's a bunch of cruft in the code right now related to saving an activity segment. I took it out for now because a) it seems pretty pointless for a single segment, and b) whenever I tried to overwrite a session with a shorter time interval than the previous version, it left a separate "session" in Google Fit for the remaining time. It's like all the sessions need to be cleared out first.

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



