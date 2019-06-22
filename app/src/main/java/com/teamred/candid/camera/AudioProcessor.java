package com.teamred.candid.camera;

import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class AudioProcessor {
    private static final int SAMPLE_RATE_HZ = 8000; // Use 44.1kHz for actual devices
    private static final long SAMPLE_PERIOD_MS = (long) (1000 * 1.0 / (double) SAMPLE_RATE_HZ);

    private MediaRecorder recorder;

    public Observable<Double> audioStream() {
        recorderStart();

        return Observable
                .interval(SAMPLE_PERIOD_MS, TimeUnit.MILLISECONDS)
                .map(i -> recorderGetLevel());
    }

    private void recorderStart() {

        if (recorder == null) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile("/dev/null");

        }

        try {
            recorder.prepare();
        } catch (java.io.IOException ioe) {
            Log.e("mic", "prepare error");
        }

        try {
            recorder.start();
        } catch (java.lang.SecurityException e) {
            Log.e("mic", "start error");
        }
    }

    public void recorderStop() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    private double recorderGetLevel() {
        return recorder != null ? recorder.getMaxAmplitude() : 0;
    }
}
