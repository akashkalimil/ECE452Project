package com.teamred.candid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.otaliastudios.cameraview.CameraView;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CandidMain";

    private Disposable dispose;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        Button start = findViewById(R.id.start_button);
        View overlay = findViewById(R.id.overlay);

        start.setOnClickListener(v -> overlay.animate()
                .setStartDelay(200)
                .alpha(0)
                .withEndAction(() -> {
                    overlay.setVisibility(View.GONE);
                    start.setVisibility(View.GONE);
                    startCamera();
                }));
    }

    private void startCamera() {
        FaceDetector faceDetector = new FaceDetector();
        FirebaseImageProcessor imageProcessor = new FirebaseImageProcessor();

        cameraView.addFrameProcessor(imageProcessor);

        dispose = imageProcessor
                .imageStream()
                .flatMapMaybe(faceDetector::detectMomentInImage)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(candidMoment -> {
                    Log.d(TAG, "Detected candid moment! " + candidMoment);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dispose != null && !dispose.isDisposed()) {
            dispose.dispose();
        }
    }
}
