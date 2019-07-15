package com.teamred.candid.camera;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.internal.functions.Functions;

public class AudioProcessor {

    private static final String TAG = "AudioProcessor";
    private static final int SAMPLE_PERIOD = 500; // 1 second

    private int sampleIdx = 0;
    private int peakCount = 0;
    private boolean samplesInit = false;
    private final int[] samples = new int[10]; //Array that stores 10 audio samples, taken 1 second apart

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

    public Observable<Boolean> audioStream() {
        if (recorder == null) return Observable.empty();
        return Observable
                .interval(SAMPLE_PERIOD, TimeUnit.MILLISECONDS)
                .doOnDispose(this::stopRecorder)
                .map(i -> isPeak()) //maxAmplitude is  value associated with interval i
                .filter(isPeak -> isPeak);
    }

    private void stopRecorder() {
        recorder.stop();
        recorder.release();
    }

    private boolean isPeak() {
        int audioLvl = recorder.getMaxAmplitude();

        //Check if first 10 sec of mic array is not full
        if (!samplesInit) {
            //Add val to mic array
            samples[sampleIdx] = audioLvl;

            if (sampleIdx >= samples.length - 1) {
                //mic is finished initializing
                samplesInit = true;
                Log.e("a", "Mic recorded 10 samples");
            } else {
                sampleIdx += 1;
            }
        } else { //Check if value is greater than moving average
            int avg = 0;
            //compute average of all elements
            for (int i = 0; i < samples.length; i++) {
                avg += samples[i];
            }

            avg = (int) ((float) (((float) avg) / ((float) samples.length)));

            if (audioLvl > avg) {
                peakCount++;

                if (peakCount == 2) {
                    Log.e("a", "Audio peak detected " + avg + " " + audioLvl);
                    peakCount = 0;
                    return true;
                }
            } else {
                if (peakCount > 0) {
                    peakCount--;
                }
            }
            Log.e("a", "Audio val " + avg + " " + audioLvl);

            //update array
            if (sampleIdx == samples.length - 1) {
                sampleIdx = 0;
            } else {
                sampleIdx += 1;
            }

            samples[sampleIdx] = audioLvl;
        }
        return false;
    }
    
}
