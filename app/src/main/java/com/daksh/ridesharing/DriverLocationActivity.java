package com.daksh.ridesharing;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;

public class DriverLocationActivity extends FragmentActivity implements OnMapReadyCallback {

	private GoogleMap mMap;
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_driver_location);

		intent = getIntent();

		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
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

		ArrayList<Marker> markers = new ArrayList<>();

		LatLng driverLatLng = new LatLng(intent.getDoubleExtra("driverLatitude", 0),
				intent.getDoubleExtra("driverLongitude", 0));
		markers.add(mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Location")));


		LatLng requestLatLng = new LatLng(intent.getDoubleExtra("requestLatitude", 0),
					intent.getDoubleExtra("requestLongitude", 0));
		markers.add(mMap.addMarker(new MarkerOptions()
					.position(requestLatLng).title("Request Location")
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		markers.stream().forEach((marker -> builder.include(marker.getPosition())));
		LatLngBounds bounds = builder.build();
		mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 60));
	}

	public void acceptRequest(View view) {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
		query.whereEqualTo("username", intent.getStringExtra("requestUsername"));
		query.findInBackground(((objects, e) -> {
			if (e == null && objects.size() > 0) {
				objects.stream().forEach(object -> {
					object.put("driverUsername", ParseUser.getCurrentUser().getUsername());
					object.saveInBackground((exception) -> {
						if (exception == null) {
							Log.i("AcceptRequest", "request accepted");
							finish();
						} else {
							Log.i("AcceptRequest", "request was not accepted", exception);
							Toast.makeText(this, "Unable to accept request: "
									+ exception.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				});
			}
		}));
	}
}