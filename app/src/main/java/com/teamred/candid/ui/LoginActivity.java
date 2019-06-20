package com.teamred.candid.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.teamred.candid.R;
import com.teamred.candid.data.GoogleDriveService;

public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 9123;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account == null) {
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))

//                            .requestScopes(new Scope(Scopes.DRIVE_FILE))
                            .requestEmail()
                            .build();
            GoogleSignInClient client = GoogleSignIn.getClient(getApplicationContext(), signInOptions);
            startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.d(TAG, "req = " + requestCode + " res = " + resultCode);
        if (requestCode == REQUEST_CODE_SIGN_IN && resultCode == RESULT_OK && resultData != null) {
            GoogleSignIn.getSignedInAccountFromIntent(resultData)
                    .addOnSuccessListener(googleSignInAccount -> {
                        Log.d(TAG, "Signed in as " + googleSignInAccount.getEmail());
                        GoogleDriveService.authenticate(googleSignInAccount, this);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Unable to sign in.", e));
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

}
