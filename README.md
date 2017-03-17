# Cordova plugin for [Google Fit](https://developers.google.com/fit/) (SDK)

## Purpose of the Fork

* Add functionality for saving workout sessions and weights (and maybe some other stuff) to the Google Fit plugin.

## Status of the Fork

* Added ability to save a simple workout (see below for API)
* Changed isConnected() to just return a true/ false result, don't try to connect if not connected.
* Split connect() into connect() (for connecting when you've already signed into Google Fit) and connectWithAuthentication (to try to connect, but prompt for sign in with your Google Account if the app isn't already registered with Google Fit)
* Refactored connection code so events aren't registered more than once if connection is attempted more than once.
* Changed readWeight() to readMostRecentWeight() and readMostRecentWeightAsOfDate().
* Added saveWeight() (not tested yet)
* Added deleteWorkout()

## To-do

* test saving weight
* read a workout
* save a workout with some activity info inside of it

## Requirements

* Java JDK 1.7 / 7 or greater
* [Android SDK](http://developer.android.com)
* Installed Android Platform and Google Play Services (see next step) within the Android SDK
* A [Google Developer Console](https://console.developers.google.com/) project
  with enabled "Fitness API" and a client certificate.


For more information please read the [Google Fit Getting Started on Android](https://developers.google.com/fit/android/get-started) documention.


## Install plugin

	cordova plugin add https://github.com/llamaluvr/cordova-plugin-googlefit.git



## Status / API

**connect:**

Attempt to connect to Google Fit, but don't try to authenticate if the app was never registered with Google Fit for the user in the first place. So, if your app is not listed in the Connected Apps section on the Google Fit app, this will fail.

	navigator.googlefit.connect(function() {
		console.info('Connected successfully!');
	}, function(error) {
		console.warn('Connection failed:', error);
	});
	
**connectWithAuthentication:**

Attempt to connect to Google Fit. If connection fails with the "sign in required" status, the prompt will appear to login to Google Fit. When the user confirms that they would like to use Google Fit with the app, the app will appear as a connected app in the Google Fit app. Future calls to connect can be made with connect() at this point.

	navigator.googlefit.connectWithAuthentication(function() {
		console.info('Connected successfully!');
	}, function(error) {
		console.warn('Connection test failed: ', error);
	});
	
**isConnected:**

Check if the app is connected to Google Fit. Returns an object with a result property that is set to true or false.

	navigator.googlefit.isConnected(function(result) {
		console.info('Connection status: ' + result.result);
	}, function(error) {
		console.warn('Connection test failed: ', error);
	});

**Read most recent weight**

Reads the most recent weight stored in Google Fit.

Success callback contains an weight object. 

Error callback was called with an error string.

	navigator.googlefit.readMostRecentWeight({}, function(weight) {
		console.info('the weight in kg: ' + weight.value);
	}, function(error) {
		console.warn('Read weight failed:', error);
	});
	
**Save weight**

Saves a weight as of a given date.

	navigator.googlefit.saveWeight({weight: weightInKg, date: dateInMilliseconds}, function() {
		console.info('weight saved!');
	}, function(error) {
		console.warn('Save weight failed:', error);
	});
	
**Read most recent weight as of date**

Reads the most recent weight stored in Google Fit as of a particular date. Useful for finding out what weight was recorded at the time of a particular workout.

Success callback contains an weight object. 

Error callback was called with an error string.

	navigator.googlefit.readMostRecentWeightAsOfDate({date: someDateInMilliseconds}, function(weight) {
		console.info('the weight in kg: ' + weight.value);
	}, function(error) {
		console.warn('Read weight failed:', error);
	});


**Read height**

TODO: Options could be used to get the height for a specific date (from, to). Always just the latest result was returned.

Success callback contains an height object. DOES NOT MATCH HEALTHKIT API YET.

Error callback was called with an error string.

	navigator.googlefit.readHeight({}, function(height) {
		console.info(heiht);
	}, function(error) {
		console.warn('Read height failed:', error);
	});


See also [www/googlefit.js](https://github.com/ilovept/cordova-plugin-googlefit/blob/master/www/googlefit.js)

**Save simple workout**

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

**Delete workout**

Given a unique identifier, start time, and end time, if any workouts are found matching all of the criteria, then the workout is deleted.

Isn't a unique identifier enough to delete one workout? You'd think, but the Google Fit API wants a time interval on the query to read and the request to delete the workout. So there ya go. Think of the startTime and endTime as a range in which Google Fit should look for the uniquely-identified workout.

deleteWorkout() will only fail if there's some error. A request that does not match any workouts will have no effect, and will not return an error. Sorry about that. I'll probably change that.


	var workout = {
                uniqueIdentifier: 'some unique id',
                startTime: startTimeInMilliseconds,
                endTime: endTimeInMilliseconds
        };
	navigator.googlefit.deleteWorkout(workout, function() {
		console.log('Workout deleted from Google Fit');
	}, function(error) {
		console.warn('Delete workout failed:', error);
	});

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



