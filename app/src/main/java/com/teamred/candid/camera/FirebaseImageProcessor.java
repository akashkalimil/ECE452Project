package com.teamred.candid.camera;

import android.support.annotation.NonNull;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Size;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class FirebaseImageProcessor implements FrameProcessor {

    private final PublishSubject<FirebaseVisionImage> imageSubject;

    // Cache metadata objects for re-use
    private final Map<String, FirebaseVisionImageMetadata> metadataCache;

    public FirebaseImageProcessor() {
        this.metadataCache = new HashMap<>();
        this.imageSubject = PublishSubject.create();
    }

    public Observable<FirebaseVisionImage> imageStream() {
        return imageSubject;
    }

    @Override
    public void process(@NonNull Frame frame) {
        if (frame.getData() == null) return;

        final FirebaseVisionImageMetadata metadata;

        String metadataKey = metadataCacheKey(frame);
        if (metadataCache.containsKey(metadataKey)) {
            metadata = metadataCache.get(metadataKey);
        } else {
            FirebaseVisionImageMetadata meta = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(frame.getSize().getWidth())
                    .setHeight(frame.getSize().getHeight())
                    .setFormat(frame.getFormat())
                    .setRotation(frame.getRotation() / 90)
                    .build();
            metadataCache.put(metadataKey, meta);
            metadata = meta;
        }

        FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(frame.getData(), metadata);
        imageSubject.onNext(image);
    }

    private static String metadataCacheKey(Frame frame) {
        Size size = frame.getSize();
        return String.format("%s:%s:%s:%s",
                size.getWidth(), size.getHeight(), frame.getFormat(), frame.getRotation());
    }
}
