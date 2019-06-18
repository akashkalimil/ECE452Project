package com.teamred.candid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.otaliastudios.cameraview.CameraView;

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
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        Button start = findViewById(R.id.start_button);
        Button end = findViewById(R.id.end_button);
        View overlay = findViewById(R.id.overlay);

        sessionManager = new SessionManager(SessionManager.Mode.SELFIE, getFilesDir());

        start.setOnClickListener(v -> overlay.animate()
                .setStartDelay(200)
                .alpha(0)
                .withEndAction(() -> {
                    overlay.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    end.setVisibility(View.VISIBLE);
                    end.animate().alpha(1);
                    startCamera();
                    sessionManager.start();
                }));

        end.setOnClickListener(v -> {
            // Open new activity with collection of photos from this session
            File sessionDir = sessionManager.end();
            startActivity(ViewSessionActivity.newIntent(this, sessionDir));
        });
    }

    private void startCamera() {
        FaceDetector faceDetector = new FaceDetector();
        FirebaseImageProcessor imageProcessor = new FirebaseImageProcessor();

        cameraView.addFrameProcessor(imageProcessor);

        // TODO: rate limit the image stream
        //      (if we save an image, wait for a few secs before listening again)
        dispose = imageProcessor
                .imageStream()
                .flatMapMaybe(faceDetector::detectMomentInImage)
                .flatMapMaybe(sessionManager::saveOrDispose)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(candidMoment -> Log.d(TAG, "Detected candid moment! " + candidMoment));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dispose != null && !dispose.isDisposed()) {
            dispose.dispose();
        }
    }
}
