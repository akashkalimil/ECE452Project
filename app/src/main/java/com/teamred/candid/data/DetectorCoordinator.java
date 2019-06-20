package com.teamred.candid.data;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.model.Noise;
import com.teamred.candid.model.Moment;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;

public class DetectorCoordinator {

    private final Observable<FirebaseVisionImage> imageStream;
    private final Observable<Double> audioStream;

    private final Detector<FirebaseVisionImage, FaceDetector.Result> faceDetector;
    private final Detector<Double, Noise> noiseDetector;

    public DetectorCoordinator(Observable<FirebaseVisionImage> imageStream,
                               Observable<Double> audioStream,
                               Detector<FirebaseVisionImage, FaceDetector.Result> faceDetector,
                               Detector<Double, Noise> noiseDetector) {
        this.imageStream = imageStream;
        this.audioStream = audioStream;
        this.faceDetector = faceDetector;
        this.noiseDetector = noiseDetector;
    }

    /**
     * Returns a stream that when subscribed to emits all sampled Moments events.
     * This stream should be filtered to determine which Moments to save.
     */
    public Observable<Moment> detectedMomentStream() {
        return Observable.zip(
                createDetectionPipe(imageStream, faceDetector),
                createDetectionPipe(audioStream, noiseDetector),
                (face, noise) -> new Moment(face.image, face.faces, noise.level)
        );
    }

    private static <A, B> Observable<B> createDetectionPipe(
            Observable<A> source, Detector<A, B> detector) {
        return source
                .sample(2, TimeUnit.SECONDS)
                .flatMapMaybe(detector::detect);
    }
}
