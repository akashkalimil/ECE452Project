package com.teamred.candid;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.util.List;

class CandidMoment {
    private final long timestamp;
    private final List<FirebaseVisionFace> faces;

    CandidMoment(long timestamp, List<FirebaseVisionFace> faces) {
        this.timestamp = timestamp;
        this.faces = faces;
    }

    @Override
    public String toString() {
        long smiling = faces.stream().filter(f -> f.getSmilingProbability() > .7).count();
        return String.format("CandidMoment(total_faces=%s, smiling_faces=%s, time=%s)",
                faces.size(), smiling, timestamp);
    }
}
