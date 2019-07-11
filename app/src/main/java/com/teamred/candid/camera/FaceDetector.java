package com.teamred.candid.camera;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

import io.reactivex.Single;

public class FaceDetector {

    public static class Result {
        public final Bitmap image;
        public final List<FirebaseVisionFace> faces;

        Result(Bitmap image, List<FirebaseVisionFace> faces) {
            this.image = image;
            this.faces = faces;
        }
    }

    private static final FirebaseVisionFaceDetectorOptions DETECTOR_OPTIONS =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setMinFaceSize(0.1f)
                    .build();

    private final FirebaseVisionFaceDetector detector;

    public FaceDetector() {
        detector = FirebaseVision.getInstance().getVisionFaceDetector(DETECTOR_OPTIONS);
    }

    public Single<Result> detect(FirebaseVisionImage image) {
        return Single.create(emitter -> {
            try {
                List<FirebaseVisionFace> faces = Tasks.await(detector.detectInImage(image));
                emitter.onSuccess(new Result(image.getBitmap(), faces));
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
