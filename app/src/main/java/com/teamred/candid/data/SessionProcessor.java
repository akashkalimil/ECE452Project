package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.model.Emotion;
import com.teamred.candid.model.Session;
import com.teamred.candid.vision.BatchResponse.Response;
import com.teamred.candid.vision.CloudVision;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Observable;
import io.reactivex.Single;

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
                .map(classifications -> {
                    classifications.put(Emotion.LOUD, getLoudFiles());
                    return classifications;
                })
                .flatMap(classificationStore::write);
    }

    private Observable<String> getFilesWithFaces() {
        return Observable
                .fromArray(session.getDirectory().listFiles())
                .filter(this::isPhoto)
                .map(File::getPath)
                .flatMapMaybe(path -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                    return faceDetector.detect(image)
                            .filter(this::hasValidFaces)
                            .map(r -> path);
                });
    }

    private List<String> getLoudFiles() {
        return Stream.of(session.getDirectory().listFiles())
                .filter(this::isLoudPhoto)
                .map(File::getPath)
                .collect(Collectors.toList());
    }

    private boolean isPhoto(File file) {
        return file.isFile() && !file.getName().endsWith("-loud.png")
                && file.getName().endsWith(".png");
    }

    private boolean isLoudPhoto(File file) {
        return file.isFile() && file.getName().endsWith("-loud.png");
    }

    private Observable<Response> annotateWithCloudVision(Observable<String> files) {
        return files
                .map(BitmapFactory::decodeFile)
                .toList()
                .flatMap(cloudVision::annotateImages)
                .flatMapObservable(Observable::fromIterable);
    }

    private boolean hasValidFaces(FaceDetector.Result res) {
        return res.faces.size() > 0 && res.faces.stream().anyMatch(f ->
                f.getLeftEyeOpenProbability() >= 0.9 && f.getRightEyeOpenProbability() >= 0.9);
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
