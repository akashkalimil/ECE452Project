package com.teamred.candid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.teamred.candid.camera.AudioProcessor;
import com.teamred.candid.camera.BitmapProcessor;
import com.teamred.candid.R;
import com.teamred.candid.data.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CandidMain";
    private static final int RC_SIGN_IN = 512;

    private Disposable dispose;
    private CameraView cameraView;
    private SessionManager sessionManager;

    private Button startButton;
    private TextView photoCountTextView;
    private View overlay;

    private boolean sessionInProgress = false;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                handleSignInResult(task);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) throws ApiException {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            String authCode = account.getServerAuthCode();

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormEncodingBuilder()
                .add("grant_type", "authorization_code")
                .add("client_id", "1006666980865-tu83mb86ku1et8ccikrq7ajdagl4ohdp.apps.googleusercontent.com")
                .add("client_secret", "GNaSkd6yT73PhM6xN_eUnox-")
                .add("redirect_uri","")
                .add("code",authCode )
                .add("id_token", idToken)
                .build();
        final Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                Log.e("a", e.toString());
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    final String message = jsonObject.toString(5);
                    Log.i("a", message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary")	)
                .requestIdToken("1006666980865-tu83mb86ku1et8ccikrq7ajdagl4ohdp.apps.googleusercontent.com")
                .requestServerAuthCode("1006666980865-tu83mb86ku1et8ccikrq7ajdagl4ohdp.apps.googleusercontent.com", true)
                .build();



        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.sign_in_button).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

    /*Add after
    // Check for existing Google Sign In account, if the user is already signed in
// the GoogleSignInAccount will be non-null.
GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
updateUI(account);
     */


        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
        cameraView.clearFrameProcessors();
        cameraView.setFlash(Flash.OFF);
        cameraView.setFacing(Facing.BACK);

        startButton = findViewById(R.id.start_button);
        overlay = findViewById(R.id.overlay);
        photoCountTextView = findViewById(R.id.photo_count);

        sessionManager = new SessionManager(getFilesDir());
    }

    public void onStartClick(View v) {
        if (sessionInProgress) { // Stop
            sessionInProgress = false;

            startButton.animate().alpha(0).withEndAction(() -> {
                startButton.setText("Start");
                startButton.animate().alpha(1);
            });
            overlay.animate().setStartDelay(200).alpha(1);

            endSession();

        } else { // Start
            sessionInProgress = true;

            startButton.animate().setStartDelay(200).alpha(0).withEndAction(() -> {
                startButton.setText("End");
                startButton.animate().alpha(1);
                startSession();
            });
            overlay.animate().setStartDelay(200).alpha(0.5f);
        }
    }

    public void onSessionsClick(View v) {
        startActivity(new Intent(this, SessionListActivity.class));
    }

    public void onFlashClick(View v) {
        Flash flash = cameraView.getFlash();
        cameraView.setFlash(flash == Flash.OFF ? Flash.ON : Flash.OFF);
    }

    public void onFlipCameraClick(View v) {
        Facing facing = cameraView.getFacing();
        cameraView.setFacing(facing == Facing.BACK ? Facing.FRONT : Facing.BACK);
    }

    private void startSession() {
        BitmapProcessor bitmapProcessor = new BitmapProcessor();
        cameraView.addFrameProcessor(bitmapProcessor);

        AudioProcessor audioProcessor = new AudioProcessor();
        dispose = sessionManager
                .start(bitmapProcessor.imageStream(), audioProcessor.audioStream())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> {
                    Log.d(TAG, "Saved file: " + file.getPath());
                    animateCapturedMoment();
                });
    }

    private void endSession() {
        // Open new activity with collection of photos from this session
        SessionManager.Session session = sessionManager.end();
        photoCountTextView.setText("");
        startActivity(SessionActivity.newIntent(this, session.getDirectory()));
    }

    private void animateCapturedMoment() {
        photoCountTextView.animate().alpha(0).scaleX(.9f).scaleY(.9f).withEndAction(() -> {
            int count = sessionManager.getPictureCount();
            String format = "Captured %s moment" + (count > 1 ? "s" : "");
            photoCountTextView.setText(String.format(format, sessionManager.getPictureCount()));

            photoCountTextView.animate().alpha(1).scaleX(1.2f).scaleY(1.2f)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> photoCountTextView.animate().scaleX(1).scaleY(1));
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (dispose != null && !dispose.isDisposed()) {
            dispose.dispose();
        }
    }
}
