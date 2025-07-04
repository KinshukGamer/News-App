package com.example.newsapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class SplashActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private SignInButton signInButton;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Auto-login: Skip if already logged in
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        String loginType = prefs.getString("loginType", "");
        if (!loginType.isEmpty()) {
            startActivity(new Intent(SplashActivity.this, StartActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_splash);

        // ✅ Debug Firebase App ID
        try {
            Resources res = getResources();
            int id = res.getIdentifier("google_app_id", "string", getPackageName());
            String appId = res.getString(id);
            Log.d("FIREBASE_APP_ID", "Loaded google_app_id: " + appId);
        } catch (Exception e) {
            Log.e("FIREBASE_APP_ID", "google_app_id missing from google-services.json", e);
        }

        // ✅ Print SHA-1 for debugging (you can delete after setup is complete)
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String sha1 = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                Log.d("APP_SHA1", "SHA-1: " + sha1);
            }
        } catch (Exception e) {
            Log.e("APP_SHA1", "SHA-1 error", e);
        }



        // UI
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        signInButton = findViewById(R.id.sign_in_button);

        databaseHelper = new DatabaseHelper(this);

        setupManualLogin();
        setupGoogleLogin();
    }

    private void setupManualLogin() {
        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.checkUser(username, password)) {
                getSharedPreferences("loginPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("loginType", "sqlite")
                        .apply();

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SplashActivity.this, StartActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        });

        buttonRegister.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
        });
    }

    private void setupGoogleLogin() {
        // Update button text
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View child = signInButton.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setText("Sign in with Google");
                break;
            }
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String email = account.getEmail();

            if (!databaseHelper.checkEmailExists(email)) {
                databaseHelper.registerUser(email, "google_auth");
            }

            getSharedPreferences("loginPrefs", MODE_PRIVATE)
                    .edit()
                    .putString("loginType", "google")
                    .apply();

            Toast.makeText(this, "Google Sign-In Successful\nWelcome " + email, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SplashActivity.this, StartActivity.class));
            finish();

        } catch (ApiException e) {
            Log.e("GOOGLE_SIGN_IN", "Sign-In Failed: code=" + e.getStatusCode(), e);
            Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
