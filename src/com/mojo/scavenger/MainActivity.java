package com.mojo.scavenger;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;

import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener,
OnAddGeofencesResultListener {

	// Global constants
	/*
	 * Define a request code to send to Google Play services
	 * This code is returned in Activity.onActivityResult
	 */
	private final static int
	CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;

	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;

	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL =
			MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;

	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL =
			MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
	
	// Defines the allowable request types.
    public enum REQUEST_TYPE {ADD};

	// Define an object that holds accuracy and frequency parameters
	LocationRequest mLocationRequest;    
	LocationClient mLocationClient;
	
	// Determine if a request is in progress
	private boolean mInProgress = false;
    private REQUEST_TYPE mRequestType;

	// Current location
	Location currentLocation;

	// List of geofence items
	ArrayList<SimpleGeofence> fencelist = new ArrayList<SimpleGeofence>();

	// Local storage of geofence items
	SimpleGeofenceStore fencelist_store;

	// Status fields
	boolean location_avail = false;

	// GUI components
	TextView tb_loc;
	ListView fencelistview;
	Button   btn_mark;

	// Data adapters
	ArrayAdapter<SimpleGeofence> fencelist_adapter;

	// Other globals
	Random rand = new Random();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tb_loc = (TextView) findViewById(R.id.textbox_location);
		fencelistview = (ListView) findViewById(R.id.fencelist);
		btn_mark = (Button) findViewById(R.id.button_mark);

		// Check for google play services
		location_avail = servicesConnected();
		if (location_avail) {
//
//			// Create the LocationRequest object
//			mLocationRequest = LocationRequest.create();
//
//			// Use high accuracy
//			mLocationRequest.setPriority(
//					LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//			// Set the update interval to 5 seconds
//			mLocationRequest.setInterval(UPDATE_INTERVAL);
//
//			// Set the fastest update interval to 1 second
//			mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
//
//			mLocationClient = new LocationClient(this, this, this);
		} else {
			// Location data is unavailable
			tb_loc.setText("Location: Unavailable");
		}

		// Connect fencelist to view using adapter
		fencelist_adapter = new ArrayAdapter<SimpleGeofence>(this,
				R.layout.simple_list_entry_1, fencelist);

		fencelistview.setAdapter(fencelist_adapter);
		fencelistview.setOnItemClickListener(mMessageClickedHandler);

	}

	@Override
	protected void onStart() {
		super.onStart();
		//if (location_avail) {
		//	mLocationClient.connect();
		//}

		fencelist_store = new SimpleGeofenceStore(this);

		// Restore all geofences from non-vol mem
		Set<String> _fencelist_ids = fencelist_store.getStoredIDs();

		if (_fencelist_ids != null) {
			ArrayList<String> fencelist_ids = new ArrayList<String>(_fencelist_ids);

			for (int i = 0; i < fencelist_ids.size(); i++) {
				SimpleGeofence sgf = fencelist_store.getGeofence(fencelist_ids.get(i));
				fencelist.add(sgf);
			}
			fencelist_adapter.notifyDataSetChanged();
		}
	}

	/*
	 * Called when the Activity is no longer visible at all.
	 * Stop updates and disconnect.
	 */
	@Override
	protected void onStop() {
		//if (location_avail) {
		//	// If the client is connected
		//	if (mLocationClient.isConnected()) {
		//		/*
		//		 * Remove location updates for a listener.
		//		 * The current Activity is the listener, so
		//		 * the argument is "this".
		//		 */
		//		mLocationClient.removeLocationUpdates(this);
		//	}
		//	/*
		//	 * After disconnect() is called, the client is
		//	 * considered "dead".
		//	 */
		//	mLocationClient.disconnect();
		//}

		// Store all geofences to non-vol mem
		for (int i = 0; i < fencelist.size(); i++) {
			SimpleGeofence sgf = fencelist.get(i);
			fencelist_store.putGeofence(sgf.getId(), sgf);
		}

		super.onStop();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;
		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}
		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}
		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	/*
	 * Handle results returned to the FragmentActivity
	 * by Google Play services
	 */
	@Override
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST :
			/*
			 * If the result code is Activity.RESULT_OK, try
			 * to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK :
				/*
				 * Try the request again
				 */
				break;
			}
		}
	}

	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode =
				GooglePlayServicesUtil.
				isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates",
					"Google Play services is available.");
			// Continue
			return true;

			// Google Play services was not available for some reason
		} else {
			//return false;
			// Get the error code
			//int errorCode = result.getErrorCode();
			// Get the error dialog from Google Play services
			Log.d("Location Updates", "Google Play Services are not available.");
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					resultCode,
					this,
					CONNECTION_FAILURE_RESOLUTION_REQUEST);
			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment =
						new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(
						getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}

	/*
	 * Called by Location Services if the attempt to
	 * Location Services fails.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 */
		if (result.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				result.startResolutionForResult(
						this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error.
			 */
			Toast.makeText(this, Integer.toString(result.getErrorCode()), Toast.LENGTH_SHORT).show();
		}
	}

	/*
	 * Called by Location Services when the request to connect the
	 * client finishes successfully. At this point, you can
	 * request the current location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		// Display the connection status
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	/*
	 * Called by Location Services if the connection to the
	 * location client drops because of an error.
	 */
	@Override
	public void onDisconnected() {
		// Display the connection status
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();		
	}

	// Define the callback method that receives location updates
	@Override
	public void onLocationChanged(Location location) {
		currentLocation = location;

		// Report to the UI that the location was updated
		String msg = String.format("%.2f, %.2f",
				location.getLatitude(), location.getLongitude());
		
		tb_loc.setText("Location: " + msg);

		Log.d("Location", "Location update: " + msg);
		//Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	// Called when "Mark!" button is clicked
	// Adds a geofence around the current location to fencelist
	public void onMarkButtonClicked(View v) {
		//String msgId = Integer.toString(rand.nextInt(1000));
		String msgId = String.format("%d", rand.nextInt(999999999));
		
		//Location location = 
		
		fencelist.add(new SimpleGeofence(
				"test." + Integer.toString(rand.nextInt()),
				currentLocation.getLatitude(),
				currentLocation.getLongitude(),
				100, // radius in meters
				Geofence.NEVER_EXPIRE,
				Geofence.GEOFENCE_TRANSITION_ENTER,
				msgId));

		// Alert the list of the new addition
		fencelist_adapter.notifyDataSetChanged();
		
		// Register this geofence with location services
		
	}
	
	/*
     * Create a PendingIntent that triggers an IntentService in your
     * app when a geofence transition occurs.
     */
    private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent
        Intent intent = new Intent(this,
                ReceiveTransitionsIntentService.class);
        /*
         * Return the PendingIntent
         */
        return PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }


	// Callback for clicked items in list
	// Removes geofence from list
	private OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {
		public void onItemClick(AdapterView parent, View v, int position, long id) {
			SimpleGeofence sgf = fencelist_adapter.getItem(position);
			
			// Popup the associated message id
			String msgId = sgf.getMsgId();
			Toast.makeText(parent.getContext(), msgId, Toast.LENGTH_SHORT).show();

			// Remove this fencelist from LocationServices
			//TODO: Do this

			// Remove from storage
			fencelist_store.clearGeofence(sgf.getId());

			// Remove from list
			fencelist_adapter.remove(sgf);	    	
			fencelist_adapter.notifyDataSetChanged();
		}
	};

	@Override
	public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * Start a request for geofence monitoring by calling
     * LocationClient.connect().
     */
    public void addGeofences() {
        // Start a request to add geofences
        mRequestType = REQUEST_TYPE.ADD;
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the proper request
         * can be restarted.
         */
        if (!servicesConnected()) {
            return;
        }
        /*
         * Create a new location client object. Since the current
         * activity class implements ConnectionCallbacks and
         * OnConnectionFailedListener, pass the current activity object
         * as the listener for both parameters
         */
        
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is underway
            mInProgress = true;
            // Request a connection from the client to Location Services
            mLocationClient.connect();
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
        
    }


}
