package com.teamred.candid.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.otaliastudios.cameraview.CameraView;
import com.teamred.candid.camera.AudioProcessor;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.camera.FirebaseImageProcessor;
import com.teamred.candid.R;
import com.teamred.candid.camera.NoiseDetector;
import com.teamred.candid.data.DetectorCoordinator;
import com.teamred.candid.data.SessionManager;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CandidMain";

    private Disposable dispose;
    private CameraView cameraView;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);


        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        Button start = findViewById(R.id.start_button);
        Button end = findViewById(R.id.end_button);
        View overlay = findViewById(R.id.overlay);

        sessionManager = new SessionManager(SessionManager.Mode.SELFIE, getFilesDir());

        start.setOnClickListener(v -> {
            overlay.animate()
                    .setStartDelay(200)
                    .alpha(0)
                    .withEndAction(() -> {
                        overlay.setVisibility(View.GONE);
                        start.setVisibility(View.GONE);
                        end.setVisibility(View.VISIBLE);
                        end.animate().alpha(1);
                        sessionManager.start();
                        startCamera();
                    });
        });

        end.setOnClickListener(v -> {
            // Open new activity with collection of photos from this session
            File sessionDir = sessionManager.end();
            startActivity(ViewSessionActivity.newIntent(this, sessionDir));

            // Make the start button visible again
            start.setVisibility(View.VISIBLE);
            overlay.setVisibility(View.VISIBLE);
            overlay.animate().alpha(1);
        });
    }

    private void startCamera() {
        FirebaseImageProcessor imageProcessor = new FirebaseImageProcessor();
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
                .flatMapSingle(sessionManager::saveMoment)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(moment -> Log.d(TAG, "Detected candid moment! " + moment));
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
