package com.teamred.candid.camera;

import android.icu.text.AlphabeticIndex;
import android.media.MediaRecorder;
import android.provider.MediaStore;
import android.util.Log;

import com.google.android.gms.vision.label.internal.client.INativeImageLabeler;
import com.teamred.candid.data.Detector;
import com.teamred.candid.model.Noise;

import io.reactivex.Maybe;

public class NoiseDetector implements Detector<Double, Noise> {

    @Override
    public Maybe<Noise> detect(Double val) {

        Log.d("D", "noise: "+ val);
        // TODO actually analyze the mic input
        Noise noise = new Noise();
        noise.level = 0;




        /*
        if (Recorder != null) {
            noise.level = (int) Math.round(Recorder.getMaxAmplitude());
        }
        else {
            noise.level = 0;
        }*/
        return Maybe.just(noise);
    }

}
