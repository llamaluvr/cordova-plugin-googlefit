package de.ilpt.plugins.googlefit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.*;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.*;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

public class GoogleFitApiClientConnector {

  private static final int REQUEST_OAUTH = 1;

	private Loggable logger;

  private GoogleApiClient googleApiClient;

  private GoogleFit googleFitCordovaPlugin;

  private GoogleApiClient.ConnectionCallbacks connectionCallbacks;

  private GoogleApiClient.OnConnectionFailedListener regularConnectionFailedListener;

  private GoogleApiClient.OnConnectionFailedListener authenticationAttemptConnectionFailedListener;

	public GoogleFitApiClientConnector(GoogleApiClient googleApiClient, Loggable logger, GoogleFit googleFitCordovaPlugin) {
    this.googleApiClient = googleApiClient;
		this.logger = logger;
    this.googleFitCordovaPlugin = googleFitCordovaPlugin;
	}

  private void rebuildConnectionListeners(final CallbackContext callback) {

    final Loggable logger= this.logger;

    final GoogleFit googleFitCordovaPlugin = this.googleFitCordovaPlugin;

    this.connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
      @Override
      public void onConnected(Bundle bundle) {
        logger.log("Connected successfully. Bundle: " + bundle);
        callback.success();
      }

      @Override
      public void onConnectionSuspended(int statusCode) {
        logger.log("Connection suspended.");
        callback.error(Connection.getStatusString(statusCode));
      }
    };

    this.regularConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
      @Override
      public void onConnectionFailed(ConnectionResult connectionResult) {
        logger.log("Connection failed: " + connectionResult);
        callback.error("not connected");
      }
    };

    this.authenticationAttemptConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
      @Override
      public void onConnectionFailed(ConnectionResult connectionResult) {
        logger.log("Connection failed: " + connectionResult);

        if (connectionResult.hasResolution()) {
          try {
            logger.log("Start oauth login...");

            googleFitCordovaPlugin.cordova.setActivityResultCallback(googleFitCordovaPlugin);
            connectionResult.startResolutionForResult(getActivity(), REQUEST_OAUTH);

          } catch (IntentSender.SendIntentException e) {
            logger.log("OAuth login failed", e);

            callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
          }
        } else {
          // Show the localized error dialog
          logger.log("Show error dialog!");

          GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();

          callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
        }
      }
    };
  }

	public void isConnected(final CallbackContext callback) {
		logger.log("isConnected");

		try {
			boolean result = (googleApiClient != null && googleApiClient.isConnected());

			if(result) {
				logger.log("Already initialized successfully.");
			} else {
				logger.log("Not connected.");
			}

			JSONObject json = new JSONObject();
			json.put("result", result);

			callback.success(json);
		} catch (Exception e) {
			callback.error("Error checking connection status.");
		}

	}

	public void connect(final CallbackContext callback) {

  	final Loggable logger = this.logger; //so inline classes can access

    logger.log("isConnected");

    if (googleApiClient != null && googleApiClient.isConnected()) {
      logger.log("Already connected successfully.");
      callback.success();
      return;
    }

    if (googleApiClient != null) {
      if(this.connectionCallbacks != null) {
        googleApiClient.unregisterConnectionCallbacks(this.connectionCallbacks);
      }
      if(this.regularConnectionFailedListener != null) {
        googleApiClient.unregisterConnectionFailedListener(this.regularConnectionFailedListener);
      }
      if(this.authenticationAttemptConnectionFailedListener != null) {
        googleApiClient.unregisterConnectionFailedListener(this.authenticationAttemptConnectionFailedListener);
      }
      rebuildConnectionListeners(callback);
      googleApiClient.registerConnectionCallbacks(this.connectionCallbacks);
      googleApiClient.registerConnectionFailedListener(this.regularConnectionFailedListener);

      logger.log("Will connect...");
      googleApiClient.connect();
    } else {
      logger.log("Will not connect because client is not instantiated");
      callback.error("Client not instantiated.");
    }
	}


	public void connectWithAuthentication(final CallbackContext callback) {

  	final Loggable logger = this.logger; //so inline classes can access

    logger.log("Connect ! ");

    if (googleApiClient != null && googleApiClient.isConnecting()) {
      logger.log("Connection already in progress.");
      callback.error("Connection already in progress."); //not really sure if this is an error, but surely a client would want to know this happened.
      return;
    }

    if (googleApiClient != null && googleApiClient.isConnected()) {
      logger.log("Already connected successfully.");
      callback.success();
      return;
    }

    if (googleApiClient != null) {
      if(this.connectionCallbacks != null) {
        googleApiClient.unregisterConnectionCallbacks(this.connectionCallbacks);
      }
      if(this.regularConnectionFailedListener != null) {
        googleApiClient.unregisterConnectionFailedListener(this.regularConnectionFailedListener);
      }
      if(this.authenticationAttemptConnectionFailedListener != null) {
        googleApiClient.unregisterConnectionFailedListener(this.authenticationAttemptConnectionFailedListener);
      }
      rebuildConnectionListeners(callback);
      googleApiClient.registerConnectionCallbacks(this.connectionCallbacks);
      googleApiClient.registerConnectionFailedListener(this.authenticationAttemptConnectionFailedListener);

      logger.log("Will connect...");
      googleApiClient.connect();
    } else {
      logger.log("Will not connect because client is not instantiated");
      callback.error("Client not instantiated.");
    }    
	}

	public void disconnect(final CallbackContext callback) {
    logger.log("Disconnect ! ");

    if (googleApiClient != null) {

      googleApiClient.disconnect();
      callback.success();

    } else {
      callback.success();
    }
	}

  private Activity getActivity() {
    return googleFitCordovaPlugin.cordova.getActivity();
  }

  private Context getApplicationContext() {
    return googleFitCordovaPlugin.cordova.getActivity().getApplicationContext();
  }
}