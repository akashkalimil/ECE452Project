package com.teamred.candid.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.support.annotation.NonNull;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Size;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class BitmapProcessor implements FrameProcessor {

    private final PublishSubject<Bitmap> bitmapSubject;

    public BitmapProcessor() {
        this.bitmapSubject = PublishSubject.create();
    }

    public Observable<Bitmap> imageStream() {
        return bitmapSubject;
    }

    @Override
    public void process(@NonNull Frame frame) {
        byte[] data = frame.getData();
        if (data == null) return;

        final int width = frame.getSize().getWidth();
        final int height = frame.getSize().getHeight();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] bytes = out.toByteArray();

        Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        bitmapSubject.onNext(image);
    }
}
