package com.teamred.candid.rest;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

class BatchRequest {
    List<ImageRequest> requests;

    BatchRequest(List<ImageRequest> requests) {
        this.requests = requests;
    }

    static class ImageRequest {
        static class Image {
            String content;

            Image(String content) {
                this.content = content;
            }
        }

        static class Feature {
            final String type = "FACE_DETECTION";
        }

        final Image image;
        final List<Feature> features = Collections.singletonList(new Feature());

        ImageRequest(Bitmap bitmap) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, out);
            byte[] bytes = out.toByteArray();
            String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
            image = new Image(encoded);
        }
    }
}