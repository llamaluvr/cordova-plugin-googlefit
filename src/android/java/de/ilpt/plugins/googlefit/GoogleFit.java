
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

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.StringWriter;
import java.io.PrintWriter;

public class GoogleFit extends CordovaPlugin {

  private static final String TAG = "GoogleFit";

  private static final int REQUEST_OAUTH = 1;

  private GoogleApiClient googleApiClient;

  public boolean execute(String action, JSONArray args, final CallbackContext callback) throws JSONException {

    Log.i(TAG, "Will execute ffff action \"" + action + "\" with arguments " + args);


    switch (action) {
      case "connect":
        connect(callback);
        return true;
      case "disconnect":
        disconnect(callback);
        return true;
      case "isConnected":
        isConnected(callback);
        return true;
      case "disable":
        disable(callback);
        return true;
      case "query":
        query(callback);
        return true;
      case "available":
        available(callback);
        return true;
      case "checkAuthStatus":
        checkAuthStatus(callback);
        return true;
      case "requestAuthorization":
        requestAuthorization(callback);
        return true;
      case "readDateOfBirth":
        readDateOfBirth(callback);
        return true;
      case "readGender":
        readGender(callback);
        return true;
      case "readWeight":
        readWeight(callback);
        return true;
      case "saveWeight":
        saveWeight(callback);
        return true;
      case "readHeight":
        readHeight(callback);
        return true;
      case "saveHeight":
        saveHeight(callback);
        return true;

      //
      // WORKOUTS
      // 
      case "saveWorkout":
        saveWorkout(args, callback);
        return true;

      case "getStepsLastWeek":
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            getStepsLastWeek(callback);
          }
        });
        return true;
      default:
        Log.w(TAG, "Could not execute unknown action \"" + action + "\"!");
        return false;
    }
  }


  protected void isConnected(final CallbackContext callback) {
    Log.i(TAG, "isConnected");
    
    try {
      boolean result = (googleApiClient != null && googleApiClient.isConnected());

      if(result) {
        Log.i(TAG, "Already connected successfully.");
      } else {
        Log.i(TAG, "Not connected.");
      }

      JSONObject json = new JSONObject();
      json.put("result", result);

      callback.success(json);
    } catch (Exception e) {
      callback.error("Error checking connection status.");
    }
    
  }


  protected void connect(final CallbackContext callback) {
    Log.i(TAG, "Connect ! ");

    if (googleApiClient != null && googleApiClient.isConnecting()) {
      // TODO
    }

    if (googleApiClient != null && googleApiClient.isConnected()) {
      Log.i(TAG, "Already connected successfully.");
      callback.success();
      return;
    }

    if (googleApiClient == null) {
      googleApiClient = new GoogleApiClient.Builder(getActivity())
        .useDefaultAccount()
        .addApi(Fitness.HISTORY_API)
        .addApi(Fitness.SESSIONS_API)
        .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
        .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
        .build();
    }

    googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
      @Override
      public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected successfully. Bundle: " + bundle);
        callback.success();
      }

      @Override
      public void onConnectionSuspended(int statusCode) {
        Log.i(TAG, "Connection suspended.");
        callback.error(Connection.getStatusString(statusCode));
      }
    });

    googleApiClient.registerConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
      @Override
      public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: " + connectionResult);

        if (connectionResult.hasResolution()) {
          try {
            Log.i(TAG, "Start oauth login...");

            cordova.setActivityResultCallback(GoogleFit.this);
            connectionResult.startResolutionForResult(getActivity(), REQUEST_OAUTH);

          } catch (IntentSender.SendIntentException e) {
            Log.i(TAG, "OAuth login failed", e);

            callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
          }
        } else {
          // Show the localized error dialog
          Log.i(TAG, "Show error dialog!");

          GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();

          callback.error(Connection.getStatusString(connectionResult.getErrorCode()));
        }
      }
    });

    Log.i(TAG, "Will connect...");
    googleApiClient.connect();
  }


  protected void disconnect(final CallbackContext callback) {
    Log.i(TAG, "Disconnect ! ");

    if (googleApiClient != null) {


      googleApiClient.disconnect();
      callback.success();

    } else {
      callback.success();
    }
  }

  protected void disable(final CallbackContext callback) {
    PendingResult<Status> pendingResult = Fitness.ConfigApi.disableFit(googleApiClient);

    handleStatus(pendingResult, callback);
  }

  protected void query(final CallbackContext callback) {
    Calendar cal = Calendar.getInstance();
    Date now = new Date();
    cal.setTime(now);
    long endTime = cal.getTimeInMillis();
    cal.add(Calendar.WEEK_OF_YEAR, -1);
    long startTime = cal.getTimeInMillis();

    SimpleDateFormat dateFormat = new SimpleDateFormat();
    Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
    Log.i(TAG, "Range End: " + dateFormat.format(endTime));

    DataReadRequest readRequest = new DataReadRequest.Builder()
            // The data request can specify multiple data types to return, effectively
            // combining multiple data queries into one call.
            // In this example, it's very unlikely that the request is for several hundred
            // datapoints each consisting of a few steps and a timestamp.  The more likely
            // scenario is wanting to see how many steps were walked per day, for 7 days.
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                    // bucketByTime allows for a time span, whereas bucketBySession would allow
                    // bucketing by "sessions", which would need to be defined in code.
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build();

    Log.i(TAG, "Read data: " + readRequest);

    PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(googleApiClient, readRequest);

    pendingResult.setResultCallback(new ResultCallback<DataReadResult>() {
      @Override
      public void onResult(DataReadResult result) {
        Log.i(TAG, "Result: " + result);

        if (result.getStatus().isSuccess()) {
          JSONArray array = new JSONArray();

          for (DataSet dataSet : result.getDataSets()) {
            String dataType = dataSet.getDataType().getName();
            array.put(dataType);
          }

          callback.success(array);
        } else {
          callback.error(result.getStatus().getStatusCode() + " " + result.getStatus().getStatusMessage());
        }


      }
    }, 10, TimeUnit.SECONDS);
  }

  protected void available(final CallbackContext callback) {

  }

  protected void checkAuthStatus(final CallbackContext callback) {

  }

  protected void requestAuthorization(final CallbackContext callback) {

  }

  protected void readDateOfBirth(final CallbackContext callback) {

  }

  protected void readGender(final CallbackContext callback) {

  }

  //
  // WEIGHT AND HEIGHT
  //

  protected void readWeight(final CallbackContext callback) {
    Calendar cal = Calendar.getInstance();
    Date now = new Date();
    cal.setTime(now);
    long endTime = cal.getTimeInMillis();
    long startTime = 1;

    SimpleDateFormat dateFormat = new SimpleDateFormat();
    Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
    Log.i(TAG, "Range End: " + dateFormat.format(endTime));


    DataReadRequest request = new DataReadRequest.Builder()
            .read(DataType.TYPE_WEIGHT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .setLimit(1)
            .build();
    PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(googleApiClient, request);

    handleLatestDataReadResult(pendingResult, DataType.TYPE_WEIGHT, Field.FIELD_WEIGHT, callback);
  }

  protected void saveWeight(final CallbackContext callback) {

  }

  protected void readHeight(final CallbackContext callback) {
    long startTime = 1;
    long endTime = System.currentTimeMillis();

    SimpleDateFormat dateFormat = new SimpleDateFormat();
    Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
    Log.i(TAG, "Range End: " + dateFormat.format(endTime));

    DataReadRequest request = new DataReadRequest.Builder()
            .read(DataType.TYPE_HEIGHT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS) // all time
            .setLimit(1) // latest result only
            .build();
    PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(googleApiClient, request);

    handleLatestDataReadResult(pendingResult, DataType.TYPE_HEIGHT, Field.FIELD_HEIGHT, callback);
  }


  //
  // WORKOUTS
  //

  protected void saveWorkout(final JSONArray args, final CallbackContext callback) {
    WorkoutWriter workoutWriter = new WorkoutWriter(this.googleApiClient);
    workoutWriter.saveSimpleWorkout(args, callback);
  }

  protected void getStepsLastWeek(final CallbackContext callback) {


    // Begin by creating the query.
    DataReadRequest readRequest = queryFitnessData();

    // [START read_dataset]
    // Invoke the History API to fetch the data with the query and await the result of
    // the read request.
    DataReadResult dataReadResult =
            Fitness.HistoryApi.readData(googleApiClient, readRequest).await(1, TimeUnit.MINUTES);
    // [END read_dataset]

    // For the sake of the sample, we'll print the data so we can see what we just added.
    // In general, logging fitness information should be avoided for privacy reasons.
    printData(dataReadResult, callback);

  }

  /**
   * Return a {@link DataReadRequest} for all step count changes in the past week.
   */
  private DataReadRequest queryFitnessData() {
    // [START build_read_data_request]
    // Setting a start and end date using a range of 1 week before this moment.
    Calendar cal = Calendar.getInstance();
    Date now = new Date();

    cal.setTime(now);
    cal.add(Calendar.DATE, 1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    long endTime = cal.getTimeInMillis();

    cal.add(Calendar.WEEK_OF_YEAR, -1);
    long startTime = cal.getTimeInMillis();

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
    Log.i(TAG, "Range End: " + dateFormat.format(endTime));

    DataReadRequest readRequest = new DataReadRequest.Builder()
            // The data request can specify multiple data types to return, effectively
            // combining multiple data queries into one call.
            // In this example, it's very unlikely that the request is for several hundred
            // datapoints each consisting of a few steps and a timestamp.  The more likely
            // scenario is wanting to see how many steps were walked per day, for 7 days.
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                    // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                    // bucketByTime allows for a time span, whereas bucketBySession would allow
                    // bucketing by "sessions", which would need to be defined in code.
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build();
    // [END build_read_data_request]

    return readRequest;
  }

  /**
   * Log a record of the query result. It's possible to get more constrained data sets by
   * specifying a data source or data type, but for demonstrative purposes here's how one would
   * dump all the data. In this sample, logging also prints to the device screen, so we can see
   * what the query returns, but your app should not log fitness information as a privacy
   * consideration. A better option would be to dump the data you receive to a local data
   * directory to avoid exposing it to other applications.
   */
  private void printData(DataReadResult dataReadResult, CallbackContext callback) {
    // [START parse_read_data_result]
    // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
    // as buckets containing DataSets, instead of just DataSets.
    if (dataReadResult.getBuckets().size() > 0) {
      Log.i(TAG, "Number of returned buckets of DataSets is: "
              + dataReadResult.getBuckets().size());

      JSONArray jsonArray = new JSONArray();


      for (Bucket bucket : dataReadResult.getBuckets()) {
        List<DataSet> dataSets = bucket.getDataSets();
        for (DataSet dataSet : dataSets) {
          JSONObject aa = dumpDataSet(dataSet);
          jsonArray.put(aa);
        }


      }

      callback.success(jsonArray);

    } else if (dataReadResult.getDataSets().size() > 0) {
      Log.i(TAG, "Number of returned DataSets is: "
              + dataReadResult.getDataSets().size());
      for (DataSet dataSet : dataReadResult.getDataSets()) {
        dumpDataSet(dataSet);
      }
    }
    // [END parse_read_data_result]
  }

  private JSONObject dumpDataSet(DataSet dataSet) {
    Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    JSONObject json = new JSONObject();


    try {
      for (DataPoint dp : dataSet.getDataPoints()) {
        // Log.i(TAG, "Data point:");


        json.put("type", dp.getDataType().getName());
        json.put("start", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
        json.put("end", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

        // Log.i(TAG, "\tType: " + dp.getDataType().getName());
        // Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
        // Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

        for (Field field : dp.getDataType().getFields()) {
          Log.i(TAG, "\tField: " + field.getName() +
                  " Value: " + dp.getValue(field));
          json.put(field.getName(), dp.getValue(field));

        }
      }


    } catch (JSONException e) {
      Log.i(TAG, "\r error: " + e.toString());

      return null;
    }


    return json;
  }

  protected void saveHeight(final CallbackContext callback) {

  }

  public void onConnected(Bundle connectionHint) {
    Log.w(TAG, "onConnected");
    // Connected to Google Fit Client.
    Fitness.SensorsApi.add(
            googleApiClient,
            new SensorRequest.Builder()
                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                    .build(),
            new OnDataPointListener() {
              @Override
              public void onDataPoint(DataPoint dataPoint) {

              }
            });
  }

  public void onConnectionSuspended(int cause) {
    Log.w(TAG, "onConnectionSuspended");
  }

  public void onConnectionFailed(ConnectionResult result) {
    Log.w(TAG, "onConnectionFailed");

    // Error while connecting. Try to resolve using the pending intent returned.
        /*
        if (result.getErrorCode() == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
        try {
        result.startResolutionForResult(this, REQUEST_OAUTH);
        } catch (IntentSender.SendIntentException e) {
        }
        }
        */
  }

  public void onDataPoint(DataPoint dataPoint) {
    Log.w(TAG, "onDataPoint");
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.w(TAG, "onActivityResult requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + data);

    if (requestCode == REQUEST_OAUTH && resultCode == Activity.RESULT_OK) {
      googleApiClient.connect(); // TODO: We need to call the previously success/failire callbacks here...
    }
  }

  protected void handleStatus(PendingResult<Status> pendingResult, final CallbackContext callback) {
    pendingResult.setResultCallback(new ResultCallback<Status>() {
      @Override
      public void onResult(Status status) {
        if (status.isSuccess()) {
          Log.i(TAG, "Action handled successfully!");
          callback.success(Connection.getStatusString(ConnectionResult.SUCCESS));
        } else {
          Log.i(TAG, "Action failed: " + Connection.getStatusString(status.getStatusCode()));
          callback.error(Connection.getStatusString(status.getStatusCode()));
        }
      }
    });
  }

  protected void handleDataReadResult(PendingResult<DataReadResult> pendingResult, final CallbackContext callback) {
    pendingResult.setResultCallback(new ResultCallback<DataReadResult>() {
      @Override
      public void onResult(DataReadResult dataReadResult) {
        if (dataReadResult.getStatus().isSuccess()) {
          Log.i(TAG, "Read data was successfully!");

          try {


            List<DataSet> dataSets = dataReadResult.getDataSets();
            Log.i(TAG, "steppps: " + dataSets.toString());

            JSONObject json = new JSONObject();

            for (DataSet dataSet : dataSets) {
              dumpDataSet(dataSet);
              Log.i(TAG, "receive dataset: " + dataSet);
              Log.i(TAG, "receive dataset: " + dataSet.getDataSource() + " " + dataSet.getDataType() + " " + dataSet.getDataPoints());
              json.put(dataSet.getDataSource().toString(), "receive dataset: " + " " + dataSet.getDataType().toString() + " " + dataSet.getDataPoints().toString());

            }

            callback.success(json);

          } catch (JSONException e) {
            callback.error(e.toString());
          }


        } else {
          Log.i(TAG, "Read data failed: " + Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
          callback.error(Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
        }
      }
    });
  }


  protected void handleLatestDataReadResult(PendingResult<DataReadResult> pendingResult,
                                            final DataType dataType,
                                            final Field field,
                                            final CallbackContext callback) {
    pendingResult.setResultCallback(new ResultCallback<DataReadResult>() {
      @Override
      public void onResult(DataReadResult dataReadResult) {
        if (dataReadResult.getStatus().isSuccess()) {
          Log.i(TAG, "Read data was successfully!");

          List<DataSet> dataSets = dataReadResult.getDataSets();
          for (DataSet dataSet : dataSets) {
            Log.i(TAG, "receive dataset: " + dataSet);
            Log.i(TAG, "  data source: " + dataSet.getDataSource());
            Log.i(TAG, "  data type: " + dataSet.getDataType() + " " + dataSet.getDataPoints());
            Log.i(TAG, "  data points: " + dataSet.getDataPoints());
          }

          List<DataPoint> dataPoints = dataReadResult.getDataSet(dataType).getDataPoints();

          if (dataPoints.isEmpty()) {
            callback.error("NO_DATA_POINT");
            return;
          }

          DataPoint latestDataPoint = dataPoints.get(dataPoints.size() - 1);

          try {
            JSONObject json = new JSONObject();

            json.put("value", latestDataPoint.getValue(field));
            json.put("dataType", latestDataPoint.getDataType().getName());
            json.put("endTime", latestDataPoint.getEndTime(TimeUnit.MILLISECONDS));
            json.put("startTime", latestDataPoint.getStartTime(TimeUnit.MILLISECONDS));
            json.put("timestamp", latestDataPoint.getTimestamp(TimeUnit.MILLISECONDS));
            json.put("versionCode", latestDataPoint.getVersionCode());

            callback.success(json);

          } catch (JSONException e) {
            callback.error(e.toString());
          }

        } else {
          Log.i(TAG, "Read data failed: " + Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
          callback.error(Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
        }
      }
    });
  }


  private Activity getActivity() {
    return cordova.getActivity();
  }

  private Context getApplicationContext() {
    return cordova.getActivity().getApplicationContext();
  }
}
