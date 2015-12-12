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
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.data.Session;

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

public class WorkoutWriter {

	private static final String TAG = "GoogleFit";

	private GoogleApiClient googleApiClient;

	public WorkoutWriter(GoogleApiClient apiClient) {
		this.googleApiClient = apiClient;
	}

	//Saves a workout with an activity, start time, end time, name, description, and a single segment that encompasses the entire start/ end time.
	public void saveSimpleWorkout(final JSONArray args, final CallbackContext callback) {

		JSONObject props;
		String name;
		String description;
		String uniqueId;
		String activity;
		int startTimeInMilliseconds;
		int endTimeInMilliseconds;

		//get values
		try {
			props = args.getJSONObject(0);
			name = props.getString("description");
			description = props.getString("name");
			uniqueId = props.getString("uniqueIdentifier");
			activity = props.getString("activity");
			startTimeInMilliseconds = props.getInt("startTime");
			endTimeInMilliseconds = props.getInt("endTime");
		} catch (JSONException e) {
			String errorMessage = getExceptionMessage(e);
			Log.i(TAG, errorMessage);
			callback.error(errorMessage);
			return;
		}

		DataSet activityDataSet = buildSingleSegmentActivityDataSet(name, activity, startTimeInMilliseconds, endTimeInMilliseconds);

		SessionInsertRequest insertRequest = buildSessionInsertRequest(name, description, uniqueId, activity, startTimeInMilliseconds, endTimeInMilliseconds, activityDataSet);

		try {
			insertAndVerifySession(insertRequest);
			callback.success();
		} catch (Exception e) {
			String errorMessage = getExceptionMessage(e);
			Log.i(TAG, errorMessage);
         	callback.error(errorMessage);
		}		
	}

	private void insertAndVerifySession(SessionInsertRequest insertRequest) throws WorkoutWriterException {
        // [START insert_session]
        Log.i(TAG, "Inserting the session in the History API");
        com.google.android.gms.common.api.Status insertStatus =
                Fitness.SessionsApi.insertSession(this.googleApiClient, insertRequest)
                        .await(1, TimeUnit.MINUTES);

        // Before querying the session, check to see if the insertion succeeded.
        if (!insertStatus.isSuccess()) {
        	String errorStatusMessage = "There was a problem inserting the session: " + insertStatus.getStatusMessage();
            Log.i(TAG, errorStatusMessage);
            throw new WorkoutWriterException(errorStatusMessage);
        }

        // At this point, the session has been inserted and can be read.
        //TODO: maybe take this out. I guess it will throw an exception if it can't be read, but it doesn't really serve any other purpose.
        //This was from Google's example here: https://github.com/googlesamples/android-fit/blob/master/BasicHistorySessions/app/src/main/java/com/google/android/gms/fit/samples/basichistorysessions/MainActivity.java
        Log.i(TAG, "Session insert was successful!");
        // [END insert_session]

        // Begin by creating the query.
        /*SessionReadRequest readRequest = readFitnessSession();

        // [START read_session]
        // Invoke the Sessions API to fetch the session with the query and wait for the result
        // of the read request.
        SessionReadResult sessionReadResult =
                Fitness.SessionsApi.readSession(mClient, readRequest)
                        .await(1, TimeUnit.MINUTES);

        // Get a list of the sessions that match the criteria to check the result.
        Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                + sessionReadResult.getSessions().size());
        for (Session session : sessionReadResult.getSessions()) {
            // Process the session
            dumpSession(session);

            // Process the data sets for this session
            List<DataSet> dataSets = sessionReadResult.getDataSet(session);
            for (DataSet dataSet : dataSets) {
                dumpDataSet(dataSet);
            }
        }*/
        // [END read_session]
	}

	private DataSet buildSingleSegmentActivityDataSet(String name, String activity, int startTimeInMilliseconds, int endTimeInMilliseconds) {
		DataSource activitySegmentDataSource = new DataSource.Builder()
                //.setAppPackageName(Context.getApplicationContext())
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setName(name + "-activity segments")
                .setType(DataSource.TYPE_RAW)
                .build();
        DataSet activitySegments = DataSet.create(activitySegmentDataSource);

        DataPoint activityDataPoint = activitySegments.createDataPoint()
                .setTimeInterval(startTimeInMilliseconds, endTimeInMilliseconds, TimeUnit.MILLISECONDS);
        activityDataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(activity);
        activitySegments.add(activityDataPoint);

        return activitySegments;
	}

	private SessionInsertRequest buildSessionInsertRequest(String name, String description, String uniqueId, String activity, int startTimeInMilliseconds, int endTimeInMilliseconds, DataSet activityDataSet) {
		// Create a session with metadata about the activity.
        Session session = new Session.Builder()
                .setName(name)
                .setDescription(description)
                .setIdentifier(uniqueId)
                .setActivity(activity)
                .setStartTime(startTimeInMilliseconds, TimeUnit.MILLISECONDS)
                .setEndTime(endTimeInMilliseconds, TimeUnit.MILLISECONDS)
                .build();

        // Build a session insert request
        SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                .setSession(session)
                .addDataSet(activityDataSet)
                .build();

        return insertRequest;
	}

	//logging

	/*private void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        for (DataPoint dp : dataSet.getDataPoints()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    private void dumpSession(Session session) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    } */

    //exception handling

    private String getExceptionMessage(Exception e) {
    	StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionDetails = sw.toString();
		return exceptionDetails;
    }
}