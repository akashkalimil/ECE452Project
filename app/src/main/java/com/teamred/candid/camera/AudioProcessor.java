package com.teamred.candid.camera;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class AudioProcessor {

    private static final String TAG = "AudioProcessor";
    private static final int SAMPLE_PERIOD = 500; // 1 second
    private int micArr[] = new int[10]; //Array that stores 10 audio samples, taken 1 second apart
    private int micArrIndex = 0;
    private boolean micArrInit = false;
    private int peakCnt = 0;

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
                .map(i -> processAmplitude()) //maxAmplitude is  value associated with interval i
                .filter(x->x);
    }

    private void stopRecorder() {
        recorder.stop();
        recorder.release();
    }

    public boolean processAmplitude() {
        int audioLvl = recorder.getMaxAmplitude();

        //Check if first 10 sec of mic array is not full
        if (!micArrInit) {
            //Add val to mic array
            micArr[micArrIndex] = audioLvl;

            if (micArrIndex >= micArr.length - 1) {
                //mic is finished initializing
                micArrInit = true;
                Log.e("a","Mic recorded 10 samples");
            } else {
                micArrIndex += 1;
            }
        }
        else { //Check if value is greater than moving average
            int avg = 0;
            //compute average of all elements
            for (int i = 0; i < micArr.length; i++) {
                avg += micArr[i];
            }

            avg = (int)((float) (((float)avg)/((float)micArr.length)));

            if (audioLvl > avg){
                peakCnt++;

                if (peakCnt ==2 ){
                    Log.e("a","Audio peak detected " +String.valueOf(avg) + " "  + String.valueOf(audio_lvl));
                    peakCnt = 0;
                    return true;
                }
            }
            else {
                if (peakCnt > 0){
                    peakCnt--;
                }
            }
            Log.e("a", "Audio val " + String.valueOf(avg) + " " + String.valueOf(audio_lvl));

            //update array
            if (micArrIndex == micArr.length - 1){
                micArrIndex = 0;
            }
            else{
                micArrIndex+= 1;
            }

            micArr[micArrIndex] = audioLvl;
            }
        return false;
    }

}
