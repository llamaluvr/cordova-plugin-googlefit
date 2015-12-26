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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
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

public class WeightRepository {

	private static final String TAG = "GoogleFit";

	private GoogleApiClient googleApiClient;
	private Loggable logger;

	public WeightRepository(GoogleApiClient apiClient, Loggable logger) {
		this.googleApiClient = apiClient;
		this.logger = logger;
	}

	//Provides the most recent weight recorded given a date. Will look for all weights on or before that date, and return the one most recently recorded.
	public void readMostRecentWeightAsOfDate(final JSONArray args, final CallbackContext callback) {

		JSONObject props;
		long endTimeInMilliseconds;
		long startTimeInMilliseconds = 1;

		//get values
		try {
			props = args.getJSONObject(0);
			endTimeInMilliseconds = props.getLong("date");
		} catch (JSONException e) {
			String errorMessage = ExceptionMessageProvider.getExceptionMessage(e);
			this.logger.log(errorMessage);
			callback.error(errorMessage);
			return;
		}

	    SimpleDateFormat dateFormat = new SimpleDateFormat();
	    this.logger.log("Range Start: " + dateFormat.format(startTimeInMilliseconds));
	    this.logger.log("Range End: " + dateFormat.format(endTimeInMilliseconds));

	    readMostRecentWeightInRange(startTimeInMilliseconds, endTimeInMilliseconds, callback); 
	}

	//Provides the latest weight
	public void readMostRecentWeight(final CallbackContext callback) {
		Calendar cal = Calendar.getInstance();
	    Date now = new Date();
	    cal.setTime(now);
	    long endTime = cal.getTimeInMillis();
	    long startTime = 1;

	    SimpleDateFormat dateFormat = new SimpleDateFormat();
	    this.logger.log("Range Start: " + dateFormat.format(startTime));
	    this.logger.log("Range End: " + dateFormat.format(endTime));

	    readMostRecentWeightInRange(startTime, endTime, callback); 
	}

	//save weight from json of format: {date: dateInMilliseconds, weight: weightInKg}
	public void saveWeight(final JSONArray args, final CallbackContext callback) {

		JSONObject props;
		double weightInKg;
		long dateInMilliseconds;

		//get values
		try {
			props = args.getJSONObject(0);
			weightInKg = props.getDouble("weight");
			dateInMilliseconds = props.getLong("date");
		} catch (JSONException e) {
			String errorMessage = ExceptionMessageProvider.getExceptionMessage(e);
			this.logger.log(errorMessage);
			callback.error(errorMessage);
			return;
		}

    DataSet weightDataSet = createDataForRequest(
       DataType.TYPE_WEIGHT,    // for height, it would be DataType.TYPE_HEIGHT
       DataSource.TYPE_RAW,
       weightInKg,                  // weight in kgs
       dateInMilliseconds,          // start time
       dateInMilliseconds,          // end time
       TimeUnit.MILLISECONDS  
    );

    com.google.android.gms.common.api.Status insertStatus =
            Fitness.HistoryApi.insertData(this.googleApiClient, weightDataSet)
                    .await(1, TimeUnit.MINUTES);

    if (!insertStatus.isSuccess()) {
    	String errorStatusMessage = "There was a problem inserting the weight: " + insertStatus.getStatusMessage();
        this.logger.log(errorStatusMessage);
        callback.error(errorStatusMessage);
    }

    // At this point, the session has been inserted and can be read.
    //TODO: maybe take this out. I guess it will throw an exception if it can't be read, but it doesn't really serve any other purpose.
    //This was from Google's example here: https://github.com/googlesamples/android-fit/blob/master/BasicHistorySessions/app/src/main/java/com/google/android/gms/fit/samples/basichistorysessions/MainActivity.java
    this.logger.log("Weight insert was successful!");
	}

	private void readMostRecentWeightInRange(long startTimeInMilliseconds, long endTimeInMilliseconds, final CallbackContext callback) {
		DataReadRequest request = new DataReadRequest.Builder()
	            .read(DataType.TYPE_WEIGHT)
	            .setTimeRange(startTimeInMilliseconds, endTimeInMilliseconds, TimeUnit.MILLISECONDS)
	            .setLimit(1)
	            .build();
	    PendingResult<DataReadResult> pendingResult = Fitness.HistoryApi.readData(googleApiClient, request);

	    handleLatestDataReadResult(pendingResult, DataType.TYPE_WEIGHT, Field.FIELD_WEIGHT, callback);
	}

	private void handleLatestDataReadResult(PendingResult<DataReadResult> pendingResult,
                                            final DataType dataType,
                                            final Field field,
                                            final CallbackContext callback) {
		final Loggable logger = this.logger;

    pendingResult.setResultCallback(new ResultCallback<DataReadResult>() {
      @Override
      public void onResult(DataReadResult dataReadResult) {
        if (dataReadResult.getStatus().isSuccess()) {
          logger.log("Read data was successfully!");

          List<DataSet> dataSets = dataReadResult.getDataSets();
          for (DataSet dataSet : dataSets) {
            logger.log("receive dataset: " + dataSet);
            logger.log("  data source: " + dataSet.getDataSource());
            logger.log("  data type: " + dataSet.getDataType() + " " + dataSet.getDataPoints());
            logger.log("  data points: " + dataSet.getDataPoints());
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
          logger.log("Read data failed: " + Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
          callback.error(Connection.getStatusString(dataReadResult.getStatus().getStatusCode()));
        }
      }
    });
  }

  /**
 * This method creates a dataset object to be able to insert data in google fit
 * @param dataType DataType Fitness Data Type object
 * @param dataSourceType int Data Source Id. For example, DataSource.TYPE_RAW
 * @param values Object Values for the fitness data. They must be int or float
 * @param startTime long Time when the fitness activity started
 * @param endTime long Time when the fitness activity finished
 * @param timeUnit TimeUnit Time unit in which period is expressed
 * @return
 */
	private DataSet createDataForRequest(DataType dataType, int dataSourceType, Object values,
	                                     long startTime, long endTime, TimeUnit timeUnit) {
	    DataSource dataSource = new DataSource.Builder()
	            .setDataType(dataType)
	            .setType(dataSourceType)
	            .build();

	    DataSet dataSet = DataSet.create(dataSource);
	    DataPoint dataPoint = dataSet.createDataPoint().setTimeInterval(startTime, endTime, timeUnit);

	    if (values instanceof Integer) {
	        dataPoint = dataPoint.setIntValues((Integer)values);
	    } else {
	        dataPoint = dataPoint.setFloatValues((Float)values);
	    }

	    dataSet.add(dataPoint);

	    return dataSet;
	}

}