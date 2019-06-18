package com.teamred.candid;

import android.graphics.Bitmap;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.util.List;

class CandidMoment {
    private final Bitmap bitmap;
    private final long timestamp;
    private final List<FirebaseVisionFace> faces;

    CandidMoment(Bitmap bitmap, List<FirebaseVisionFace> faces) {
        this.timestamp = System.currentTimeMillis();
        this.bitmap = bitmap;
        this.faces = faces;
    }

    @Override
    public String toString() {
        long smiling = faces.stream().filter(f -> f.getSmilingProbability() > .7).count();
        return String.format("CandidMoment(total_faces=%s, smiling_faces=%s, time=%s)",
                faces.size(), smiling, timestamp);
    }

    public boolean hasFaces() {
        return faces.size() > 0;
    }

    public List<FirebaseVisionFace> getFaces() {
        return faces;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
