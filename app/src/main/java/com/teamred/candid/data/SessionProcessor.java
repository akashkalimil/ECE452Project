package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.data.EmotionClassifier.Emotion;
import com.teamred.candid.vision.BatchResponse.Response;
import com.teamred.candid.vision.CloudVision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.teamred.candid.data.SessionManager.*;

public class SessionProcessor {

    private final Session session;
    private final FaceDetector faceDetector;
    private final CloudVision cloudVision;
    private final EmotionClassifier emotionClassifier;
    private final EmotionClassificationStore classificationStore;

    public SessionProcessor(Session session) {
        this.session = session;
        faceDetector = new FaceDetector();
        cloudVision = new CloudVision();
        emotionClassifier = new EmotionClassifier();
        classificationStore = new EmotionClassificationStore(session);
    }

    public Single<? extends Map<Emotion, List<String>>> groupByEmotion() {
        return classificationStore.classificationFileExists()
                ? classificationStore.read()
                : computeGroupings();
    }

    private Single<Map<Emotion, List<String>>> computeGroupings() {
        Observable<String> files = getFilesWithFaces();
        Observable<Response> annotations = annotateWithCloudVision(files);
        return Observable
                .zip(files, annotations, PathResponse::new)
                .filter(r -> r.response.hasFaces())
                .collectInto(new HashMap<Emotion, List<String>>(), (acc, res) -> {
                    Set<Emotion> emotions = emotionClassifier.extract(res.response);
                    for (Emotion e : emotions) {
                        if (acc.containsKey(e)) {
                            acc.get(e).add(res.path);
                        } else {
                            List<String> list = new ArrayList<>();
                            list.add(res.path);
                            acc.put(e, list);
                        }
                    }
                })
                .flatMap(classificationStore::write);
    }

    private Observable<String> getFilesWithFaces() {
        return Observable
                .fromArray(session.getDirectory().listFiles())
                .filter(f -> f.isFile() && f.getName().endsWith(".png"))
                .map(File::getPath)
                .flatMapMaybe(path -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                    return faceDetector.detect(image)
                            .filter(this::hasValidFaces)
                            .map(r -> path);
                });
    }

    private Observable<Response> annotateWithCloudVision(Observable<String> files) {
        return files
                .map(BitmapFactory::decodeFile)
                .toList()
                .flatMap(cloudVision::annotateImages)
                .flatMapObservable(Observable::fromIterable);
    }

    private boolean hasValidFaces(FaceDetector.Result res) {
        return res.faces.size() > 0 && res.faces.stream().anyMatch(this::eyesOpen);
    }

    private boolean eyesOpen(FirebaseVisionFace face) {
        return face.getLeftEyeOpenProbability() >= 0.9 &&
                face.getRightEyeOpenProbability() >= 0.9;
    }

    private static class PathResponse {
        final String path;
        final Response response;

        PathResponse(String path, Response response) {
            this.response = response;
            this.path = path;
        }
    }
}
