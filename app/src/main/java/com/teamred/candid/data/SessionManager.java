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
import java.util.stream.Stream;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SessionManager {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss_dd_MM_yy", Locale.US);
    private static final DateFormat VISIBLE_DATE_FORMAT = new SimpleDateFormat("MMMM dd", Locale.US);

    private static final String TAG = "SessionManager";
    private static final int CAMERA_SAMPLE_PERIOD = 5;

    private int pictureCount;
    private File sessionDirectory;
    private final File rootDirectory;

    public SessionManager(File fileDirectory) {
        this.rootDirectory = fileDirectory;
    }

    public Observable<File> start(Observable<Bitmap> frames, Observable<Boolean> audio) {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(rootDirectory, sessionName);
        sessionDirectory.mkdir();

        Observable<Bitmap> sampled = frames
                .sample(CAMERA_SAMPLE_PERIOD, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread());

        Observable<Bitmap> audioSampled = frames
                .sample(audio)
                .subscribeOn(Schedulers.newThread());

        return Observable.merge(sampled, audioSampled)
                .flatMapSingle(this::savePhoto)
                .subscribeOn(Schedulers.io());
    }

    public Session end() {
        pictureCount = 0;
        return new Session(sessionDirectory);
    }

    public List<Session> getSessions() {
        return Arrays.stream(rootDirectory.listFiles())
                .map(Session::new)
                .filter(s -> s.getPictureCount() > 0)
                .sorted(SessionDateComparator)
                .collect(Collectors.toList());
    }

    public int getPictureCount() {
        return pictureCount;
    }

    private synchronized Single<File> savePhoto(Bitmap bitmap) {
        String filename = String.format("%s.png", pictureCount++);
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
        private final List<File> pictures;

        public Session(File directory) {
            this.directory = directory;
            this.pictures = Stream.of(directory.listFiles())
                    .filter(f -> f.isFile() && f.getName().endsWith(".png"))
                    .collect(Collectors.toList());
        }

        public String getDateString() {
            try {
                return VISIBLE_DATE_FORMAT.format(DATE_FORMAT.parse(directory.getName()));
            } catch (ParseException e) {
                return null;
            }
        }

        public int getPictureCount() {
            return pictures.size();
        }

        public File getPreviewPicture() {
            return pictures.size() > 0 ? pictures.get(0) : null;
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
