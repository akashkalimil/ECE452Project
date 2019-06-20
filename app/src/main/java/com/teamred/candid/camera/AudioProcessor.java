package com.teamred.candid.camera;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class AudioProcessor {
    private static final int SAMPLE_RATE_HZ = 8000; // Use 44.1kHz for actual devices
    private static final long SAMPLE_PERIOD_MS = (long) (1000 * 1.0 / (double) SAMPLE_RATE_HZ);

    private MediaRecorder Recorder;
    private AudioProcessor processor;

    public Observable<Double> audioStream() {
        recorderStart();

        return Observable
                .interval(SAMPLE_PERIOD_MS, TimeUnit.MILLISECONDS)
                .map(i -> recorderGetLevel());
    }

    public void recorderStart() {

        if (Recorder == null) {
            Recorder = new MediaRecorder();
            Recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            Recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            Recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            Recorder.setOutputFile("/dev/null");

        }

        try {
            Recorder.prepare();
        } catch (java.io.IOException ioe) {
            Log.e("mic", "prepare error");
        }

        try {
            Recorder.start();
        } catch (java.lang.SecurityException e) {
            Log.e("mic", "start error");
        }
    }

    public void recorderStop() {
        if (Recorder != null) {
            Recorder.stop();
            Recorder.release();
            Recorder = null;
        }
    }

    public double recorderGetLevel(){
        if (Recorder != null){
            return Recorder.getMaxAmplitude();
        }
        else{
            return 0;
        }
    }
}


/*
    private static final int SAMPLE_RATE_HZ = 8000; // Use 44.1kHz for actual devices
    private static final long SAMPLE_PERIOD_MS = (long) (1000 * 1.0 / (double) SAMPLE_RATE_HZ);

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private final AudioRecord audio;

    public AudioProcessor() {
        audio = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
    }

    public Observable<Double> audioStream() {
        // TODO: Actually use the recorder
//        audio.startRecording();
        return Observable
                .interval(SAMPLE_PERIOD_MS, TimeUnit.MILLISECONDS)
                .map(i -> 0.0);
    }

    public void stop() {
        audio.stop();
    }

    private double getAmplitude() {
        byte[] buffer = new byte[BUFFER_SIZE];
        audio.read(buffer, 0, BUFFER_SIZE);
        int max = 0;
        for (short s : buffer) {
            max = Math.max(max, Math.abs(s));
        }
        return max;
    }*/

//}
