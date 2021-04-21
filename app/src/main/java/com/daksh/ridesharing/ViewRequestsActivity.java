package com.daksh.ridesharing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.parse.Parse;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Objects;

public class ViewRequestsActivity extends AppCompatActivity {

	LocationManager locationManager;
	LocationListener locationListener;

	ListView requestListView;

	ArrayList<String> requests = new ArrayList<>();
	ArrayList<Double> requestLatitudes = new ArrayList<>();
	ArrayList<Double> requestLongitudes = new ArrayList<>();
	ArrayList<String> requestUsernames = new ArrayList<>();

	ArrayAdapter arrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_requests);

		setTitle("Nearby Requests");

		requestListView = findViewById(R.id.requestListView);
		arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);

		requests.clear();
		requests.add("Getting nearby requests...");

		requestListView.setAdapter(arrayAdapter);
		requestListView.setOnItemClickListener((adapterView, view, i, l) -> {
			if (ActivityCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				Location lastKnownLocation =
						locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (requestLatitudes.size() > i && requestLongitudes.size() > i
						&& requestUsernames.size() > 0 && Objects.nonNull(lastKnownLocation)) {
					Intent intent = new Intent(getApplicationContext(),
							DriverLocationActivity.class);
					intent.putExtra("requestUsername", requestUsernames.get(i));
					intent.putExtra("requestLatitude", requestLatitudes.get(i));
					intent.putExtra("requestLongitude", requestLongitudes.get(i));

					intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());
					intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());


					startActivity(intent);
				}
			}
		});

		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = (location) -> {
			updateListView(location);
			ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(),
					location.getLongitude()));
			ParseUser.getCurrentUser().saveInBackground((e) -> {
				if (e == null) {
					Log.i("updateLocation", "Successful");
				} else {
					Log.i("updateLocation", "Failed", e);
				}
			});
		};


		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		} else {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					0, 0, locationListener);
			Location lastKnownLocation =
					locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				updateListView(lastKnownLocation);
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
					updateListView(lastKnownLocation);
				}
			}
		}
	}

	/**
	 * Dispatch onResume() to fragments.  Note that for better inter-operation
	 * with older versions of the platform, at the point of this call the
	 * fragments attached to the activity are <em>not</em> resumed.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		} else {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					0, 0, locationListener);
			Location lastKnownLocation =
					locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastKnownLocation != null) {
				updateListView(lastKnownLocation);
			}
		}
	}

	private void updateListView(Location location) {
		if (location != null) {
			ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
			query.include("user");
			query.include("user.destinationLat");
			query.include("user.destinationLong");
			ParseGeoPoint geoPointLocation =
					new ParseGeoPoint(location.getLatitude(), location.getLongitude());
			query.whereNear("location", geoPointLocation);
			query.whereDoesNotExist("driverUsername");
			query.setLimit(10);
			query.findInBackground(((objects, e) -> {
				if (e == null) {
					requests.clear();
					requestLatitudes.clear();
					requestLongitudes.clear();

					objects.stream()
							.filter(object -> Objects.nonNull(object.get("location")))
							.filter(object -> {
								final ParseUser userObj = object.getParseUser("user");

								final double userDestinationLat = userObj.getDouble("destinationLat");
								final double userDestinationLong = userObj.getDouble("destinationLong");
								final LatLng requestDestLatLng = new LatLng(userDestinationLat, userDestinationLong);

								final double currentUserDestLat =
										ParseUser.getCurrentUser().getDouble("destinationLat");
								final double currentUserDestLong =
										ParseUser.getCurrentUser().getDouble("destinationLong");
								final LatLng currDestLatLng = new LatLng(currentUserDestLat, currentUserDestLong);

								final boolean filterRequestRes =
										currDestLatLng.equals(requestDestLatLng);
								Log.d("filterRequest", String.valueOf(filterRequestRes));

								return filterRequestRes;
							})
							.peek(object -> requestUsernames.add(object.getString("username")))
							.map(object -> (ParseGeoPoint) object.get("location"))
							.peek((requestLocation) -> {
								requestLatitudes.add(requestLocation.getLatitude());
								requestLongitudes.add(requestLocation.getLongitude());
							})
							.mapToDouble(geoPointLocation::distanceInKilometersTo)
							.map(distance -> Math.round(distance * 10)/10.0)
							.forEach(distance -> requests.add(distance + " kilometres"));
				} else {
					requests.add("No active requests nearby");
				}

				arrayAdapter.notifyDataSetChanged();
			}));

		}
	}

	public void logout(View view) {
		ParseUser.logOut();
		final Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));
		finish();
	}

	public void navigate(View view) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
		query.include("user");

		final StringBuilder builder = new StringBuilder("http://maps.google.com/maps?saddr=");
//		ParseGeoPoint tmp = ParseUser.getCurrentUser().getParseGeoPoint()
//		builder.append(ParseUser.getCurrentUser().get);
//		Intent directionsIntent = new Intent(android.content.Intent.ACTION_VIEW,
//				Uri.parse("http://maps.google.com/maps?saddr="
//						+ intent.getDoubleExtra("driverLatitude", 0)
//						+ "," + intent.getDoubleExtra("driverLongitude", 0)
//						+ "&daddr="
//						+ intent.getDoubleExtra("requestLatitude", 0)
//						+ ","
//						+ intent.getDoubleExtra("requestLongitude", 0)));
//		startActivity(directionsIntent);
	}


}