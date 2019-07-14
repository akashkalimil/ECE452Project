package com.teamred.candid.camera;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class AudioProcessor {

    private static final String TAG = "AudioProcessor";
    private static final int SAMPLE_PERIOD = 1; // 1 second

    private MediaRecorder recorder;

    public AudioProcessor() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile("/dev/null");
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start microphone!", e);
            recorder = null;
        }
    }

    public Observable<Integer> audioStream() {
        if (recorder == null) return Observable.empty();
        return Observable
                .interval(SAMPLE_PERIOD, TimeUnit.SECONDS)
                .doOnDispose(this::stopRecorder)
                .map(i -> recorder.getMaxAmplitude());
    }

    private void stopRecorder() {
        recorder.stop();
        recorder.release();
    }
//    2019-07-13 21:33:10.548 32143-967/com.teamred.candid W/StreamingFormatChecker: ML Kit has detected that you seem to pass camera frames to the detector as a Bitmap object. This is inefficient. Please use YUV_420_888 format for camera2 API or NV21 format for (legacy) camera API and directly pass down the byte array to ML Kit.

}
