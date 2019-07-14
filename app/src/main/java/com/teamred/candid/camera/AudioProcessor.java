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
    private static int mic_arr[] = new int[10]; //Array that stores 10 audio samples, taken 1 second apart
    private static int mic_arr_index = 0;
    private static boolean mic_arr_init = false;
    private static int peak_cnt = 0;

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
                .map(i -> process_amplitude()) //maxAmplitude is  value associated with interval i
                .filter(x->x);
    }

    private void stopRecorder() {
        recorder.stop();
        recorder.release();
    }

    public boolean process_amplitude() {
        int audio_lvl = recorder.getMaxAmplitude();

        //Check if first 10 sec of mic array is not full
        if (!mic_arr_init) {
            //Add val to mic array
            mic_arr[mic_arr_index] = audio_lvl;

            if (mic_arr_index >= mic_arr.length - 1) {
                //mic is finished initializing
                mic_arr_init = true;
                Log.e("a","Mic recorded 10 samples");
            } else {
                mic_arr_index += 1;
            }
        }
        else { //Check if value is greater than moving average
            int avg = 0;
            //compute average of all elements
            for (int i = 0; i < mic_arr.length; i++) {
                avg += mic_arr[i];
            }

            avg = (int)((float) (((float)avg)/((float)mic_arr.length)));

            if (audio_lvl > avg){
                peak_cnt++;

                if (peak_cnt ==2 ){
                    Log.e("a","Audio peak detected " +String.valueOf(avg) + " "  + String.valueOf(audio_lvl));
                    peak_cnt = 0;
                    return true;
                }
            }
            else {
                if (peak_cnt > 0){
                    peak_cnt--;
                }
            }
            Log.e("a", "Audio val " + String.valueOf(avg) + " " + String.valueOf(audio_lvl));

            //update array
            if (mic_arr_index == mic_arr.length - 1){
                mic_arr_index = 0;
            }
            else{
                mic_arr_index+= 1;
            }

            mic_arr[mic_arr_index] = audio_lvl;
            }
        return false;
    }

}
