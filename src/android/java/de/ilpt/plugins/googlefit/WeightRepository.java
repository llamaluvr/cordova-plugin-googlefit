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

}