package com.teamred.candid.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.teamred.candid.model.Session;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class SessionManager {

    private static final String TAG = "SessionManager";

    private static final int CAMERA_SAMPLE_PERIOD = 3;
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss_dd_MM_yy", Locale.US);

    private File sessionDirectory;
    private AtomicInteger pictureCount;

    private final File root;
    private final int samplePeriod;

    /**
     * Creates a new SessionManager instance.
     *
     * @param fileDirectory The file system location where session contents will be persisted.
     */
    public SessionManager(UserManager userManager) {
        this.root = userManager.getCurrentUserDirectory();
        this.pictureCount = new AtomicInteger();
        this.samplePeriod = CAMERA_SAMPLE_PERIOD;
    }

    /**
     * Creates a new observable stream which emits File objects corresponding to saved images.
     * Images are captured at every multiple instance of the sample period in addition to when
     * peak audio levels are detected from the microphone.
     *
     * @param camera     An observable representing a stream of nearly continuous frames from the camera.
     * @param audioPeaks An observable which emits the value true whenever an audio peak is detected.
     */
    public Observable<File> start(Observable<Bitmap> camera, Observable<Boolean> audioPeaks) {
        String sessionName = DATE_FORMAT.format(new Date());
        sessionDirectory = new File(root, sessionName);
        sessionDirectory.mkdir();

        Observable<File> files = camera.sample(samplePeriod, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .flatMapSingle(this::savePhoto)
                .subscribeOn(Schedulers.io());

        Observable<File> loudFiles = camera.sample(audioPeaks)
                .subscribeOn(Schedulers.newThread())
                .flatMapSingle(this::saveLoudPhoto)
                .subscribeOn(Schedulers.io());

        return Observable.merge(files, loudFiles);
    }

    public Session end() {
        pictureCount.set(0);
        return new Session(sessionDirectory);
    }

    public List<Session> getSessions() {
        return Arrays.stream(root.listFiles())
                .map(Session::new)
                .filter(s -> s.getPictureCount() > 0)
                .sorted(SessionDateComparator)
                .collect(Collectors.toList());
    }

    public int getPictureCount() {
        return pictureCount.get();
    }

    private Single<File> savePhoto(Bitmap bitmap) {
        return savePhoto(bitmap, "");
    }

    private Single<File> saveLoudPhoto(Bitmap bitmap) {
        return savePhoto(bitmap, "-loud");
    }

    private Single<File> savePhoto(Bitmap bitmap, String suffix) {
        String filename = String.format("%s%s.png", pictureCount.incrementAndGet(), suffix);
        File file = new File(sessionDirectory, filename);
        try {
            Log.d(TAG, "saving image!");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return Single.just(file);
        } catch (Exception e) {
            return Single.error(e);
        }
    }


    private static final Comparator<Session> SessionDateComparator = (a, b) -> {
        try {
            Date dateA = DATE_FORMAT.parse(a.getDirectory().getName());
            Date dateB = DATE_FORMAT.parse(b.getDirectory().getName());
            return dateB.compareTo(dateA);
        } catch (ParseException e) {
            return 0;
        }
    };
}
