package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.teamred.candid.camera.FaceDetector;
import com.teamred.candid.data.EmotionExtractor.Emotion;
import com.teamred.candid.vision.BatchResponse.Response;
import com.teamred.candid.vision.CloudVision;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
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

    public SessionProcessor(Session session) {
        this.session = session;
        faceDetector = new FaceDetector();
        cloudVision = new CloudVision();
        emotionExtractor = new EmotionExtractor();
    }

    /*
    - Run all pictures through firebase face detect with .001 min face size
    - delete photos that don't have faces or dont have open eyes
    - Push all photos to google vision
    - get results and store grouping of emotions (joy, anger, surprise, sorrow, serious)
     */

    static class PathResponse {
        final String path;
        final Response response;

        PathResponse(String path, Response response) {
            this.response = response;
            this.path = path;
        }
    }

    public void processSession() {
        Observable<String> paths = Observable
                .fromArray(session.getDirectory().listFiles())
                .filter(f -> f.isFile() && f.getName().endsWith(".png"))
                .map(File::getPath)
                .flatMapMaybe(path -> {
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                    return faceDetector.detect(image)
                            .filter(r -> r.faces.size() > 0 && r.faces.stream().anyMatch(this::eyesOpen))
                            .map(r -> path);
                });

        Observable<Response> responses = paths
                .map(BitmapFactory::decodeFile)
                .toList()
                .flatMap(cloudVision::annotateImages)
                .flatMapObservable(Observable::fromIterable);

        Single<File> groupings = Observable
                .zip(paths, responses, PathResponse::new)
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


    // joy, .../sessionDir/1.png
    // joy, .../sessionDir/2.png
    // surprise, .../sessionDir/3.png
    //
    Single<File> writeGroupings(Map<Emotion, List<String>> groupings) {
        final File file = new File(session.getDirectory() + File.separator + GROUPINGS_FILE);
        return Single.create(emitter -> {
            try {
                FileOutputStream out = new FileOutputStream(file);
                StringBuilder builder = new StringBuilder();
                for (Emotion group : groupings.keySet()) {
                    List<String> paths = groupings.get(group);
                    for (String path : paths) {
                        builder.append(group).append(",")
                                .append(path).append("\n");
                    }
                    out.write(builder.toString().getBytes());
                    builder.setLength(0);
                }
                out.flush();
                out.close();
                emitter.onSuccess(file);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private boolean eyesOpen(FirebaseVisionFace face) {
        return face.getLeftEyeOpenProbability() >= 0.9 &&
                face.getRightEyeOpenProbability() >= 0.9;
    }

}
