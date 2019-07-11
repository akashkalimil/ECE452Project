package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class SessionManager {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss_dd_MM_yy", Locale.US);
    private static final DateFormat VISIBLE_DATE_FORMAT = new SimpleDateFormat("MMMM dd", Locale.US);

    private static final String TAG = "SessionManager";
    private static final int CAMERA_SAMPLE_PERIOD = 5;

    private int photoCount;
    private File sessionDirectory;
    private final File rootDirectory;

    public SessionManager(File fileDirectory) {
        this.rootDirectory = fileDirectory;
    }

    public Observable<File> start(Observable<Bitmap> frames) {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(rootDirectory, sessionName);
        sessionDirectory.mkdir();

        return frames.sample(CAMERA_SAMPLE_PERIOD, TimeUnit.SECONDS)
                .flatMapSingle(this::savePhoto)
                .subscribeOn(Schedulers.io());
    }

    public Session end() {
        photoCount = 0;
        return new Session(sessionDirectory);
    }

    public List<Session> getSessions() {
        return Arrays.stream(rootDirectory.listFiles())
                .map(Session::new)
                .filter(s -> s.getPictureCount() > 0)
                .sorted(SessionDateComparator)
                .collect(Collectors.toList());
    }

    public int getPhotoCount() {
        return photoCount;
    }

    private Single<File> savePhoto(Bitmap bitmap) {
        String filename = String.format("%s.png", photoCount++);
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

    public static class Session {
        private final File directory;
        private final File[] pictures;

        public Session(File directory) {
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
