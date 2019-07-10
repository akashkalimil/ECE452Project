package com.teamred.candid.data;

import com.teamred.candid.vision.BatchResponse.Response;
import com.teamred.candid.vision.CloudVision.FaceAnnotation;

import java.util.HashSet;
import java.util.Set;

public class EmotionExtractor {

    public enum Emotion {
        JOY,
        SORROW,
        ANGER,
        SURPRISE,
        SERIOUS
    }

    private static String LIKELY = "LIKELY";
    private static String VERY_LIKELY = "VERY_LIKELY";

    private static String UNLIKELY = "UNLIKELY";
    private static String VERY_UNLIKELY = "VERY_UNLIKELY";

    Set<Emotion> extract(Response response) {
        if (response.annotations == null) return new HashSet<>();

        Set<Emotion> res = new HashSet<>();
        for (FaceAnnotation face : response.annotations) {
            if (isLikely(face.joyLikelihood)) {
                res.add(Emotion.JOY);
            } else if (isLikely(face.sorrowLikelihood)) {
                res.add(Emotion.SORROW);
            } else if (isLikely(face.surpriseLikelihood)) {
                res.add(Emotion.SURPRISE);
            } else if (isLikely(face.angerLikelihood)) {
                res.add(Emotion.ANGER);
            } else if (isSerious(face)) {
                res.add(Emotion.SERIOUS);
            }
        }
        return res;
    }

    private static boolean isSerious(FaceAnnotation face) {
        return isUnlikely(face.joyLikelihood) && isUnlikely(face.sorrowLikelihood)
                && isUnlikely(face.angerLikelihood) && isUnlikely(face.surpriseLikelihood);
    }

    private static boolean isLikely(String likelihood) {
        return likelihood.equals(LIKELY) || likelihood.equals(VERY_LIKELY);
    }

    private static boolean isUnlikely(String likelihood) {
        return likelihood.equals(UNLIKELY) || likelihood.equals(VERY_UNLIKELY);
    }
}