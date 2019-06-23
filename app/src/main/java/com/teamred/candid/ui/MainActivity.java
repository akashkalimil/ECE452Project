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
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.camera.FirebaseImageProcessor;
import com.teamred.candid.R;
import com.teamred.candid.camera.NoiseDetector;
import com.teamred.candid.data.DetectorCoordinator;
import com.teamred.candid.data.SessionManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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

        Button startStop = findViewById(R.id.start_button);
        View overlay = findViewById(R.id.overlay);

        findViewById(R.id.sessions).setOnClickListener(v ->
                startActivity(new Intent(this, SessionListActivity.class)));

        photoCountTextView = findViewById(R.id.photo_count);

        sessionManager = new SessionManager(SessionManager.Mode.SELFIE, getFilesDir());


        startStop.setOnClickListener(v -> {
            if (sessionInProgress) { // Stop
                sessionInProgress = false;

                startStop.animate().alpha(0).withEndAction(() -> {
                    startStop.setText("Start");
                    startStop.animate().alpha(1);
                });
                overlay.animate().setStartDelay(200).alpha(1);

                // Open new activity with collection of photos from this session
                File sessionDir = sessionManager.end();
                photoCountTextView.setText("");
                startActivity(SessionActivity.newIntent(this, sessionDir));

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
        sessionManager.start();

        FirebaseImageProcessor imageProcessor = new FirebaseImageProcessor();
        cameraView.clearFrameProcessors();
        cameraView.addFrameProcessor(imageProcessor);

        AudioProcessor audioProcessor = new AudioProcessor();

        DetectorCoordinator coordinator = new DetectorCoordinator(
                imageProcessor.imageStream(),
                audioProcessor.audioStream(),
                new FaceDetector(),
                new NoiseDetector()
        );

        dispose = coordinator.detectedMomentStream()
                .doOnNext(s -> Log.d(TAG, s.toString()))
                .filter(sessionManager::shouldSaveMoment)
                .throttleFirst(3, TimeUnit.SECONDS) // After capturing, dont save anything for 3 seconds
                .flatMapSingle(sessionManager::saveMoment)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(moment -> {
                    Log.d(TAG, "Detected candid moment! " + moment);
                    animateCapturedMoment();
                });
    }

    private void animateCapturedMoment() {
        photoCountTextView.animate().alpha(0).scaleX(.9f).scaleY(.9f).withEndAction(() -> {
            int count = sessionManager.getSaveCount();
            String format = "Captured %s moment" + (count > 1 ? "s" : "");
            photoCountTextView.setText(String.format(format, sessionManager.getSaveCount()));

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
