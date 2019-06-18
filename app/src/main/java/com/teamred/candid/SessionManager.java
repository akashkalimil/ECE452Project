package com.teamred.candid;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.reactivex.Maybe;

public class SessionManager {

    private static final float SMILING_THRESHOLD = .7f;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH_mm_ss_yy_MM_dd");

    enum Mode {
        PARTY,
        SELFIE
    }

    private final Mode mode;
    private final File rootDirectory;

    private int saveCount;
    private File sessionDirectory;

    SessionManager(Mode mode, File fileDirectory) {
        this.mode = mode;
        this.rootDirectory = fileDirectory;
    }

    void start() {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(rootDirectory, sessionName);
        sessionDirectory.mkdir();
    }

    // Returns directory containing images taken during this session
    File end() {
        saveCount = 0;
        return sessionDirectory;
    }

    Maybe<CandidMoment> saveOrDispose(CandidMoment moment) {
        if (!shouldSaveMoment(moment))
            return Maybe.empty();

        String filename = String.format("%s.jpg", saveCount++);
        File file = new File(sessionDirectory, filename);
        try {
            Log.d("SessionManager", "saving image!");
            FileOutputStream out = new FileOutputStream(file);
            moment.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Maybe.just(moment);
        } catch (Exception e) {
            e.printStackTrace();
            return Maybe.empty();
        }
    }

    private boolean shouldSaveMoment(CandidMoment moment) {
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
