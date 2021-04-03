package com.daksh.ridesharing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

	public void redirectActivity() {
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

		String userType = "rider";
		if (userTypeSwitch.isChecked()) {
			userType = "driver";
		}

		ParseUser.getCurrentUser().put("riderOrDriver", userType);

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

		getSupportActionBar().hide();

//		if (ParseUser.getCurrentUser() == null) {
//			ParseAnonymousUtils.logIn((user, e) -> {
//				if (e == null) {
//					Log.i("Info", "Anonymous login successful");
//				} else {
//					Log.i("Info", "Anonymous login failed");
//				}
//			});
//		} else {
//			if (ParseUser.getCurrentUser().get("riderOrDriver") != null) {
//				Log.i("Info",
//						"Redirecting as " + ParseUser.getCurrentUser().get("riderOrDriver"));
//				redirectActivity();
//			}
//		}
		startActivity(new Intent(getApplicationContext(), LoginActivity.class));

		ParseAnalytics.trackAppOpenedInBackground(getIntent());
	}
}