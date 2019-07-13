package com.teamred.candid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.otaliastudios.cameraview.CameraView;
import com.teamred.candid.camera.AudioProcessor;
import com.teamred.candid.camera.BitmapProcessor;
import com.teamred.candid.R;
import com.teamred.candid.data.SessionManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CandidMain";

    private Disposable dispose;
    private CameraView cameraView;
    private SessionManager sessionManager;

    private TextView photoCountTextView;
    boolean sessionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);
        cameraView.clearFrameProcessors();

        Button startStop = findViewById(R.id.start_button);
        View overlay = findViewById(R.id.overlay);

        findViewById(R.id.sessions).setOnClickListener(v ->
                startActivity(new Intent(this, SessionListActivity.class)));

        photoCountTextView = findViewById(R.id.photo_count);

        sessionManager = new SessionManager(getFilesDir());

        startStop.setOnClickListener(v -> {
            if (sessionInProgress) { // Stop
                sessionInProgress = false;

                startStop.animate().alpha(0).withEndAction(() -> {
                    startStop.setText("Start");
                    startStop.animate().alpha(1);
                });
                overlay.animate().setStartDelay(200).alpha(1);

                endSession();

            } else { // Start
                sessionInProgress = true;

                startStop.animate().setStartDelay(200).alpha(0).withEndAction(() -> {
                    startStop.setText("End");
                    startStop.animate().alpha(1);
                    startSession();
                });
                overlay.animate().setStartDelay(200).alpha(0.5f);
            }
        });
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
            int count = sessionManager.getPhotoCount();
            String format = "Captured %s moment" + (count > 1 ? "s" : "");
            photoCountTextView.setText(String.format(format, sessionManager.getPhotoCount()));

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
