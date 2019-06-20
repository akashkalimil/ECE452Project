package com.teamred.candid.camera;

import com.teamred.candid.data.Detector;
import com.teamred.candid.model.Noise;

import io.reactivex.Maybe;

public class NoiseDetector implements Detector<Double, Noise> {
    @Override
    public Maybe<Noise> detect(Double audioLevel) {
        // TODO actually analyze the mic input
        Noise noise = new Noise();
        noise.level = (int) Math.round(audioLevel);
        return Maybe.just(noise);
    }
}
