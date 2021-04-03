package com.daksh.ridesharing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseAnalytics;
import com.parse.ParseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

	private boolean signUpModeActive;
	private EditText usernameEditText, passwordEditText;
	private TextView changeSignUpModeTextView;
	private Button singUpButton;

	public void signUp(View view) {
		if (TextUtils.isEmpty(usernameEditText.getText())
				|| TextUtils.isEmpty(passwordEditText.getText())) {
			Toast.makeText(this, "Username and password are required",
					Toast.LENGTH_SHORT).show();
		} else {
			if (signUpModeActive) {
				final ParseUser user = new ParseUser();
				user.setUsername(usernameEditText.getText().toString());
				user.setPassword(passwordEditText.getText().toString());

				user.signUpInBackground(ex -> {
					if (Objects.isNull(ex)) {
						Log.i("SignUp", "Successful");
						startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
					} else {
						Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
					}
				});
			} else {
				ParseUser.logInInBackground(usernameEditText.getText().toString(),
						passwordEditText.getText().toString(),
						((user, e) -> {
							if (Objects.nonNull(user)) {
								Log.i("SignUp", "Login successful");
								startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
							} else {
								Toast.makeText(this, e.getMessage(),
										Toast.LENGTH_SHORT).show();
							}
						}));
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		signUpModeActive = true;
		passwordEditText = findViewById(R.id.passwordEditText);
		usernameEditText = findViewById(R.id.usernameEditText);
		changeSignUpModeTextView = findViewById(R.id.changeSignupModeTextView);
		singUpButton = findViewById(R.id.signupButton);

		changeSignUpModeTextView.setOnClickListener(this::onClickSignUpMode);
		passwordEditText.setOnKeyListener(this::onKeyPasswordEnter);
		findViewById(R.id.backgroundRelativeLayout).setOnClickListener(this::hideKeyboard);
		findViewById(R.id.logoImageView).setOnClickListener(this::hideKeyboard);

		if (ParseUser.getCurrentUser() != null) {
			startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
		}

		ParseAnalytics.trackAppOpenedInBackground(getIntent());
	}

	/**
	 * OnClickListener for changeSignUpModeTextView.
	 * @param view SignUpModeTextView.
	 */
	private void onClickSignUpMode(View view) {
		Log.i("AppInfo", "Change SignUp mode");
		if (signUpModeActive) {
			signUpModeActive = false;
			singUpButton.setText("Login");
			changeSignUpModeTextView.setText("Or, SignUp");
		} else {
			signUpModeActive = true;
			singUpButton.setText("SignUp");
			changeSignUpModeTextView.setText("Or, Login");
		}
	}

	/**
	 * Hide keyboard.
	 * @param view view.
	 */
	private void hideKeyboard(View view) {
		final View focusedView = this.getCurrentFocus();
		if (Objects.nonNull(focusedView)) {
			((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
		}
	}

	private boolean onKeyPasswordEnter(final View view, final int code, final KeyEvent keyEvent) {
		boolean enterPressed = false;
		if (code == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
			signUp(view);
			enterPressed = true;
		}

		return enterPressed;
	}

}