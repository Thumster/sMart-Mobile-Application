package com.example.smart;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LOGIN_ACTIVITY";
    public static final int RC_SIGN_IN = 123;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createSignInIntent();
    }

    public void createSignInIntent() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());
//        new AuthUI.IdpConfig.FacebookBuilder().build(),

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.ic_smart_white_transparent)
                        .setTheme(R.style.FirebaseUI_sMartTheme)
                        .build(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
//                Log.iFirebaseUtil.getCurrentUser()
                Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(mainActivity);
                finish();
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if (response == null) {
                    finishAndRemoveTask();
                }
            }
        }
    }

}