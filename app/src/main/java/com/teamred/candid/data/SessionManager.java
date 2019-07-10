package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.teamred.candid.model.Moment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Single;

public class SessionManager {

    private static final float SMILING_THRESHOLD = .7f;
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss_dd_MM_yy", Locale.US);
    private static final DateFormat VISIBLE_DATE_FORMAT = new SimpleDateFormat("MMMM dd", Locale.US);

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

    public SessionManager(File fileDirectory) {
        this.mode = Mode.SELFIE;
        this.rootDirectory = fileDirectory;
    }

    public void start() {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(rootDirectory, sessionName);
        sessionDirectory.mkdir();
    }

    // Returns directory containing images taken during this session
    public Session end() {
        saveCount = 0;
        return new Session(sessionDirectory);
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

    public Single<File> saveBitmap(Bitmap bitmap) {
        String filename = String.format("%s.png", saveCount++);
        File file = new File(sessionDirectory, filename);
        try {
            Log.d("SessionManager", "saving image!");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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
//        List<FirebaseVisionFace> faces = moment.getFaces();
//        if (mode == Mode.SELFIE) {
//            return faces.size() == 1 &&
//                    faces.get(0).getSmilingProbability() > SMILING_THRESHOLD;
//        }
//
//        // Otherwise only return true if everyone's smiling
//        for (FirebaseVisionFace face : faces) {
//            if (face.getSmilingProbability() < SMILING_THRESHOLD) {
//                return false;
//            }
//        }
//        return true;

        // TODO: this is for demo
        for (FirebaseVisionFace image : moment.getFaces()) {
            if (image.getSmilingProbability() > SMILING_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    public static class Session {
        private final File directory;
        private final File[] pictures;

        Session(File directory) {
            this.directory = directory;
            this.pictures = directory.listFiles();
        }

        public String getDateString() {
            try {
                return VISIBLE_DATE_FORMAT.format(DATE_FORMAT.parse(directory.getName()));
            } catch (ParseException e) {
                return null;
            }
        }

        public int getPictureCount() {
            return pictures.length;
        }

        public File getPreviewPicture() {
            return pictures.length > 0 ? pictures[0] : null;
        }

        public File getDirectory() {
            return directory;
        }
    }

    public List<Session> getSessions() {
        return Arrays.stream(rootDirectory.listFiles())
                .map(Session::new)
                .filter(s -> s.getPictureCount() > 0)
                .sorted(SessionDateComparator)
                .collect(Collectors.toList());
    }

    public int getSaveCount() {
        return saveCount;
    }

    private static final Comparator<Session> SessionDateComparator = (a, b) -> {
        try {
            Date dateA = DATE_FORMAT.parse(a.directory.getName());
            Date dateB = DATE_FORMAT.parse(b.directory.getName());
            return dateB.compareTo(dateA);
        } catch (ParseException e) {
            return 0;
        }
    };
}
