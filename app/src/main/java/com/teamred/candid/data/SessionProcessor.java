package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.data.EmotionExtractor.Emotion;
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

    private static final String GROUPINGS_FILE = "groupings";

    private final Session session;
    private final FaceDetector faceDetector;
    private final CloudVision cloudVision;
    private final EmotionExtractor emotionExtractor;
    private final File groupings;

    public SessionProcessor(Session session) {
        this.session = session;
        faceDetector = new FaceDetector();
        cloudVision = new CloudVision();
        emotionExtractor = new EmotionExtractor();
        groupings = new File(session.getDirectory() + File.separator + GROUPINGS_FILE);

    }

    public Single<? extends Map<Emotion, List<String>>> groupByEmotion() {
        return groupings.exists() ? readGroupings() : computeGroupings();
    }

    private Single<Map<Emotion, List<String>>> computeGroupings() {
        Observable<String> files = getFilesWithFaces();
        Observable<Response> annotations = annotateWithCloudVision(files);
        return Observable
                .zip(files, annotations, PathResponse::new)
                .filter(r -> r.response.hasFaces())
                .collectInto(new HashMap<Emotion, List<String>>(), (acc, res) -> {
                    Set<Emotion> emotions = emotionExtractor.extract(res.response);
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
                .flatMap(this::writeGroupings);
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

    private Single<Map<Emotion, List<String>>> writeGroupings(Map<Emotion, List<String>> groupings) {
        return Single.create(emitter -> {
            try {
                FileOutputStream out = new FileOutputStream(this.groupings);
                for (Map.Entry<Emotion, List<String>> entry : groupings.entrySet()) {
                    StringBuilder row = new StringBuilder();
                    Emotion e = entry.getKey();
                    row.append(e).append(",");
                    List<String> files = entry.getValue();
                    for (String f : files) row.append(f).append(",");
                    out.write(row.append("\n").toString().getBytes());
                }
                out.flush();
                out.close();
                emitter.onSuccess(groupings);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private Single<Map<Emotion, List<String>>> readGroupings() {
        return Single.create(emitter -> {
            try {
                FileInputStream in = new FileInputStream(groupings);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                Map<Emotion, List<String>> groups = new HashMap<>();
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    if (tokens.length <= 1) continue;
                    Emotion e = Emotion.valueOf(tokens[0]);
                    List<String> files = Arrays.asList(tokens).subList(1, tokens.length);
                    groups.put(e, files);
                }
                emitter.onSuccess(groups);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
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
