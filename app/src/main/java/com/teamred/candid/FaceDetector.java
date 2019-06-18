package com.teamred.candid;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

import io.reactivex.Maybe;

class FaceDetector {

    private static final FirebaseVisionFaceDetectorOptions DETECTOR_OPTIONS =
            new FirebaseVisionFaceDetectorOptions.Builder()
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                    .setMinFaceSize(0.1f)
                    .build();

    private final FirebaseVisionFaceDetector detector;

    FaceDetector() {
        detector = FirebaseVision.getInstance().getVisionFaceDetector(DETECTOR_OPTIONS);
    }

    Maybe<CandidMoment> detectMomentInImage(FirebaseVisionImage image) {
        return Maybe.create(emitter -> {
            try {
                List<FirebaseVisionFace> faces = Tasks.await(detector.detectInImage(image));
                CandidMoment moment = new CandidMoment(image.getBitmap(), faces);
                emitter.onSuccess(moment);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

}
