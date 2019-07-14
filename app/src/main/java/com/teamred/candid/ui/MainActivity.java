package com.teamred.candid.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Flash;
import com.teamred.candid.camera.AudioProcessor;
import com.teamred.candid.camera.BitmapProcessor;
import com.teamred.candid.R;
import com.teamred.candid.data.SessionManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "CandidMain";

    private Disposable dispose;
    private CameraView cameraView;
    private SessionManager sessionManager;

    private Button startButton;
    private TextView photoCountTextView;
    private View overlay;

    private boolean sessionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

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

            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Analyzing audio...");
            dialog.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    dialog.dismiss();
                }
            }, 5000);
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
