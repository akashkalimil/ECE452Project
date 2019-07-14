package com.teamred.candid.data;

import com.teamred.candid.data.EmotionClassifier.Emotion;
import com.teamred.candid.data.SessionManager.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;

class EmotionClassificationStore {

    private static final String FILE_NAME = "classifications.txt";

    private final File classifications;

    EmotionClassificationStore(Session session) {
        classifications = new File(session.getDirectory() + File.separator + FILE_NAME);
    }

    boolean classificationFileExists() {
        return classifications.exists() && classifications.length() > 0;
    }

    Single<Map<Emotion, List<String>>> write(Map<Emotion, List<String>> classifications) {
        return Single.create(emitter -> {
            try {
                FileOutputStream out = new FileOutputStream(this.classifications);
                for (Map.Entry<Emotion, List<String>> entry : classifications.entrySet()) {
                    StringBuilder row = new StringBuilder();
                    Emotion e = entry.getKey();
                    row.append(e).append(",");
                    List<String> files = entry.getValue();
                    for (String f : files) row.append(f).append(",");
                    out.write(row.append("\n").toString().getBytes());
                }
                out.flush();
                out.close();
                emitter.onSuccess(classifications);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    Single<Map<Emotion, List<String>>> read() {
        return Single.create(emitter -> {
            try {
                FileInputStream in = new FileInputStream(classifications);
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
}
