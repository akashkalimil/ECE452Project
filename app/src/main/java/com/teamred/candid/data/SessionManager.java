package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.teamred.candid.model.Moment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class SessionManager {

    private static final float SMILING_THRESHOLD = .7f;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss_dd_MM_yy");

    public enum Mode {
        PARTY,
        SELFIE
    }

    private final Mode mode;
    private final File rootDirectory;

    private int saveCount;
    private File sessionDirectory;

    public SessionManager(Mode mode, File fileDirectory) {
        this.mode = mode;
        this.rootDirectory = fileDirectory;
    }

    public void start() {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(rootDirectory, sessionName);
        sessionDirectory.mkdir();
    }

    // Returns directory containing images taken during this session
    public File end() {
        saveCount = 0;
        return sessionDirectory;
    }

    public Single<File> saveMoment(Moment moment) {
        String filename = String.format("%s.png", saveCount++);
        File file = new File(sessionDirectory, filename);
        try {
            Log.d("SessionManager", "saving image!");
            FileOutputStream out = new FileOutputStream(file);
            moment.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Single.just(file);
        } catch (Exception e) {
            return Single.error(e);
        }
    }

    public boolean shouldSaveMoment(Moment moment) {
        if (!moment.hasFaces()) return false;

        // Return true if we're in selfie mode and there's one person smiling
        List<FirebaseVisionFace> faces = moment.getFaces();
        if (mode == Mode.SELFIE) {
            return faces.size() == 1 &&
                    faces.get(0).getSmilingProbability() > SMILING_THRESHOLD;
        }

        // Otherwise only return true if everyone's smiling
        for (FirebaseVisionFace face : faces) {
            if (face.getSmilingProbability() < SMILING_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

}
