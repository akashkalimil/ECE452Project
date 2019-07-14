package com.teamred.candid.model;

import com.teamred.candid.data.SessionManager;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Session {
    private static final DateFormat VISIBLE_DATE_FORMAT = new SimpleDateFormat("MMMM dd", Locale.US);

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
            Date parsed = SessionManager.DATE_FORMAT.parse(directory.getName());
            return VISIBLE_DATE_FORMAT.format(parsed);
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