package com.apps.isatimur.lookatme;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.isatimur.lookatme.utils.LUtils;
import com.apps.isatimur.lookatme.widget.CheckableFrameLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    public static final int UPDATE_INTERVAL_MILLIS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_INTERVAL_MILLS = UPDATE_INTERVAL_MILLIS / 2;

    public static final String TAG = "LookAtME";
    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private GoogleApiClient mGoogleApiClient;
    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    private TextView mLastUpdateTimeTextView;
    private boolean mLocationIsTracking;
    private LocationRequest mLocationRequest;
    private LUtils mLUtils;
    private CheckableFrameLayout mTrackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatitudeTextView = (TextView) findViewById(R.id.mLatitudeId);
        mLongitudeTextView = (TextView) findViewById(R.id.mLongitudeId);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.mLastUpdateText);

        mLocationIsTracking = false;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        ;

        mLUtils = LUtils.getInstance(this);
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        //showSettingsAlert();

        buildGoogleApiClient();

        mTrackButton = (CheckableFrameLayout) findViewById(R.id.add_schedule_button);

        mTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isTracking = !mLocationIsTracking;
                showPowerButton(isTracking, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mTrackButton.announceForAccessibility(isTracking ?
                            getString(R.string.session_details_a11y_session_added) :
                            getString(R.string.session_details_a11y_session_removed));
                }


            }
        });

        ViewCompat.setElevation(mTrackButton, 6.0f);

    }


    public LUtils getLUtils() {
        return mLUtils;
    }


    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building google api client");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_MILLIS);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL_MILLS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateUI() {
        if (mLocationIsTracking) {
            mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
            mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
            mLastUpdateTimeTextView.setText(String.valueOf(mLastUpdateTime));
        }
    }

    private void showPowerButton(boolean isTracking, boolean allowAnimate) {
        mLocationIsTracking = isTracking;

        mTrackButton.setChecked(mLocationIsTracking, allowAnimate);

        ImageView iconView = (ImageView) mTrackButton.findViewById(R.id.add_schedule_icon);
        getLUtils().setOrAnimatePlusCheckIcon(iconView, isTracking, allowAnimate);
        mTrackButton.setContentDescription(getString(isTracking
                ? R.string.stop_tracking
                : R.string.start_tracking));
        if (mLocationIsTracking) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mLocationIsTracking = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                showPowerButton(mLocationIsTracking, true);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = java.text.DateFormat.getTimeInstance().format(new Date());
        updateUI();
        Toast.makeText(this, "Location has been updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected ro GAPIClient");

        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = java.text.DateFormat.getTimeInstance().format(new Date());
            updateUI();
        }

        if (mLocationIsTracking) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && mLocationIsTracking) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mLocationIsTracking);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

}
