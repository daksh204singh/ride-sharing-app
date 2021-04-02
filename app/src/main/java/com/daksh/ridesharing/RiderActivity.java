package com.daksh.ridesharing;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;


public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;

	LocationManager locationManager;
	LocationListener locationListener;

	Button requestButton;
	boolean requestActive = false;
	boolean driverActive = false;

	Handler handler = new Handler();

	TextView infoTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rider);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		requestButton = findViewById(R.id.requestPickup);
		infoTextView = findViewById(R.id.infoTextView);

		ParseQuery<ParseObject> query = new ParseQuery<>("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.findInBackground(((objects, e) -> {
			if (e == null && objects.size() > 0) {
				requestActive = true;
				requestButton.setText(R.string.cancelPickupRequest);
				checkForUpdates();
			}
		}));
	}

	private void checkForUpdates() {
		Log.i("Driver Active", String.valueOf(driverActive));
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
		query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
		query.whereExists("driverUsername");

		query.findInBackground(((objects, e) -> {
			if (e == null && objects.size() > 0) {
				driverActive = true;
				ParseQuery<ParseUser> parseUserQuery = ParseUser.getQuery();
				parseUserQuery.whereEqualTo("username",
						objects.get(0).getString("driverUsername"));

				parseUserQuery.findInBackground((driverObjects, exception) -> {
					if (e == null && driverObjects.size() > 0) {
						ParseGeoPoint driverLocation =
								driverObjects.get(0).getParseGeoPoint("location");

						if (ContextCompat.checkSelfPermission(this,
								Manifest.permission.ACCESS_FINE_LOCATION)
								== PackageManager.PERMISSION_GRANTED) {
							Location lastKnownLocation =
									locationManager
											.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							if (lastKnownLocation != null) {
								ParseGeoPoint userLocation = new ParseGeoPoint(
										lastKnownLocation.getLatitude(),
										lastKnownLocation.getLongitude());
								double distanceInKms =
										driverLocation.distanceInKilometersTo(userLocation);

								if (distanceInKms < 0.01) {
									infoTextView.setText("Your driver is here!");

									ParseQuery<ParseObject> query1 = ParseQuery.getQuery("Request");
									query1.whereEqualTo("username",
											ParseUser.getCurrentUser().getUsername());

									query1.findInBackground((requestObjects, exception1) -> {
										if (exception1 == null) {
											requestObjects.stream()
													.forEach(ParseObject::deleteInBackground);
										}
									});
									handler.postDelayed(() -> {
										requestButton.setVisibility(View.VISIBLE);
										requestButton.setText("Request Pickup");
										requestActive = false;
										driverActive = false;
									}, 5000);
								} else {

									double distanceOneDP = (double) Math.round(distanceInKms * 10) / 10;
									infoTextView.setText("Your Driver is: "
											+ distanceOneDP + " miles away");


									ArrayList<Marker> markers = new ArrayList<>();
									mMap.clear();

									LatLng driverLatLng = new LatLng(driverLocation.getLatitude(),
											driverLocation.getLongitude());
									LatLng userLatLng = new LatLng(userLocation.getLatitude(),
											userLocation.getLongitude());

									markers.add(mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location")));
									markers.add(mMap.addMarker(new MarkerOptions()
											.position(driverLatLng).title("Driver Location")
											.icon(BitmapDescriptorFactory
													.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

									LatLngBounds.Builder builder = new LatLngBounds.Builder();
									markers.stream().forEach((marker -> builder.include(marker.getPosition())));
									LatLngBounds bounds = builder.build();
									mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));


									requestButton.setVisibility(View.INVISIBLE);
									handler.postDelayed(this::checkForUpdates, 2000);
								}
							}
						}
					}
				});
			} else {
				checkForUpdates();
			}
		}));
	}

	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = this::updateMap;


		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		} else {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					0, 0, locationListener);
			Location lastKnownLocation =
					locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				updateMap(lastKnownLocation);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1) {
			if (grantResults.length > 0 && grantResults[0]
					== ContextCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION)) {
				locationManager
						.requestLocationUpdates(LocationManager.GPS_PROVIDER,
								0, 0, locationListener);
				Location lastKnownLocation =
						locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLocation != null) {
					updateMap(lastKnownLocation);
				}
			}
		}
	}

	public void requestButtonCallback(View view) {
		Log.i("Info", "Request status: " + requestActive);

		if (requestActive) {
			ParseQuery<ParseObject> query = new ParseQuery<>("Request");
			query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
			query.findInBackground(((objects, e) -> {
				if (e == null && objects.size() > 0) {
					objects.stream().forEach(ParseObject::deleteInBackground);
					requestActive = false;
					requestButton.setText("Request Pickup");
				}
			}));
		} else {
			if (ContextCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						0, 0, locationListener);
				Location lastKnownLocation =
						locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (lastKnownLocation != null) {
					ParseObject request = new ParseObject("Request");
					request.put("username", ParseUser.getCurrentUser().getUsername());
					ParseGeoPoint parseGeoPoint = new ParseGeoPoint(
							lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
					request.put("location", parseGeoPoint);
					request.saveInBackground((e) -> {
						if (e == null) {
							requestButton.setText(R.string.cancelPickupRequest);
							requestActive = true;
							checkForUpdates();
						}
					});
				}
			} else {
				Toast.makeText(this,
						"Could not find location. Please try again later.",
						Toast.LENGTH_SHORT).show();
			}
		}

		Log.i("Info", "Request status: " + requestActive);
	}

	public void logout(View view) {
		ParseUser.logOut();
		startActivity(new Intent(getApplicationContext(), MainActivity.class));
	}

	private void updateMap(Location location) {
		if (!driverActive) {
			LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
			mMap.clear();
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
			mMap.addMarker(new MarkerOptions().position(userLocation).title("Your location"));
		}
	}
}