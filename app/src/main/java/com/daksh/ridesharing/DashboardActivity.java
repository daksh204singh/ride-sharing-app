package com.daksh.ridesharing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;
import com.parse.http.ParseHttpResponse;

import java.util.Arrays;
import java.util.Objects;

public class DashboardActivity extends AppCompatActivity {

	private Place placeSelected;

	private void redirectActivity() {
		if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {
			Log.i("Info", "Redirecting as rider");
			startActivity(new Intent(getApplicationContext(), RiderActivity.class));
		} else {
			Log.i("Info", "Redirecting as driver");
			startActivity(new Intent(getApplicationContext(), ViewRequestsActivity.class));
		}
	}

	public void getStarted(View view) {
		final Switch userTypeSwitch = (Switch) findViewById(R.id.userTypeSwitch);
		Log.i("Switch value", String.valueOf(userTypeSwitch.isChecked()));

		if (placeSelected == null) {
			Toast.makeText(this, "Please select a destination",
					Toast.LENGTH_SHORT).show();
			return;
		}

		String userType = "rider";
		if (userTypeSwitch.isChecked()) {
			userType = "driver";
		}

		ParseUser.getCurrentUser().put("riderOrDriver", userType);

		final LatLng latLng = placeSelected.getLatLng();
		final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(latLng.latitude, latLng.longitude);
		ParseUser.getCurrentUser().put("destination", parseGeoPoint);

		ParseUser.getCurrentUser().saveInBackground((e) -> {
			if (e == null) {
				redirectActivity();
			} else {
				Log.e("ParseUser", "Error save in background user type", e);
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Objects.requireNonNull(getSupportActionBar()).hide();

		Places.initialize(DashboardActivity.this, String.valueOf(R.string.google_places_api));
		PlacesClient placesClient = Places.createClient(this);
		AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

		// Initialize the AutocompleteSupportFragment.
		AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
				getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

		// Specify the types of place data to return.
		autocompleteFragment.setPlaceFields(Arrays.asList(
				Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

		// Set up a PlaceSelectionListener to handle the response.
		autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
			@Override
			public void onPlaceSelected(@NonNull Place place) {
				// TODO: Get info about the selected place.
				Log.i("AutocompleteFragment", "Place: " + place.getName()
						+ ", " + place.getId() + ", [" + place.getLatLng() + "]");
				placeSelected = place;
			}

			@Override
			public void onError(@NonNull Status status) {
				// TODO: Handle the error.
				Log.i("AutocompleteFragment", "An error occurred: " + status);
			}
		});
	}
}